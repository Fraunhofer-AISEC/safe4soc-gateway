# Configuration

The gateway must be configured using the file application.properties which is available under path ./src/main/resources. Please note that a copy of the contents of this resources folder in embedded into the GraalVM binary, in any case and must be overloaded using an external configuration file via docker volume as shown in the corresponding docker compose files.

## Configuration Files

Different configuration profiles were developed for configuration and testing within IntelliJ IDEA for sender and receiver. The corresponding files are:

- application.properties (main default config for debugging directly in IntelliJ IDEA)
- application-receiver.properties
- application-sender.properties
- application-tls-in-on.properties (profile that activates incoming tls config)
- application-tls-out-on.properties (profile that activates outgoing tls config)
- docker.properties (is loaded externally docker-compose script.)

Please edit config in docker-compose file as otherwise the config will be overwritten.


## Configuration Overview

### Configuration Table
The following table shows on overview of configuration values that can be set either via docker compose environment variable or within the Spring Boot configuration files. 
The meaning of the most important variables which is not self-explanatory is detailed in the sections below.

| Category 	| Spring Boot application.properties 	                     | Docker Compose environment variable 	| Sample value 	|
|---	|----------------------------------------------------------|---	|---	|
| General Configuration 	| spring.application.name 	                                |  	| s4s-gateway 	|
| General Configuration 	| gateway.version 	                                        |  	| 0.3 	|
| General Configuration 	| spring.profiles.active 	                                 |  	| docker 	|
| Gateway Configuration 	| gatewayRole 	                                            | GATEWAY_ROLE 	| SENDER 	|
| Gateway Configuration 	| gatewayDomainRC 	                                        | GATEWAY_RECEIVER_DOMAIN 	| gateway-receiver 	|
| Gateway Configuration 	| gatewayPortRC 	                                          | GATEWAY_RECEIVER_PORT 	| 10081 	|
| Gateway Configuration 	| gatewayDomainSD 	                                        | GATEWAY_SENDER_DOMAIN 	| gateway-sender 	|
| Gateway Configuration 	| gatewayPortSD 	                                          | GATEWAY_SENDER_PORT 	| 10080 	|
| Gateway Configuration 	| gatewayProtocol 	                                        | GATEWAY_PROTOCOL 	| https 	|
| Kafka Configuration 	| kafkaDomainLocal 	                                       | KAFKA_LOCAL_DOMAIN 	| kafka 	|
| Kafka Configuration 	| kafkaPortLocal 	                                         | KAFKA_LOCAL_PORT 	| 9092 	|
| Kafka Configuration 	| kafkaTopic 	                                             | KAFKA_TOPIC 	| idmefv2-alerts 	|
| Gateway Receiver Connection 	| gatewayReceiverURL 	                                     | GATEWAY_RECEIVER_URL 	| https://gateway-receiver:10081 	|
| Gateway Receiver Connection 	| gatewayReceiverUsername 	                                | GATEWAY_RECEIVER_USERNAME 	| gateway-user 	|
| Gateway Receiver Connection 	| gatewayReceiverPassword 	                                | GATEWAY_RECEIVER_PASSWORD 	| change-me 	|
| Spring Boot Server Configuration 	| server.port 	                                            | GATEWAY_PORT 	| 10080 	|
| Inbound SSL/TLS Configuration 	| server.ssl.enabled 	                                     | SSL_INBOUND_ACTIVATED 	| true 	|
| Inbound SSL/TLS Configuration 	| server.ssl.key-store 	                                   | SSL_KEY_STORE 	| /keystore.p12 	|
| Inbound SSL/TLS Configuration 	| server.ssl.key-store-password 	                          | SSL_KEY_STORE_PASSWORD 	| password 	|
| Inbound SSL/TLS Configuration 	| server.ssl.key-store-type 	                              |  	| PKCS12 	|
| Inbound SSL/TLS Configuration 	| server.ssl.key-alias 	                                   | SSL_KEY_ALIAS 	| 1 	|
| Inbound SSL/TLS Configuration 	| server.ssl.trust-store 	                                 | SSL_TRUST_STORE 	| /truststore.p12 	|
| Inbound SSL/TLS Configuration 	| server.ssl.trust-store-password 	                        | SSL_TRUST_STORE_PASSWORD 	| password 	|
| Inbound SSL/TLS Configuration 	| server.ssl.trust-store-type 	                            |  	| PKCS12 	|
| Inbound SSL/TLS Configuration 	| server.ssl.client-auth 	                                 | SSL_MTLS_SETTING 	| want 	|
| Outbound SSL/TLS Configuration (Java SSLContext) 	| sslOutboundActivated 	                                   | SSL_OUTBOUND_ACTIVATED 	| true 	|
| Outbound SSL/TLS Configuration (Java SSLContext) 	| trustStorePassword 	                                     | SSL_TRUST_STORE_PASSWORD 	| password 	|
| Outbound SSL/TLS Configuration (Java SSLContext) 	| trustStorePath 	                                         | SSL_TRUST_STORE 	| /truststore.p12 	|
| Outbound SSL/TLS Configuration (Java SSLContext) 	| keyStorePath 	                                           | SSL_KEY_STORE 	| /keystore.p12 	|
| Outbound SSL/TLS Configuration (Java SSLContext) 	| keyStorePassword 	                                       | SSL_KEY_STORE_PASSWORD 	| password 	|
| Administrative Endpoints 	| endpoints.reloadconfig.enabled 	                         | ENDPOINT_ACTIVE_RELOADCONFIG 	| true 	|
| Administrative Endpoints 	| endpoints.setisa.enabled 	                               | ENDPOINT_ACTIVE_SETISA 	| false 	|
| Message Processing Configuration 	| relevantHeaders 	                                        |  	| {'content-type','accept','user-Agent','accept-Encoding','safe4soc-receiver-hostname','safe4soc-receiver-port'} 	|
| Message Processing Configuration 	| isaPath 	                                                | ISA_PATH 	| /config/isa.json 	|
| Message Processing Configuration 	| IDMEFv2SchemaPath 	                                      | IDMEFv2SchemaPath 	| classpath:/schemas/idmefv2-schema.json 	|
| Logging Configuration 	| logging.level.root 	                                     |  	| INFO 	|
| Logging Configuration 	| logging.level. de.fraunhofer.aisec. s4sgateway 	         |  	| DEBUG 	|
| Logging Configuration 	| logging.level. org.springframework. boot.autoconfigure 	 |  	| INFO 	|
| Logging Configuration 	| logging.file.name 	                                      |  	| logs/spring-boot-app.log 	|
| Logging Configuration 	| logging.file.max-size 	                                  |  	| 10MB 	|
| Logging Configuration 	| logging.file.total-size-cap 	                            |  	| 50MB 	|

