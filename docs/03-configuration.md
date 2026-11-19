# Configuration

The gateway must be configured using the file application.properties. 
A different version of this file is used for a gateway taking either the sender or receiver role, 
respectively the files:
- application.properties (for debugging directly in IntelliJ IDE)
- receiver.properties
- sender.properties
- docker.properties (for launch via docker-compose script.)

The files are mapped into the corresponding docker container via docker-compose.yaml.
Please edit config in docker-compose file as otherwise the config will be overwritten.

The following entries must be specified for the gateway in sender mode:
```
gatewayRole=SENDER
```
This switch lets the gateway act as sending gateway. Options are either SENDER or RECEIVER.
```
relevantHeaders={'content-type','accept','user-Agent','accept-Encoding','safe4soc-receiver-hostname','safe4soc-receiver-port'}
```
This array lists headers to be forwarded within the gateway. All other headers are dropped.

## Sender Gateway Properties
```
gatewayDomainRC=localhost
gatewayPortRC=8081
```
These entries specify the destination domain and port of the receiving gateway where messages must be forwarded to.

```
gatewayDomainSD=localhost
gatewayPortSD=8080
```
These entries specify self-information for the gateway, needed for alert entry enrichment, for instance.

## Receiver Gateway Properties
```
kafkaDomainLocal=kafkalocal
kafkaPortLocal=8082
```
These entries identify where the local kafka database can be found.

## SSL Activation or Deactivation (DEBUG Connection Settings)

The gateway has a setting to control the inbound SSL configuration (Spring boot VM settings), 
and the outbound SSL configuration (Java internal SSL Context object).
Both can be activated or deactivated separately using the following flags in docker-compose.

```
      GATEWAY_PROTOCOL: https     # for communication from gateway sender to gateway receiver
      SSL_INBOUND_ACTIVATED: true
      SSL_OUTBOUND_ACTIVATED: true
```

For debug purposes in a testing environment the SSL-settings can be both be set to false. 
This means that neither the initial connection from the outside inside the Gateway-sender nor the forwarding from Gateway-sender to Geteway-receiver will apply HTTPS but only HTTP, which is of course not acceptable for a production environment but ONLY FOR TESTING PURPOSES!
The file docker-compose-debug-http.yaml is properly configured to work out of the box and can be launched using:

```bash
docker compose up -d rest-proxy control-center schema-registry broker 
docker compose -f docker-compose-debug-http.yaml up gateway-sender
docker compose -f docker-compose-debug-http.yaml up gateway-receiver
```

## Information Sharing Agreement Configuration
The entry about the ISA is important, as it also contains the config about necessary filtering and enrichment instructions.
Please note: JSON objects across multiple lines must be formatted with \ at the end of each line is the properties files.

```
isaConfig=\
{\
  "provider_reason": "compliance",\
  "intended_use": "alert_gathering",\
  "data_usage_policies": {\
    "use_until": "2024-12-12",\
    "use_after": "2024-09-12",\
    "share_further": "false"\
  },\
  "data_sender_SIEM_brand": "Example Company A",\
  "data_sender_SIEM_version": "Version 0.1",\
  "IDMEFv2_attributes": {\
    "attributes_to_transmit": [],\
    "attributes_to_neglect": [],\
    "attributes_to_anonymize": [],\
    "attributes_to_pseudonemize": []\
  },\
  "sending_mode": "",\
  "encryption_requirements": [],\
  "authentication_requirements": [],\
  "connection_requirements": [],\
```
The basic attributes above were derived from the requirements specified and might be frequently adapted
in meaning and structure.

