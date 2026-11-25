/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fraunhofer.aisec.s4sgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.*;

import com.networknt.schema.Error;

import static de.fraunhofer.aisec.s4sgateway.Utils.checkMandatoryFields;

@Component
@RestController
public class Controller {
	private static final Logger logger = LoggerFactory.getLogger(Controller.class);
	private final ObjectMapper objectMapper;
	private final JsonSchemaValidationService validator;


	@Value("${gatewayReceiverURL}")
	private String gatewayReceiverURL;

	@Value("${gatewayReceiverUsername}")
	private String gatewayReceiverUsername;
	@Value("${gatewayReceiverPassword}")
	private String gatewayReceiverPassword;
	@Value("${gateway.version}")
	private String gatewayVersion;
	@Value("${gatewayDomainRC}")
	private String gatewayDomainRC;
	@Value("${gatewayPortRC}")
	private String gatewayPortRC;
	@Value("${gatewayDomainSD}")
	private String gatewayDomainSD;
	@Value("${gatewayPortSD}")
	private String gatewayPortSD;
	@Value("${gatewayProtocol}")
	private String gatewayProtocol;
	@Value("${kafkaDomainLocal}")
	private String kafkaDomainLocal;
	@Value("${kafkaPortLocal}")
	private String kafkaPortLocal;
	@Value("${sslOutboundActivated}")
	private boolean sslOutboundActivated;
	@Value("${kafkaTopic}")
	private String kafkaTopic;
	@Value("${trustStorePassword}")
	private String trustStorePassword;
	@Value("${trustStorePath}")
	private String trustStorePath;
	@Value("${keyStorePassword}")
	private String keyStorePassword;
	@Value("${keyStorePath}")
	private String keyStorePath;

	@Value("${isaPath}")
	private String isaPath;

	@Value("${endpoints.reloadconfig.enabled}")
	private boolean endpointsEnabledReloadconfig;
	@Value("${endpoints.setisa.enabled}")
	private boolean endpointsEnabledSetisa;

	ObjectNode isa;