### Gateway Role

This switch lets the gateway act as sending gateway. Options are either SENDER or RECEIVER.
```
gatewayRole=SENDER
```

### Sender Gateway Properties
These entries specify the destination domain and port of the receiving gateway where messages must be forwarded to.

```
gatewayDomainRC=localhost
gatewayPortRC=8081
```

These entries specify self-information for the gateway, needed for alert entry enrichment, for instance.
```
gatewayDomainSD=localhost
gatewayPortSD=8080
```

### Receiver Gateway Properties
These entries identify where the local Kafka database can be found.
```
kafkaDomainLocal=kafkalocal
kafkaPortLocal=8082
```

### SSL Activation or Deactivation (DEBUG Connection Settings)

The gateway has a setting to control the inbound SSL configuration (Spring boot VM settings), 
and the outbound SSL configuration (Java internal SSL Context object).
Both can be activated or deactivated separately using the following flags in docker-compose.

```
      GATEWAY_PROTOCOL: https     # for communication from gateway sender to gateway receiver
      SSL_INBOUND_ACTIVATED: true
      SSL_OUTBOUND_ACTIVATED: true
```

For debug purposes in a testing environment the SSL-settings can be both be set to false. 
This means that neither the initial connection from the outside inside the Gateway-sender nor the forwarding from Gateway-sender to Geteway-receiver will apply HTTPS but only HTTP.
This setting is not acceptable for a production environment but ONLY FOR TESTING PURPOSES!

The file docker-compose-debug-http.yaml is properly configured to work out of the box and can be launched using:

```bash
docker compose up -d rest-proxy control-center schema-registry broker 
docker compose -f docker-compose-debug-http.yaml up gateway-sender
docker compose -f docker-compose-debug-http.yaml up gateway-receiver
```

### Relevant Headers

For information purposes the outgoing requests hold the role information as HTTP Headers.
This array lists headers to be forwarded within the gateway. All other headers are dropped.
```
relevantHeaders={'content-type','accept','user-Agent','accept-Encoding','safe4soc-receiver-hostname','safe4soc-receiver-port'}
```

### Additional Endpoints for Debugging

If configuration testing is required the following endpoints can be used to reload a configuration during runtime. However, they MUST BE DEACTIVATED in secure mode using the corresponding flags:
```
endpoints.reloadconfig.enabled: false
endpoints.setisa.enabled: false
```

