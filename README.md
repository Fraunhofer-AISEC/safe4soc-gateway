

# Safe4Soc Gateway Sender and Receiver

## General Info

This project provides code and testing functionality to create a secure gateway for date transmission 
in the context of the project SAFE4SOC (https://safe4soc.eu/).
It is configured for the IDE Intellij Community Edition.
Is uses GraalVM to produce an executable with faster startup times and less memory footprint during execution.

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

The image can take either the role of a sender, or the role of a receiver, depending on the configuration.
The docker-compose-file reads these config files:
- receiver.properties
- sender.properties


## Kafka-Proxy Rest client

Docu here: https://docs.confluent.io/platform/current/kafka-rest/quickstart.html

Source here: https://gist.github.com/ayubmalik/9300bbd8f62ce401c4d1f69eae0cb5d7

Produce a message using JSON with the value '{ "foo": "bar" }' to the topic jsontest
curl -X POST -H "Content-Type: application/vnd.kafka.json.v2+json" \
--data '{"records":[{"value":{"foo":"bar"}}]}' "http://localhost:38082/topics/jsontest"

Expected output from preceding command
{
"offsets":[{"partition":0,"offset":0,"error_code":null,"error":null}],"key_schema_id":null,"value_schema_id":null
}

Create a consumer for JSON data in "my_json_consumer_group" consumer group, starting at the beginning of the topic's
log and subscribe to a topic. Then consume some data using the base URL in the first response.
Finally, close the consumer with a DELETE to make it leave the group and clean up
its resources.
curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" \
--data '{"name": "my_consumer_instance", "format": "json", "auto.offset.reset": "earliest"}' \
http://localhost:38082/consumers/my_json_consumer_group

Expected output from preceding command
{
"instance_id":"my_consumer_instance",
"base_uri":"http://localhost:38082/consumers/my_json_consumer_group/instances/my_consumer_instance"
}

curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" --data '{"topics":["jsontest"]}' \
http://localhost:38082/consumers/my_json_consumer_group/instances/my_consumer_instance/subscription
No content in response

curl -X GET -H "Accept: application/vnd.kafka.json.v2+json" \
http://localhost:38082/consumers/my_json_consumer_group/instances/my_consumer_instance/records

Expected output from preceding command
[
{"key":null,"value":{"foo":"bar"},"partition":0,"offset":0,"topic":"jsontest"}
]

curl -X DELETE -H "Content-Type: application/vnd.kafka.v2+json" \
http://localhost:38082/consumers/my_json_consumer_group/instances/my_consumer_instance
No content in response


More info here: https://docs.confluent.io/platform/current/kafka-rest/quickstart.html



### Setting up the IDMEFv2 

coming soon


## Additional Links and Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.3.5/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.3.5/gradle-plugin/packaging-oci-image.html)
* [GraalVM Native Image Support](https://docs.spring.io/spring-boot/3.3.5/reference/packaging/native-image/introducing-graalvm-native-images.html)

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
* [Configure AOT settings in Build Plugin](https://docs.spring.io/spring-boot/3.3.5/how-to/aot.html)
