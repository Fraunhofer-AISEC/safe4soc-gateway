package de.fraunhofer.aisec.s4sgateway;

import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.*;

@Component
@RestController
public class Controller {
	private static final Logger logger = LoggerFactory.getLogger(Controller.class);

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
	JSONObject isa;
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
		LoadISA();
		CheckISAConfig();
	}


	@RequestMapping(value = "/setisa", method = {RequestMethod.POST})
	public ResponseEntity<String> setIsa(@RequestBody String isaIncoming) {
		logger.debug("SetISA API function was called");
		isaConfig = isaIncoming;
		LoadISA();
		CheckISAConfig();
		return new ResponseEntity<>(String.format("Processed ISA update."), HttpStatus.OK);
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
				if (gatewayDomainRC.startsWith("http://") || gatewayDomainRC.startsWith("https://")) {
					// Already includes scheme
					senderUrl = gatewayDomainRC +
							incomingRequest.getRequestURI();
				} else {
					// Default to http
					senderUrl = gatewayProtocol + "://" +
							gatewayDomainRC +
							":" + gatewayPortRC +
							incomingRequest.getRequestURI();
				}
				requestBuilder.uri(URI.create(senderUrl));
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
				requestBuilder.setHeader("Content-Type", "application/vnd.kafka.json.v2+json");
				break;
		}

		HttpRequest request = requestBuilder.build();

		if (logger.isDebugEnabled()) {
            logger.debug("Printing forwarded request info: {}", request.toString());
            logger.debug("Printing forwarded body: {}", answer);
		}

		// Create an HttpClient and SSL Context with TrustStore
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

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(
				KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, keyStorePassword.toCharArray());

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);


		HttpClient.Builder builder = HttpClient.newBuilder()
        		.connectTimeout(Duration.ofSeconds(10));

		if (sslOutboundActivated) {
			logger.debug("Activating SSL for outbound connection!");
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(
					kmf.getKeyManagers(),   // enables mTLS
					tmf.getTrustManagers(),
					new java.security.SecureRandom());

			builder.sslContext(sslContext);
		} else {
			logger.warn("No SSL activated for outbound connection!");
		}

		HttpClient client = builder.build();
		
		HttpResponse<String> response = null;
		try {
			// Send the request and receive the response
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("Response status code: {}", response.statusCode());
            logger.debug("Response body: {}", response.body());
			return new ResponseEntity<>(response.body(), HttpStatusCode.valueOf(response.statusCode()));
		} catch (HttpTimeoutException e) {
            logger.error("Request timed out: {}", e.getMessage());
			logger.debug("Response status code: {}", response.statusCode());
			logger.debug("Response body: {}", response.body());
		} catch (IOException | InterruptedException e) {
            logger.error("An error occurred: {}", e.getMessage());
			logger.debug("Response status code: {}", response.statusCode());
			logger.debug("Response body: {}", response.body());
		}
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		//return new ResponseEntity<>(String.format("Listed %d headers", headers.size()), HttpStatus.OK);
	}

	@RequestMapping(value = "/status", method = {RequestMethod.GET})
	public ResponseEntity status() {
		logger.debug("Status function was called");
		return new ResponseEntity<>(String.format("Gateway " + gatewayRole + " online." + "\nISA: " + isaConfig), HttpStatus.OK);
	}

	@RequestMapping(value = "/reloadconfig", method = {RequestMethod.GET})
	public ResponseEntity reloadConfig() {
		logger.debug("Reload Configuration function was called");

		// Loading file config for ISA
		try (InputStream input = new ClassPathResource("application.properties").getInputStream()) {
			Properties prop = new Properties();
			prop.load(input);
			this.isaConfig = prop.getProperty("isaConfig");
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		initConfig();
		return new ResponseEntity<>(String.format("Configuration reload attempt was processed, please see internal logs for results."),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/isacheck", method = {RequestMethod.POST})
	public ResponseEntity isaCheck(@RequestBody String isaReceived) {
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