	private HttpClient client;
	@PostConstruct
	public void initializeHttpClient() throws Exception {
		// Create an HttpClient and SSL Context with TrustStore
		HttpClient.Builder builder = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10));

		if (sslOutboundActivated) {
			// 1. Load client keystore (for mTLS client cert)
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			try (InputStream is = Files.newInputStream(Path.of(keyStorePath))) {
				keyStore.load(is, keyStorePassword.toCharArray());
			} catch (IOException | CertificateException e) {
				throw new RuntimeException(e);
			}
			// 2. Load truststore (to verify server cert)
			KeyStore trustStore = KeyStore.getInstance("PKCS12");
			try (InputStream is = Files.newInputStream(Path.of(trustStorePath))) {
				trustStore.load(is, trustStorePassword.toCharArray());
			} catch (IOException | CertificateException e) {
				throw new RuntimeException(e);
			}
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(
					TrustManagerFactory.getDefaultAlgorithm());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(
					KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, keyStorePassword.toCharArray());
			tmf.init(trustStore);

			logger.debug("Activating SSL for outbound connection!");
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(
					kmf.getKeyManagers(),   // enables mTLS
					tmf.getTrustManagers(),
					new java.security.SecureRandom());

			SSLParameters sslParameters = new SSLParameters();
			// Enforce hostname verification against the server certificate
			sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

			builder.sslContext(sslContext);
			builder.sslParameters(sslParameters);
		} else {
			logger.warn("No SSL activated for outbound connection!");
		}

		client = builder.build();

		if (endpointsEnabledReloadconfig) {
			logger.warn("Endpoint reloadconfig can be reached - only use in debug mode!");
		}
		if (endpointsEnabledSetisa) {
			logger.warn("Endpoint setisa can be reached - only use in debug mode!");
		}
	}

	public Controller(ObjectMapper objectMapper, JsonSchemaValidationService validator){
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    enum GatewayRole {
		SENDER,
		RECEIVER
	}
	@Value("${gatewayRole}")
	GatewayRole gatewayRole;
	@Value("#{${relevantHeaders}}")
	private List<String> relevantHeaders;
	ObjectNode dropConfig;
	ObjectNode filterConfig;
	ObjectNode enrichmentConfig;
	ObjectNode requiredConfig;

	@Bean
	public ApplicationRunner initiationAfterLaunch() {
		return args -> {
			initConfig();
		};
	}

	private void initConfig(){
		logger.info("SAFE4SOC-IS-Gateway version: {}",gatewayVersion);
		if(sslOutboundActivated){
			logger.info("HTTPS for Outbound connection active!");
		}
		LoadISA();
		CheckISAConfig();
	}

	@RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST})
	public ResponseEntity<String> listAllHeaders(@RequestHeader Map<String, String> headers,
												 @RequestBody String body,
												 HttpServletRequest incomingRequest) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {

		if (logger.isDebugEnabled()) {
			headers.forEach((key, value) -> logger.debug("Header:{} Key:{}", key, value));
            logger.debug("RequestURI {}", incomingRequest.getRequestURI());
            logger.debug("RemotePort {}", incomingRequest.getRemotePort());
            logger.debug("LocalPort  {}", incomingRequest.getLocalPort());
            logger.debug("ServerPort {}", incomingRequest.getServerPort());
            logger.debug("ServerName {}", incomingRequest.getServerName());
			logger.debug("Body     \n{}", body);
		}

		String answer = body;

		// Check JSON format and IDMEFv2 format from schema
		// 1. Validate JSON syntax
		ObjectNode data;

		try {
			data = readJsonObject(body);
		} catch (JsonProcessingException | IllegalArgumentException e) {
			return ResponseEntity.badRequest()
					.body("Invalid JSON: " + e.getMessage());
		}
		logger.info("Alert is valid JSON.");

		// 2. Validate JSON Schema
		List<Error> errors = validator.validate(data);
		if (!errors.isEmpty()) {
			logger.error("Incoming alert is not valid against configured JSON schema.");
			logger.error(errors.toString());
			return ResponseEntity.unprocessableEntity()
					.body("Incoming alert is not valid against JSON Schema\n" +
							errors.stream()
									.map(Error::toString)
									.toList().toString());
		}
		logger.info("Incoming alert is valid against configured JSON schema.");

		// 3. Process valid IDMEFv2 message
		if(!data.isEmpty()){
			if(!dropConfig.isEmpty()){
				String dropcheckResult = Utils.applyDropCheck(data, dropConfig);
				if (!dropcheckResult.isEmpty()){
					logger.warn("Alert rejected: forbidden value found in {}.",dropcheckResult);
					return new ResponseEntity<>(
							"Alert rejected: forbidden value found in " + dropcheckResult,
							HttpStatus.UNPROCESSABLE_ENTITY);
				}
			}
			if(!requiredConfig.isEmpty()){
				String mandatoryFieldFound = checkMandatoryFields(data, requiredConfig);
				if (!mandatoryFieldFound.isEmpty()) {
					logger.warn("Alert rejected: missing mandatory field {}.",mandatoryFieldFound);
					return new ResponseEntity<>(
							"Alert rejected: missing mandatory field " + mandatoryFieldFound,
							HttpStatus.UNPROCESSABLE_ENTITY);
				}
			}
			if(!filterConfig.isEmpty()){
				String errorFieldFound = Utils.applyFilter(data, filterConfig, validator);
				if (!errorFieldFound.isEmpty()) {
					logger.warn("Alert cant be filtered: missing field {}.",errorFieldFound);
					return new ResponseEntity<>(
							"Alert cant be filtered: missing field " + errorFieldFound,
							HttpStatus.UNPROCESSABLE_ENTITY);
				}
				logger.debug("Body after Filtering:\n{}",data.toPrettyString());
			}
			if(!enrichmentConfig.isEmpty()){
				Utils.applyEnrichment(data, enrichmentConfig);
				logger.debug("Body after Enrichment:\n{}",data.toPrettyString());
			}
			//Kafka Proxy requires enveloping alert messages in record, key, value format is below:
			if(gatewayRole==GatewayRole.RECEIVER){
				logger.debug("Putting message alert in Kafka Proxy envelope.");
				answer = "{\"records\":[{\"key\":\"test\",\"value\":" + data.toString() + "}]}";
			} else {
				answer = data.toString();
			}
		}

		// 4. Validate JSON Schema
		errors = validator.validate(data);
		if (!errors.isEmpty()) {
			logger.error("Outgoing alert is not valid against configured JSON schema.");
			logger.error(errors.toString());
			return ResponseEntity.unprocessableEntity()
					.body("Outgoing alert is not valid against JSON Schema\n" +
							errors.stream()
							.map(Error::toString)
							.toList().toString());
		}
		logger.info("Outgoing alert is valid against configured JSON schema.");

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
		switch (gatewayRole){
			case SENDER:
				String senderUrl;
				logger.debug("gatewayReceiverURL: {}", gatewayReceiverURL);
				if (gatewayReceiverURL != null && !gatewayReceiverURL.isEmpty()) {
					requestBuilder.uri(URI.create(gatewayReceiverURL + incomingRequest.getRequestURI()));
				} else {
					requestBuilder.uri(URI.create(gatewayProtocol + "://" +
							gatewayDomainRC + ":" +
							gatewayPortRC +
							incomingRequest.getRequestURI()));
				}
				break;
			case RECEIVER:
				requestBuilder.uri(URI.create(gatewayProtocol + "://" +
						kafkaDomainLocal + ":" +
						kafkaPortLocal + "/topics/" +
						kafkaTopic));
				break;
		}

		switch (incomingRequest.getMethod()) {
			case "GET":
				requestBuilder.GET();
				break;
			case "POST":
				requestBuilder.POST(HttpRequest.BodyPublishers.ofString(answer));
				break;
		}

		logger.debug("Printing headers for forward request");
		for (String headerName : relevantHeaders) {
			if (headers.containsKey(headerName)){
				logger.debug(headers.get(headerName));
				requestBuilder.setHeader(headerName, incomingRequest.getHeader(headerName));
			}
        }

		String username = gatewayReceiverUsername;
		String password = gatewayReceiverPassword;
		switch (gatewayRole){
			case SENDER:
				requestBuilder.setHeader("safe4soc-sender-hostname",gatewayDomainSD);
				requestBuilder.setHeader("safe4soc-sender-port",gatewayPortSD);
				if (username != null
						&& !username.isBlank()
						&& password != null
						&& !password.isBlank()) {

					String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
					requestBuilder.setHeader("Authorization", "Basic " + auth );
				}
				break;
			case RECEIVER:
				requestBuilder.setHeader("safe4soc-receiver-hostname",gatewayDomainRC);
				requestBuilder.setHeader("safe4soc-receiver-port",gatewayPortRC);
				requestBuilder.setHeader("Content-Type", "application/vnd.kafka.json.v2+json");
				break;
		}

		HttpRequest request = requestBuilder.build();

		if (logger.isDebugEnabled()) {
            logger.debug("Printing forwarded request info: {}", request.toString());
            logger.debug("Printing forwarded body: {}", answer);
		}

		try {
			HttpResponse<String> response = client.send(
					request,
					HttpResponse.BodyHandlers.ofString()
			);

			int status = response.statusCode();

			logger.info(
					"Receiver response: method={}, url={}, status={}",
					request.method(),
					request.uri(),
					status
			);

			logger.debug(
					"Receiver response body for {}: {}",
					request.uri(),
					response.body()
			);

			if (status == 401) {
				logger.error(
						"Receiver authentication failed: url={}, status={}",
						request.uri(),
						status
				);

				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("Receiver authentication failed: url=" + request.uri());
			}

			if (status == 403) {
				logger.error(
						"Receiver authorization denied: url={}, status={}",
						request.uri(),
						status
				);

				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body("Receiver authorization denied: url=" + request.uri());
			}

			if (status >= 500) {
				logger.error(
						"Receiver internal error: url={}, status={}",
						request.uri(),
						status
				);

				return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
						.body("Receiver returned an internal server error: url=" + request.uri());
			}

			if (status >= 400) {
				logger.error(
						"Receiver rejected request: url={}, status={}",
						request.uri(),
						status
				);

				return ResponseEntity.status(
						HttpStatusCode.valueOf(status)
				).body(response.body());
			}

			return ResponseEntity.status(
					HttpStatusCode.valueOf(status)
			).body(response.body());

		} catch (SSLHandshakeException e) {
			logger.error(
					"TLS or mTLS handshake failed: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("TLS or mTLS handshake details for {}", request.uri(), e);

			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body("TLS or mTLS handshake failed");

		} catch (SSLException e) {
			logger.error(
					"TLS communication failed: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("TLS communication details for {}", request.uri(), e);

			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body("TLS communication failed");

		} catch (UnknownHostException e) {
			logger.error(
					"Receiver hostname cannot be resolved: url={}, host={}",
					request.uri(),
					request.uri().getHost()
			);

            logger.debug("Hostname resolution details for {}", request.uri(), e);

			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body("Receiver hostname cannot be resolved");

		} catch (ConnectException | HttpConnectTimeoutException e) {
			logger.error(
					"Receiver is unreachable: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("Connection failure details for {}", request.uri(), e);

			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("Receiver is unreachable");

		} catch (HttpTimeoutException e) {

			logger.error(
					"Receiver request timed out: url={}",
					request.uri()
			);

            logger.debug("Timeout details for {}", request.uri(), e);

			return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
					.body("Receiver request timed out");

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();

			logger.error(
					"Receiver request was interrupted: url={}",
					request.uri()
			);

            logger.debug("Interrupted request details for {}", request.uri(), e);

			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("Receiver request was interrupted");

		} catch (IOException e) {
			logger.error(
					"Network error while contacting receiver: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("Network error details for {}", request.uri(), e);

			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body("Network error while contacting receiver");

		} catch (Exception e) {
			logger.error(
					"Unexpected gateway error while contacting receiver: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("Unexpected gateway error details for {}", request.uri(), e);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Internal gateway error");
		}
	}

	@RequestMapping(value = "/status", method = {RequestMethod.GET})
	public ResponseEntity<String>  status() {
		logger.debug("Status function was called");
		return new ResponseEntity<>(String.format("Gateway " + gatewayRole + " online." + "\nISA: " + isa.toString()),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/reloadconfig", method = {RequestMethod.GET})
	public ResponseEntity<String>  reloadConfig() throws IOException {
		if (!endpointsEnabledReloadconfig){
			logger.debug("Reload Configuration endpoint was called, but is disabled via config");
			return new ResponseEntity<>(
                    "Configuration reload endpoint deactivated.",
					HttpStatus.NOT_ACCEPTABLE);
		} else {
			logger.debug("Reload Configuration function was called");

			initConfig();
			return new ResponseEntity<>(
                    "Configuration reload attempt was processed, please see internal logs for results.",
					HttpStatus.OK);
		}
	}

	@RequestMapping(value = "/setisa", method = {RequestMethod.POST})
	public ResponseEntity<String> setIsa(@RequestBody String isaIncoming) {
		if (!endpointsEnabledSetisa){
			logger.debug("SetISA Configuration endpoint was called, but is disabled via config");
			return new ResponseEntity<>(
                    "SetISA configuration endpoint deactivated.",
					HttpStatus.NOT_ACCEPTABLE);
		} else {
			logger.debug("SetISA API function was called");
			LoadISA();
			try {
				loadIsaConfig(readJsonObject(isaIncoming));
			}catch (JsonProcessingException | IllegalArgumentException err){
				logger.error("Error: {}", err.toString());
			}
			logger.info("Loaded this ISA:{}",isa);
            CheckISAConfig();
			return new ResponseEntity<>("Processed ISA update.", HttpStatus.OK);
		}
	}

	@RequestMapping(value = "/isacheck", method = {RequestMethod.POST})
	public ResponseEntity<String>  isaCheck(@RequestBody String isaReceived) {
		logger.debug("IsaCheck function was called");
		logger.debug("Received the following external ISA:");
		logger.debug(isaReceived);
				ObjectNode received;
		try {
			received = readJsonObject(isaReceived);
		} catch (JsonProcessingException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("Invalid ISA JSON: " + e.getMessage());
		}
		if (Utils.isaComparison(received, this.isa)){
			logger.debug("ISA compatible.");
			return new ResponseEntity<>("ISA compatible.", HttpStatus.OK);
		} else {
			logger.debug("ISA incompatible.");
			return new ResponseEntity<>("ISA incompatible.", HttpStatus.CONFLICT);
		}
	}

	private ObjectNode readJsonObject(String json) throws JsonProcessingException {
		JsonNode node = objectMapper.reader()
				.with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
						DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
						DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
				.readTree(json);
		return Utils.requireObject(node);
	}

	private void loadIsaConfig(ObjectNode agreement) {
		ObjectNode drop = Utils.requireObject(agreement.path("dropConfig"));
		ObjectNode filter = Utils.requireObject(agreement.path("filterConfig"));
		ObjectNode required = Utils.requireObject(agreement.path("requiredConfig"));
		ObjectNode enrichment = Utils.requireObject(agreement.path("enrichmentConfig"));

		isa = agreement;
		dropConfig = drop;
		filterConfig = filter;
		requiredConfig = required;
		enrichmentConfig = enrichment;
	}

	private void LoadISA(){
		logger.info("Loading Information Sharing Agreement and Filter settings ...");
		try {
			loadIsaConfig(readJsonObject(loadIsaFromFile()));
		}catch (JsonProcessingException | IllegalArgumentException err){
			logger.error("Error: {}", err.toString());
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("Loaded this ISA:{}",isa);
	}

	private void CheckISAConfig(){
		/* Checking given configuration  */
		logger.info("Checking given configuration for role={} ...",gatewayRole);
		if(gatewayRole.toString().isEmpty()){logger.error("GatewayRole is not configured!");}
		switch (gatewayRole){
			case SENDER:
				if(gatewayDomainSD.isEmpty()){logger.error("GatewayDomainSD is not configured!");}
				if(gatewayPortSD.isEmpty()){logger.error("GatewayPortSD is not configured!");}
				if(gatewayDomainRC.isEmpty()){logger.error("GatewayDomainRC is not configured!");}
				if(gatewayPortRC.isEmpty()){logger.error("GatewayPortRC is not configured!");}
				break;
			case RECEIVER:
				if(kafkaDomainLocal.isEmpty()){logger.error("KafkaDomainLocal is not configured!");}
				if(kafkaPortLocal.isEmpty()){logger.error("KafkaPortLocal is not configured!");}
				if(gatewayDomainRC.isEmpty()){logger.error("GatewayDomainRC is not configured!");}
				if(gatewayPortRC.isEmpty()){logger.error("GatewayPortRC is not configured!");}
				break;
		}

		if(logger.isInfoEnabled()){
			switch (gatewayRole){
				case SENDER:
					logger.info("Config gatewaySD={}:{}",gatewayDomainSD,gatewayPortSD);
					logger.info("Config gatewayRC={}:{}",gatewayDomainRC,gatewayPortRC);
					break;
				case RECEIVER:
					logger.info("Config gatewayRC={}:{}",gatewayDomainRC,gatewayPortRC);
					logger.info("Config kafkaLocal={}:{}",kafkaDomainLocal,kafkaPortLocal);
					break;
			}
			logger.info("Config relevantHeaders={}",relevantHeaders.toString());
		}
	}

	private String loadIsaFromFile() throws IOException {

		// 1. Try loading an external filesystem schema
		if (isaPath != null && !isaPath.isBlank()) {

			Path path = Paths.get(isaPath);

			if (Files.isRegularFile(path) && Files.isReadable(path)) {
				logger.info("Loading external ISA: {}", path.toAbsolutePath());

				return Files.readString(path, StandardCharsets.UTF_8);
			}

			logger.warn(
					"External ISA not found or not readable: {}. "
							+ "Trying embedded schema inside binary instead.",
					path.toAbsolutePath()
			);
		}

		// 2. Fall back to the embedded classpath resource
		String resourcePath = "/isa/isa.json";

		logger.info("Loading embedded ISA from JAR or Graal VM compiled binary: {}", resourcePath);

		try (InputStream input = getClass().getResourceAsStream(resourcePath)) {

			if (input == null) {
				throw new FileNotFoundException(
						"Embedded ISA not found: " + resourcePath
				);
			}

			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

}