## Information Sharing Agreement Configuration
The entry about the ISA is very important, as it contains the configuration about necessary filtering, enrichment, required fields, and alert drop instructions.
If no external ISA.json file is found under the specified "isaPath" by default a standard ISA embedded into the GraalVM compiled binary will be used.

What was used can be seen in the startup logs:
```
gateway-sender-1  | 2026-11-23T09:08:44.257Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Loading external ISA: /isa.json
gateway-sender-1  | 2026-11-23T09:08:44.258Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Loaded this ISA:{"data_usage_policies":... ..."provider_reason":"compliance"}
```

The following is a sample ISA.json file which can be found in ./src/main/ressources/isa/isa.json.

```json
{
  "provider_reason": "compliance",
  "intended_use": "alert_gathering",
  "data_usage_policies": {
    "use_until": "2024-12-12",
    "use_after": "2024-09-12",
    "share_further": "false"
  },
  "data_sender_SIEM_brand": "Example Company A",
  "data_sender_SIEM_version": "Version 0.1",
  "IDMEFv2_attributes": {
    "attributes_to_transmit": [],
    "attributes_to_neglect": [],
    "attributes_to_anonymize": [],
    "attributes_to_pseudonemize": []
  },
  "sending_mode": "",
  "encryption_requirements": [],
  "authentication_requirements": [],
  "connection_requirements": [],
  "filterConfig": {},
  "dropConfig": {},
  "enrichmentConfig": {},
  "requiredConfig": {}
}
```
The basic attributes above were derived from the requirements specified during the Safe4Soc project.

### Filter Configuration

The Filtering configuration uses a nested list in JSON format.
If identifiers for key value pairs are found, the entry's content is replaced with an empty string ("").
If identifiers for nested objects are found, either the complete object is cleaned, when no sub-entries were given,
or only the sub entries mentioned are replaced with blank spaces or empty arrays, depending on the element.

```
  "filterConfig": {
    "CreateTime": "",
    "StartTime": ""
  },
```

<details>
<summary>Click to see a list filtering all entries:</summary>

```                                    
  "filterConfig": {
    "Version": "",
    "ID": "",
    "Status": "",
    "OrganisationName": "",
    "CreateTime": "",
    "StartTime": "",
    "Cause": "",
    "Category": "",
    "Priority": "",
    "Confidence": "",
    "Description": "",
    "Analyzer": {
      "IP": "",
      "Name": "",
      "Hostname": "",
      "Type": "",
      "Model": "",
      "Category": "",
      "Data": "",
      "Method": "",
      "GeoLocation": "",
      "UnLocation": "",
      "Location": ""
    },
    "Sensor": "",
    "Source": {
      "IP": "",
      "Protocol": ""
    },
    "Target": ""
  },
```
</details>

If a value is stored inside the corresponding entry, it will be replaced. 

```
    "OrganisationName": "Anonym",
```

### Enrichment

The enrichment configuration uses a nested list in JSON format, also.
However, each enrichment functionality must be implemented specifically.
The class Utils contains a prepared nested case structure for all elements to individually implement
an enrichment where necessary.

```
  "enrichmentConfig": {\
    "CreateTime": "",\
    "StartTime": ""\
  }\
}
```

Currently, the feature is available to enrich alerts with missing timestamps. 
If it is missing the timestamp within the running gateway will be used as value in the same format for the CreateTime or StartTime field.


### Drop Configuration

This JSON element inside the ISA lists elements that cause the forwarding of alerts to be completely dropped and aborted. 
If values that are configured here are found in an IDMEFv2 alert, the dropping is triggered.

```
  "dropConfig": {
    "Version": "",
    "ID": "",
    "Status": "",
    "OrganisationName": "",
    "CreateTime": "",
    "StartTime": "",
    "Cause": "",
    "Category": "",
    "Priority": "High",
    "Confidence": "",
    "Description": ""
  },
```

As can be seen above, these fields can be used for drop configuration. 
If an alert is processed with a priority value "High" it will NOT be forwarded, but dropped.

### Required Configuration

This JSON element inside the ISA lists elements that that must be non-empty values, otherwise the forwarding will be aborted.
Please note this is distìnguished from filtering configuration.

```
  "requiredConfig": {
    "Version": "",
    "ID": "",
    "StartTime": "",
    "Status": "",
    "Source": "",
    "Target": ""
  }
```

### Updating the ISA via REST Endpoint

Two ways exist to update the gateway configuration ONLY IN DEBUG MODE for security reasons: 
1. via Rest API call to /setisa
2. via change of application.properties file and a Rest API call to /reloadconfig

