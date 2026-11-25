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

package de.fraunhofer.aisec.s4sgateway.transport;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fraunhofer.aisec.s4sgateway.config.GatewayRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class AlertTransport {
	private static final Logger logger = LoggerFactory.getLogger(AlertTransport.class);

	private final HttpClient client;
	private final GatewayRole gatewayRole;
	private final List<String> relevantHeaders;

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

	@Value("${kafkaTopic}")
	private String kafkaTopic;

	public AlertTransport(HttpClient client, @Value("${gatewayRole}") GatewayRole gatewayRole,
						  @Value("#{${relevantHeaders}}") List<String> relevantHeaders) {
		this.client = client;
		this.gatewayRole = gatewayRole;
		this.relevantHeaders = relevantHeaders;
	}

	public record ForwardingResponse(int status, String body) {}

	public ForwardingResponse forward(String method, String path, Map<String, String> headers, ObjectNode alert) {
		URI destination = gatewayRole == GatewayRole.SENDER ? receiverUri(path) : kafkaUri();
		HttpRequest.Builder request = HttpRequest.newBuilder(destination);
		for (String headerName : relevantHeaders) {
			if (headers.containsKey(headerName)) {
				request.setHeader(headerName, headers.get(headerName));
			}
		}

		String body;
		if (gatewayRole == GatewayRole.SENDER) {
			configureReceiverRequest(request);
			body = alert.toString();
		} else {
			request.setHeader("safe4soc-receiver-hostname", gatewayDomainRC);
			request.setHeader("safe4soc-receiver-port", gatewayPortRC);
			request.setHeader("Content-Type", "application/vnd.kafka.json.v2+json");
			ObjectNode envelope = alert.objectNode();
			envelope.putArray("records").addObject().put("key", "test").set("value", alert);
			body = envelope.toString();
		}

		if ("POST".equals(method)) {
			request.POST(HttpRequest.BodyPublishers.ofString(body));
		} else {
			request.GET();
		}
		return send(request.build());
	}

	private URI receiverUri(String path) {
		String baseUrl = gatewayReceiverURL;
		if (baseUrl == null || baseUrl.isEmpty()) {
			baseUrl = gatewayProtocol + "://" + gatewayDomainRC + ":" + gatewayPortRC;
		}
		return URI.create(baseUrl + path);
	}

	private URI kafkaUri() {
		return URI.create(gatewayProtocol + "://" + kafkaDomainLocal + ":" + kafkaPortLocal + "/topics/" + kafkaTopic);
	}

	private void configureReceiverRequest(HttpRequest.Builder request) {
		request.setHeader("safe4soc-sender-hostname", gatewayDomainSD);
		request.setHeader("safe4soc-sender-port", gatewayPortSD);
		if (gatewayReceiverUsername != null && !gatewayReceiverUsername.isBlank()
				&& gatewayReceiverPassword != null && !gatewayReceiverPassword.isBlank()) {
			String credentials = gatewayReceiverUsername + ":" + gatewayReceiverPassword;
			String auth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
			request.setHeader("Authorization", "Basic " + auth);
		}
	}

	private ForwardingResponse send(HttpRequest request) {
		logger.debug("Forwarding request {}", request);
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

			if (status == 401) {
				logger.error(
						"Receiver authentication failed: url={}, status={}",
						request.uri(),
						status
				);

				return new ForwardingResponse(HttpStatus.UNAUTHORIZED.value(), "Receiver authentication failed: url=" + request.uri());
			}

			if (status == 403) {
				logger.error(
						"Receiver authorization denied: url={}, status={}",
						request.uri(),
						status
				);

				return new ForwardingResponse(HttpStatus.FORBIDDEN.value(), "Receiver authorization denied: url=" + request.uri());
			}

			if (status >= 500) {
				logger.error(
						"Receiver internal error: url={}, status={}",
						request.uri(),
						status
				);

				return new ForwardingResponse(HttpStatus.BAD_GATEWAY.value(), "Receiver returned an internal server error: url=" + request.uri());
			}

			if (status >= 400) {
				logger.error(
						"Receiver rejected request: url={}, status={}",
						request.uri(),
						status
				);

				return new ForwardingResponse(status, response.body());
			}

			return new ForwardingResponse(status, response.body());

		} catch (SSLHandshakeException e) {
			logger.error(
					"TLS or mTLS handshake failed: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("TLS or mTLS handshake details for {}", request.uri(), e);

			return new ForwardingResponse(HttpStatus.BAD_GATEWAY.value(), "TLS or mTLS handshake failed");

		} catch (SSLException e) {
			logger.error(
					"TLS communication failed: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("TLS communication details for {}", request.uri(), e);

			return new ForwardingResponse(HttpStatus.BAD_GATEWAY.value(), "TLS communication failed");

		} catch (UnknownHostException e) {
			logger.error(
					"Receiver hostname cannot be resolved: url={}, host={}",
					request.uri(),
					request.uri().getHost()
			);

            logger.debug("Hostname resolution details for {}", request.uri(), e);

			return new ForwardingResponse(HttpStatus.BAD_GATEWAY.value(), "Receiver hostname cannot be resolved");

		} catch (ConnectException | HttpConnectTimeoutException e) {
			logger.error(
					"Receiver is unreachable: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("Connection failure details for {}", request.uri(), e);

			return new ForwardingResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), "Receiver is unreachable");

		} catch (HttpTimeoutException e) {

			logger.error(
					"Receiver request timed out: url={}",
					request.uri()
			);

            logger.debug("Timeout details for {}", request.uri(), e);

			return new ForwardingResponse(HttpStatus.GATEWAY_TIMEOUT.value(), "Receiver request timed out");

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();

			logger.error(
					"Receiver request was interrupted: url={}",
					request.uri()
			);

            logger.debug("Interrupted request details for {}", request.uri(), e);

			return new ForwardingResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), "Receiver request was interrupted");

		} catch (IOException e) {
			logger.error(
					"Network error while contacting receiver: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("Network error details for {}", request.uri(), e);

			return new ForwardingResponse(HttpStatus.BAD_GATEWAY.value(), "Network error while contacting receiver");

		} catch (Exception e) {
			logger.error(
					"Unexpected gateway error while contacting receiver: url={}, type={}",
					request.uri(),
					e.getClass().getSimpleName()
			);

            logger.debug("Unexpected gateway error details for {}", request.uri(), e);

			return new ForwardingResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal gateway error");
		}
	}
}
