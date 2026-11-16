package de.fraunhofer.aisec.s4sgateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.*;

import com.networknt.schema.Error;

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
	@Value("${isaConfig}")
	private String isaConfig;
	@Value("${endpoints.reloadconfig.enabled}")
	private boolean endpointsEnabledReloadconfig;
	@Value("${endpoints.setisa.enabled}")
	private boolean endpointsEnabledSetisa;

	JSONObject isa;

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
	JSONObject filterConfig;
	JSONObject enrichmentConfig;

	@Bean
	public ApplicationRunner initiationAfterLaunch() {
		return args -> {
			initConfig();
		};
	}

	private void initConfig(){
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
		// if format is json apply filter
		JSONObject data = new JSONObject(answer);

		// Check JSON format and IDMEFv2 format from schema
		// 1. Validate JSON syntax
		JsonNode json;

		try {
			json = objectMapper.readTree(body);

			if (json == null || !json.isObject()) {
				return ResponseEntity.badRequest()
						.body("Expected a JSON object");
			}

		} catch (JsonProcessingException e) {
			return ResponseEntity.badRequest()
					.body("Invalid JSON: " + e.getOriginalMessage());
		}
		logger.info("Alert is valid JSON.");

		// 2. Validate JSON Schema
		List<Error> errors=	validator.validate(body);

		if (!errors.isEmpty()) {
			logger.error(errors.toString());
			return ResponseEntity.unprocessableEntity()
					.body(errors.stream()
							.map(Error::toString)
							.toList().toString());
		}

		// 3. Process valid IDMEFv2 message
		logger.info("Alert is valid against configured JSON schema.");

		if(!data.isEmpty()){
			if(!filterConfig.isEmpty()){
				Utils.applyFilter(data, filterConfig);
				logger.debug("Body after Filtering:\n{}",data.toString(2));
			}
			if(!enrichmentConfig.isEmpty()){
				Utils.applyEnrichment(data, enrichmentConfig);
				logger.debug("Body after Enrichment:\n{}",data.toString(2));
			}
			//Kafka Proxy requires enveloping alert messages in record, key, value format is below:
			if(gatewayRole==GatewayRole.RECEIVER){
				logger.debug("Putting message alert in Kafka Proxy envelope.");
				answer = "{\"records\":[{\"key\":\"test\",\"value\":" + data.toString() + "}]}";
			} else {
				answer = data.toString();
			}
		}

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

		switch (gatewayRole){
			case SENDER:
				requestBuilder.setHeader("safe4soc-sender-hostname",gatewayDomainSD);
				requestBuilder.setHeader("safe4soc-sender-port",gatewayPortSD);
				break;
			case RECEIVER:
				requestBuilder.setHeader("safe4soc-receiver-hostname",gatewayDomainRC);
				requestBuilder.setHeader("safe4soc-receiver-port",gatewayPortRC);
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
						.body("Receiver authentication failed");
			}

			if (status == 403) {
				logger.error(
						"Receiver authorization denied: url={}, status={}",
						request.uri(),
						status
				);

				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body("Receiver authorization denied");
			}

			if (status >= 500) {
				logger.error(
						"Receiver internal error: url={}, status={}",
						request.uri(),
						status
				);

				return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
						.body("Receiver returned an internal server error");
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
		return new ResponseEntity<>(String.format("Gateway " + gatewayRole + " online." + "\nISA: " + isaConfig),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/reloadconfig", method = {RequestMethod.GET})
	public ResponseEntity<String>  reloadConfig() {
		if (!endpointsEnabledReloadconfig){
			logger.debug("Reload Configuration endpoint was called, but is disabled via config");
			return new ResponseEntity<>(
                    "Configuration reload endpoint deactivated.",
					HttpStatus.NOT_ACCEPTABLE);
		} else {
			logger.debug("Reload Configuration function was called");

			// Loading file config for ISA
			try (InputStream input = new ClassPathResource("application.properties").getInputStream()) {
				Properties prop = new Properties();
				prop.load(input);
				this.isaConfig = prop.getProperty("isaConfig");
			} catch (IOException ex) {
				logger.error("Failed to load application.properties: {}", ex.getMessage(), ex);
				return new ResponseEntity<>(
						"Configuration reload attempt failed, please see internal logs for results.",
						HttpStatus.INTERNAL_SERVER_ERROR);
			}

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
			isaConfig = isaIncoming;
			LoadISA();
			CheckISAConfig();
			return new ResponseEntity<>("Processed ISA update.", HttpStatus.OK);
		}
	}

	@RequestMapping(value = "/isacheck", method = {RequestMethod.POST})
	public ResponseEntity<String>  isaCheck(@RequestBody String isaReceived) {
		logger.debug("IsaCheck function was called");
		logger.debug("Received the following external ISA:");
		logger.debug(isaReceived);
		if (Utils.isaComparison(new JSONObject(isaReceived), this.isa)){
			logger.debug("ISA compatible.");
			return new ResponseEntity<>("ISA compatible.", HttpStatus.OK);
		} else {
			logger.debug("ISA incompatible.");
			return new ResponseEntity<>("ISA incompatible.", HttpStatus.CONFLICT);
		}
	}

	private void LoadISA(){
		logger.info("Loading Information Sharing Agreement and Filter settings ...");
		try {
			isa = new JSONObject(isaConfig);
			filterConfig = isa.getJSONObject("filterConfig");
			enrichmentConfig = isa.getJSONObject("enrichmentConfig");
		}catch (JSONException err){
			logger.error("Error: {}", err.toString());
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
}
