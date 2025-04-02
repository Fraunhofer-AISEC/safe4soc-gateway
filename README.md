

# Safe4Soc Gateway Sender and Receiver

## General Info

This project provides code and testing functionality to create a secure gateway for date transmission 
in the context of the project SAFE4SOC (https://safe4soc.eu/).
It is configured for the IDE Intellij Community Edition.
Is uses GraalVM to produce an executable with faster startup times and less memory footprint during execution.

## Architecture Overview

![Architecture Image should be here](./Architecture.drawio.png)
The gateway consists of a sender gateway on the sender side and the receiving gateway on the receiver side.
Kafka does not natively support JSON objects send via HTTP REST, therefore a translation was provided by the Kafka 
developers to translate HTTP Rest API calls into native Kafka protocol communication.

The sender gateway receives IDMEFv2 as JSON formatet alert message via HTTP message. 
For testing or debugging curl scripts or the postman collection can be used for testing.
The gateway sender afterwards performs configured filtering or enrichment and forwards the 
alert to the receiver side gateway. 
Here the alert is also filtered or enriched as configured and afterwards sent to the Kafka HTTP Rest API container.
The Kafka REST API container does the translation to the final destination, the Kafka database which is locally 
reachable on the receiver side.

## Requirements
To create the image, run the following goal. NOTE: GraalVM 22.3+ is required.
For installation instructions please see 
- Docker and docker-compose must be installed beforehand
- Install GraalVM https://www.graalvm.org/latest/getting-started/
- Install gcc glibc-devel zlib-devel etc. https://www.graalvm.org/latest/reference-manual/native-image/

## Compilation 
In main project's path run
```
$ ./gradlew nativeCompile
```
Then a binary is compiled which is later used for the docker build. 
You can do a test of the app as follows:
```
$ build/native/nativeCompile/s4sgateway
```
Then, you can build the docker container image:
```
$ docker build -t s4sgateway .  
```
Once the image was built these commands can launch the kafka services and the gateways
Launching kafka:
```
$ docker-compose up zookeeper kafka schema-registry  kafka-rest 
```
Launching gateway-sender (separate console recommended)
```
$ docker-compose up gateway-sender 
```
Launching gateway-receiver (separate console recommended)
```
$ docker-compose up gateway-receiver 
```
Afterward, the containers can be tested using postman collection. Get postman here: https://www.postman.com/downloads/
Once installed the testing collection (SAFE4SOC.postman_collection.json) which contains predefined REST API requests 
can be imported via File -> Import.

## Configuration

The gateway must be configured using the file application.properties. 
A different version of this file is used for a gateway taking either the sender or receiver role, 
respectively the files:
- application.properties (for debugging directly in IntelliJ IDE)
- receiver.properties
- sender.properties

The files are mapped into the corresponding docker container via docker-compose.yaml.

The following entries must be specified for the gateway in sender mode:
```
gatewayRole=SENDER
```
This switch lets the gateway act as sending gateway. Options are either SENDER or RECEIVER.
```
relevantHeaders={'content-type','accept','user-Agent','accept-Encoding','safe4soc-receiver-hostname','safe4soc-receiver-port'}
```
This array lists headers to be forwarded within the gateway. All other headers are dropped.

### Sender Gateway Properties
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

### Receiver Gateway Properties
```
kafkaDomainLocal=kafkalocal
kafkaPortLocal=8082
```
These entries identify where the local kafka database can be found.

### Information Sharing Agreement Configuration
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

### Filtering and Enrichment Examples

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

## Kafka-Proxy Rest client

Docu here: https://docs.confluent.io/platform/current/kafka-rest/quickstart.html

Source here: https://gist.github.com/ayubmalik/9300bbd8f62ce401c4d1f69eae0cb5d7

Produce a message using JSON with the value '{ "foo": "bar" }' to the topic jsontest
```
curl -X POST -H "Content-Type: application/vnd.kafka.json.v2+json" \
--data '{"records":[{"value":{"foo":"bar"}}]}' "http://localhost:38082/topics/jsontest"
```
Expected output from preceding command
```
{
"offsets":[{"partition":0,"offset":0,"error_code":null,"error":null}],"key_schema_id":null,"value_schema_id":null
}
```
Create a consumer for JSON data in "my_json_consumer_group" consumer group, starting at the beginning of the topic's
log and subscribe to a topic. Then consume some data using the base URL in the first response.
Finally, close the consumer with a DELETE to make it leave the group and clean up
its resources.
```
curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" \
--data '{"name": "my_consumer_instance", "format": "json", "auto.offset.reset": "earliest"}' \
http://localhost:38082/consumers/my_json_consumer_group
```
Expected output from preceding command
```
{
"instance_id":"my_consumer_instance",
"base_uri":"http://localhost:38082/consumers/my_json_consumer_group/instances/my_consumer_instance"
}
```

```
curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" --data '{"topics":["jsontest"]}' \
http://localhost:38082/consumers/my_json_consumer_group/instances/my_consumer_instance/subscription
```
No content in response

```
curl -X GET -H "Accept: application/vnd.kafka.json.v2+json" \
http://localhost:38082/consumers/my_json_consumer_group/instances/my_consumer_instance/records
```
Expected output from preceding command
```
[
{"key":null,"value":{"foo":"bar"},"partition":0,"offset":0,"topic":"jsontest"}
]
```
To delete an entry:
```
curl -X DELETE -H "Content-Type: application/vnd.kafka.v2+json" \
http://localhost:38082/consumers/my_json_consumer_group/instances/my_consumer_instance
```
No content in response

More info here: https://docs.confluent.io/platform/current/kafka-rest/quickstart.html

### Setting up the local Demo for IDMEFv2 

To fully demonstrate how alerts are sent to Kafka through the gateway, the Kafka instance must be configured with
the right message scheme to understand IDMEFv2 structures and properly store them.
The following commands are necessary to do this.

TODO coming soon

## Additional Links and Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.3.5/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.3.5/gradle-plugin/packaging-oci-image.html)
* [GraalVM Native Image Support](https://docs.spring.io/spring-boot/3.3.5/reference/packaging/native-image/introducing-graalvm-native-images.html)

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
* [Configure AOT settings in Build Plugin](https://docs.spring.io/spring-boot/3.3.5/how-to/aot.html)
