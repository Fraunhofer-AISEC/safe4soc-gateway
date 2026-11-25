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

package de.fraunhofer.aisec.s4sgateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class HttpClientConfig {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientConfig.class);

	@Value("${sslOutboundActivated}")
	private boolean sslOutboundActivated;

	@Value("${trustStorePassword}")
	private String trustStorePassword;

	@Value("${trustStorePath}")
	private String trustStorePath;

	@Value("${keyStorePassword}")
	private String keyStorePassword;

	@Value("${keyStorePath}")
	private String keyStorePath;

	@Bean
	public HttpClient gatewayHttpClient() throws Exception {
		// Create an HttpClient and SSL Context with TrustStore
		HttpClient.Builder builder = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10));

		if (sslOutboundActivated) {
			// 1. Load client keystore (for mTLS client cert)
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			try (InputStream is = Files.newInputStream(Path.of(keyStorePath))) {
				keyStore.load(is, keyStorePassword.toCharArray());
			}
			// 2. Load truststore (to verify server cert)
			KeyStore trustStore = KeyStore.getInstance("PKCS12");
			try (InputStream is = Files.newInputStream(Path.of(trustStorePath))) {
				trustStore.load(is, trustStorePassword.toCharArray());
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

		return builder.build();
	}
}