The following example sets an ISA which filters the source IP addresses:
```bash
curl --location 'localhost:10080/setisa' \
--header 'Content-Type: application/json' \
--data '{
    "provider_reason": "compliance",
    "intended_use": "alert_gathering",
    "data_usage_policies": {
        "use_until": "2024-12-12",
        "use_after": "2024-09-12",
        "share_further": "false"
    },
    "data_sender_SIEM_brand": "Example Company A",
    "data_sender_SIEM_version": "Version 0.1",
    "IDMEFv2_attributes": {
        "attributes_to_transmit": [],
        "attributes_to_neglect": [],
        "attributes_to_anonymize": [],
        "attributes_to_pseudonemize": []
    },
    "sending_mode": "",
    "encryption_requirements": [],
    "authentication_requirements": [],
    "connection_requirements": [],
    "filterConfig": {
            "Source": {
              "IP": ""
            }
    },
    "enrichmentConfig": {
    }
}'
```

<details>
<summary>Click to see expected output:</summary>

Http response text:
```
Processed ISA update.
```
Gateway console:
```
gateway-sender-1  | 2025-07-08T07:48:47.924Z DEBUG 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : SetISA API function was called
gateway-sender-1  | 2025-07-08T07:48:47.924Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Loading Information Sharing Agreement and Filter settings ...
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Loaded this ISA:{"data_usage_policies":{"use_until":"2024-12-12","use_after":"2024-09-12","share_further":"false"},"data_sender_SIEM_version":"Version 0.1","intended_use":"alert_gathering","IDMEFv2_attributes":{"attributes_to_anonymize":[],"attributes_to_neglect":[],"attributes_to_pseudonemize":[],"attributes_to_transmit":[]},"sending_mode":"","encryption_requirements":[],"authentication_requirements":[],"enrichmentConfig":{},"data_sender_SIEM_brand":"Example Company A","connection_requirements":[],"filterConfig":{"Source":{"IP":""}},"provider_reason":"compliance"}
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Checking given configuration for role=SENDER ...
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Config gatewaySD=gateway-sender:10080
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Config gatewayRC=gateway-receiver:10081
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Config relevantHeaders=[content-type, accept, user-Agent, accept-Encoding, safe4soc-receiver-hostname, safe4soc-receiver-port]
```
</details>


or the reload endpoint which reads the current stored file within the docker container:
```declarative
curl --location 'localhost:10080/reloadconfig' \
--data ''
```


<details>
<summary>Click to see expected output:</summary>

Http request response text:
```
Configuration reload attempt was processed, please see internal logs for results.
```

Gateway console:
```declarative
gateway-sender-1  | 2025-07-08T07:55:21.230Z DEBUG 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Reload Configuration function was called
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Loading Information Sharing Agreement and Filter settings ...
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Loaded this ISA:{"data_usage_policies":{"use_until":"2024-12-12","use_after":"2024-09-12","share_further":"false"},"data_sender_SIEM_version":"Version 0.1","intended_use":"alert_gathering","IDMEFv2_attributes":{"attributes_to_anonymize":[],"attributes_to_neglect":[],"attributes_to_pseudonemize":[],"attributes_to_transmit":[]},"sending_mode":"","encryption_requirements":[],"authentication_requirements":[],"enrichmentConfig":{"CreateTime":"","StartTime":""},"data_sender_SIEM_brand":"Example Company A","connection_requirements":[],"filterConfig":{},"provider_reason":"compliance"}
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Checking given configuration for role=SENDER ...
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Config gatewaySD=gateway-sender:10080
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Config gatewayRC=gateway-receiver:10081
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Config relevantHeaders=[content-type, accept, user-Agent, accept-Encoding, safe4soc-receiver-hostname, safe4soc-receiver-port]
```
</details>

## IDMEFv2 Schema Configuration
The IDMEFv2 Schema is very important, as it contains the acceptance rules for the incoming JSON structure of alerts.
If no external IDMEFv2.json file is found under the specified "IDMEFv2SchemaPath" by default a standard schema embedded into the GraalVM compiled binary will be used.

What was used can be seen in the startup logs:
```
gateway-receiver-1  | 2026-11-23T11:47:30.772Z  INFO 1 --- [s4s-gateway] [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 95 ms
gateway-receiver-1  | 2026-11-23T11:47:30.780Z DEBUG 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : JSON Schema path is: /idmefv2-schema.json
gateway-receiver-1  | 2026-11-23T11:47:30.780Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : Loading external JSON schema: /idmefv2-schema.json
gateway-receiver-1  | 2026-11-23T11:47:30.815Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : IDMEFv2 schema found in version: 2.D.V08
gateway-receiver-1  | 2026-11-23T11:47:30.817Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : IDMEFv2 schema successfully loaded
```