```
  "filterConfig": {\
    "Version": "",\
    "ID": "",\
    "Status": "",\
    "OrganisationName": "",\
    "CreateTime": "",\
    "StartTime": "",\
    "Cause": "",\
    "Category": "",\
    "Priority": "",\
    "Confidence": "",\
    "Description": "",\
    "Analyzer": {\
      "IP": "",\
      "Name": "",\
      "Hostname": "",\
      "Type": "",\
      "Model": "",\
      "Category": "",\
      "Data": "",\
      "Method": "",\
      "GeoLocation": "",\
      "UnLocation": "",\
      "Location": ""\
    },\
    "Sensor": "",\
    "Source": {\
      "IP": "",\
      "Protocol": ""\
    },\
    "Target": ""\
  },\
```
The Filtering configuration uses a nested list in JSON format.
If identifiers for key value pairs are found, the entry's content is replaced with an empty string ("").
If identifiers for nested objects are found, either the complete object is cleaned, when no sub-entries were given, 
or only the sub entries mentioned are replaced with blank spaces or empty arrays, depending on the element.
The list above is an example of a configuration which filters all entries.

```
  "enrichmentConfig": {\
    "CreateTime": "",\
    "StartTime": ""\
  }\
}
```
The enrichment configuration uses a nested list in JSON format, also.
However, each enrichment functionality must be implemented specifically. 
The class Utils contains a prepared nested case structure for all elements to individually implement 
an enrichment where necessary.

## Filtering and Enrichment Examples

When the Gateway receives the following IDMEFv2 message:
```
{
  "Version": "2.D.V04",
  "ID": "e5f9bbae-163e-42f9-a2f2-0daaf78fefb1",
  "Status": "Incident",
  "OrganisationName": "acme.eu",
  "CreateTime": "",
  "StartTime": "2021-01-18T23:33:04.52Z",
  "Cause": "Malicious",
  "Category": [
    "Recon.Scanning"
  ],
  "Priority": "Medium",
  "Confidence": 0.9,
  "Description": "An IDS event of type \"Unknown IP Protocol\" was observed",
  "Analyzer": {
    "IP": "172.10.5.31",
    "Name": "Core router",
    "Hostname": "core-router.example.com",
    "Type": "Cyber",
    "Model": "Cisco 4532",
    "Category": [
      "NIDS"
    ],
    "Data": [
      "Datagram"
    ],
    "Method": [
      "signature"
    ],
    "GeoLocation": "+48.75726,+2.299528,+65.1",
    "UnLocation": "GR ATH",
    "Location": "SBDX1"
  },
  "Sensor": [
    {
      "IP": "172.10.5.31",
      "Name": "Core router",
      "CaptureZone": "[{\"type\":\"ipv4-addr\",\"spec_version\":\"2.1\",\"id\": \"ipv4-addr--ff26c055-6336-5bc5-b98d-13d6226742dd\",\"value\": \"172.17.0.0\"},{\"type\":\"ipv4-net-mask\",\"spec_version\":\"2.1\",\"id\":\"ipv4-net-mask--ff26c055-6336-5bc5-b98d-13d6226742dd\",\"value\":\"255.255.0.0\"}]"
    }
  ],
  "Source": [
    {
      "IP": "77.222.222.115",
      "Protocol": [
        "Unknown"
      ]
    }
  ],
  "Target": [
    {
      "IP": "172.20.7.25",
      "Port": [
        645
      ],
      "Location": "Datacenter SBX1"
    }
  ]
}
```
and processes it using the configuration from above, the output will be:
```
{
  "Status": "",
  "Category": [],
  "Description": "",
  "Priority": "",
  "CreateTime": "",
  "StartTime": "",
  "Analyzer": {
    "Type": "",
    "Category": [],
    "IP": "",
    "Hostname": "",
    "Model": "",
    "Data": [],
    "UnLocation": "",
    "Method": [],
    "GeoLocation": "",
    "Name": "",
    "Location": ""
  },
  "Source": [],
  "Target": [],
  "Version": "",
  "Confidence": "",
  "Cause": "",
  "OrganisationName": "",
  "ID": "",
  "Sensor": []
}
```
Please note that the fields CreateTime and StartTime are enriched by the gateway, if no entry was found.
Enrichment of timestamps can be made and look like this:
```
  "CreateTime": "2025-04-02T09:16:57.16Z",
  "StartTime": "2025-04-02T09:16:57.69Z",
```


