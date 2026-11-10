# Compilation 

The project is configured for the IDE Intellij Community Edition.
Is uses GraalVM to produce an executable with faster startup times and less memory footprint during execution.

## Requirements
To create the image, run the following goal. NOTE: GraalVM 22.3+ is required.
For installation instructions please see 
- Docker and docker-compose must be installed beforehand
- Install GraalVM https://www.graalvm.org/latest/getting-started/
- Install gcc glibc-devel zlib-devel etc. https://www.graalvm.org/latest/reference-manual/native-image/


## Building Instructions
In main project's path run
```
./gradlew nativeCompile
```
Then a binary is compiled which is later used for the docker build.
The compilation time takes about 5 minutes on a modern laptop.
You can do a test of the app as follows:
```
build/native/nativeCompile/s4sgateway
```
Then, you can build the docker container image:
```
docker build -t s4sgateway .  
```
Once the image was built these commands can launch the kafka services and the gateways
Launching kafka.
```
docker compose up -d rest-proxy control-center schema-registry broker
docker compose up -d gateway-receiver gateway-sender
```
Launching gateway-sender (separate console recommended)
```
docker compose up gateway-sender 
```
Launching gateway-receiver (separate console recommended)
```
docker compose up gateway-receiver 
```
Afterward, the containers can be tested using postman collection. Get postman here: https://www.postman.com/downloads/
Once installed the testing collection (SAFE4SOC.postman_collection.json) which contains predefined REST API requests 
can be imported via File -> Import.