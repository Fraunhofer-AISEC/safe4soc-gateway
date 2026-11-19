# Compilation 

The project is configured for the IDE Intellij Community Edition.
Is uses GraalVM to produce an executable with faster startup times and less memory footprint during execution.

## Requirements
To create the image, run the following goal. NOTE: GraalVM 22.3+ is required.
For installation instructions please see 
- Docker and docker-compose must be installed beforehand
- Install GraalVM https://www.graalvm.org/latest/getting-started/
- Install gcc glibc-devel zlib-devel etc. https://www.graalvm.org/latest/reference-manual/native-image/


### GraalVM Installation Via SDKMAN (a simplified option)
```
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
```

```
sdk install java 21.0.2-graalce
sdk use java 21.0.2-graalce
```
### Verify
```
java -version
echo $GRAALVM_HOME 
echo $JAVA_HOME
```
should show: GraalVM CE 24.x.x
and both variables need to show to the installed GraalVM directory on your machine.
Please note, do not use Graal VM 25 or above as it is not compatible with the Java-version used.

## Building Instructions
In main project's path run
```
./gradlew clean nativeCompile
```
Then a binary is compiled which is later used for the docker build.
The compilation time takes about 5 minutes on a modern laptop.
You can do a test of the app as follows (expect missing configuration issues at this stage):
```
build/native/nativeCompile/s4sgateway
```
Then, you can build the docker container image:
```
docker build -t s4sgateway .  
```
Testing keystores must be available for propper configuration which must be created by calling:
```
cd pki-test
./generate-mtls-certs.sh
cd ..
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

If all services are up and running the gateways can be tested, please see the next sections.

## TOC

[Architecture Overview](01-architecture-overview.md)

Compilation Instructions (here)

[Configuration Instructions](03-configuration.md)

[Testing Instructions](04-testing.md)

[Demonstration Instructions](05-demonstration.md)

[Additional Links](09-additional-links.md)