## Configuring the Filtering for IDMEFv2

Two ways exist to update the gateway configuration: 
1. via Rest API call to /setisa
2. via change of application.properties file and a Rest API call to /reloadconfig

The following example sets an ISA which filters the source IP addresses:
```declarative
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
Expected Output in curl console:
```
Processed ISA update.
```
Expected output in gateway console:
```declarative
gateway-sender-1  | 2025-07-08T07:48:47.924Z DEBUG 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : SetISA API function was called
gateway-sender-1  | 2025-07-08T07:48:47.924Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Loading Information Sharing Agreement and Filter settings ...
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Loaded this ISA:{"data_usage_policies":{"use_until":"2024-12-12","use_after":"2024-09-12","share_further":"false"},"data_sender_SIEM_version":"Version 0.1","intended_use":"alert_gathering","IDMEFv2_attributes":{"attributes_to_anonymize":[],"attributes_to_neglect":[],"attributes_to_pseudonemize":[],"attributes_to_transmit":[]},"sending_mode":"","encryption_requirements":[],"authentication_requirements":[],"enrichmentConfig":{},"data_sender_SIEM_brand":"Example Company A","connection_requirements":[],"filterConfig":{"Source":{"IP":""}},"provider_reason":"compliance"}
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Checking given configuration for role=SENDER ...
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Config gatewaySD=gateway-sender:10080
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Config gatewayRC=gateway-receiver:10081
gateway-sender-1  | 2025-07-08T07:48:47.925Z  INFO 1 --- [s4s-gateway] [io-10080-exec-2] d.f.aisec.s4sgateway.Controller          : Config relevantHeaders=[content-type, accept, user-Agent, accept-Encoding, safe4soc-receiver-hostname, safe4soc-receiver-port]
```

or the reload endpoint which reads the current stored file within the docker container:
```declarative
curl --location 'localhost:10080/reloadconfig' \
--data ''
```
Expected Output in curl console:
```
Configuration reload attempt was processed, please see internal logs for results.
```

Expected output in gateway console:
```declarative
gateway-sender-1  | 2025-07-08T07:55:21.230Z DEBUG 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Reload Configuration function was called
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Loading Information Sharing Agreement and Filter settings ...
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Loaded this ISA:{"data_usage_policies":{"use_until":"2024-12-12","use_after":"2024-09-12","share_further":"false"},"data_sender_SIEM_version":"Version 0.1","intended_use":"alert_gathering","IDMEFv2_attributes":{"attributes_to_anonymize":[],"attributes_to_neglect":[],"attributes_to_pseudonemize":[],"attributes_to_transmit":[]},"sending_mode":"","encryption_requirements":[],"authentication_requirements":[],"enrichmentConfig":{"CreateTime":"","StartTime":""},"data_sender_SIEM_brand":"Example Company A","connection_requirements":[],"filterConfig":{},"provider_reason":"compliance"}
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Checking given configuration for role=SENDER ...
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Config gatewaySD=gateway-sender:10080
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Config gatewayRC=gateway-receiver:10081
gateway-sender-1  | 2025-07-08T07:55:21.230Z  INFO 1 --- [s4s-gateway] [io-10080-exec-5] d.f.aisec.s4sgateway.Controller          : Config relevantHeaders=[content-type, accept, user-Agent, accept-Encoding, safe4soc-receiver-hostname, safe4soc-receiver-port]
```

## TOC

[Architecture Overview](01-architecture-overview.md)

[Compilation Instructions](./compilaton.md)

Configuration Instructions(here)

[Testing Instructions](04-testing.md)

[Demonstration Instructions](05-demonstration.md)

[Additional Links](09-additional-links.md)