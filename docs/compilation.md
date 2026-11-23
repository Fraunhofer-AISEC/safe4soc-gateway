# Compilation 

The project is configured for the [IDE Intellij Community Edition](https://www.jetbrains.com/de-de/idea/).
The project uses [Spring Boot](https://docs.spring.io/), however it is compiled using [GraalVM](https://www.graalvm.org/) to produce an executable with faster startup times and less memory footprint during execution.

## Requirements

To create the image, run the following gradlew build command. For installation instructions please see 
- Docker and docker-compose must be installed beforehand
- Install GraalVM https://www.graalvm.org/latest/getting-started/ (NOTE: GraalVM 21.0.2 is working. GraalVM 25+ is not working!)
- Install gcc glibc-devel zlib-devel etc. https://www.graalvm.org/latest/reference-manual/native-image/

### GraalVM Installation Via SDKMAN (a simplified option)

Install [SDKMAN](https://sdkman.io/).
```
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
```
Download and switch to GraalVM.
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
should show: GraalVM CE 21.x.x
and both variables need to show to the installed GraalVM directory on your machine.
If not you need to configure your local Java setup.
Please note, do not use Graal VM 25 or above as it is not compatible with the Java-version used.

## Building Instructions Using Docker
In main project's path run
```
./gradlew clean nativeCompile
```

<details>
<summary>Click to see expected command line output:</summary>

```
./gradlew clean nativeCompile                                                                                                                                                         ─╯
Starting a Gradle Daemon (subsequent builds will be faster)

> Task :processAot

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.5)

2026-11-23T09:46:34.379+01:00  INFO 44155 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Starting Application using Java 17.0.5 with PID 44155 (/safe4soc-gateway/build/classes/java/main started by ubuntu in /safe4soc-gateway)
2026-11-23T09:46:34.427+01:00 DEBUG 44155 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Running with Spring Boot v3.3.5, Spring v6.1.14
2026-11-23T09:46:34.431+01:00  INFO 44155 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : No active profile set, falling back to 1 default profile: "default"

> Task :generateResourcesConfigFile
[native-image-plugin] Resources configuration written into /safe4soc-gateway/build/native/generated/generateResourcesConfigFile/resource-config.json

> Task :nativeCompile
[native-image-plugin] GraalVM Toolchain detection is disabled
[native-image-plugin] GraalVM location read from environment variable: JAVA_HOME
[native-image-plugin] Native Image executable path: /home/ubuntu/.sdkman/candidates/java/21.0.2-graalce/lib/svm/bin/native-image
Warning: The option '-H:ReflectionConfigurationResources=META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-websocket/tomcat-reflection.json' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: The option '-H:ReflectionConfigurationResources=META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-el/tomcat-reflection.json' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: The option '-H:ReflectionConfigurationResources=META-INF/native-image/com.networknt/json-schema-validator/reflect-config.json' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: The option '-H:ResourceConfigurationResources=META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-core/tomcat-resource.json' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: The option '-H:ReflectionConfigurationResources=META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-core/tomcat-reflection.json' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: The option '-H:ResourceConfigurationResources=META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-el/tomcat-resource.json' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: The option '-H:ResourceConfigurationResources=META-INF/native-image/com.networknt/json-schema-validator/resource-config.json' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: The option '-H:ResourceConfigurationResources=META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-websocket/tomcat-resource.json' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: Please re-evaluate whether any experimental option is required, and either remove or unlock it. The build output lists all active experimental options, including where they come from and possible alternatives. If you think an experimental option should be considered as stable, please file an issue.
========================================================================================================================
GraalVM Native Image: Generating 's4sgateway' (executable)...
========================================================================================================================
For detailed information and explanations on the build output, visit:
https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/BuildOutput.md
------------------------------------------------------------------------------------------------------------------------
[1/8] Initializing...                                                                                   (13.5s @ 0.14GB)
 Java version: 21.0.2+13, vendor version: GraalVM CE 21.0.2+13.1
 Graal compiler: optimization level: 2, target machine: x86-64-v3
 C compiler: gcc (linux, x86_64, 13.3.0)
 Garbage collector: Serial GC (max heap size: 80% of RAM)
 2 user-specific feature(s):
 - com.oracle.svm.thirdparty.gson.GsonFeature
 - org.springframework.aot.nativex.feature.PreComputeFieldFeature
------------------------------------------------------------------------------------------------------------------------
 2 experimental option(s) unlocked:
 - '-H:ResourceConfigurationResources' (origin(s): 'META-INF/native-image/com.networknt/json-schema-validator/native-image.properties' in 'file:///home/ubuntu/.gradle/caches/modules-2/files-2.1/com.networknt/json-schema-validator/2.0.8/44ff375e8959269386f740434731c94b5df51ceb/json-schema-validator-2.0.8.jar', 'META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-websocket/native-image.properties' in 'file:///home/ubuntu/.gradle/caches/modules-2/files-2.1/org.apache.tomcat.embed/tomcat-embed-websocket/10.1.31/6f755a475e44ece496ae3402de0c6c2dbea63921/tomcat-embed-websocket-10.1.31.jar', 'META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-core/native-image.properties' in 'file:///home/ubuntu/.gradle/caches/modules-2/files-2.1/org.apache.tomcat.embed/tomcat-embed-core/10.1.31/a91cc0a95157228d9cccb1a9e8fb9c77c048c937/tomcat-embed-core-10.1.31.jar', 'META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-el/native-image.properties' in 'file:///home/ubuntu/.gradle/caches/modules-2/files-2.1/org.apache.tomcat.embed/tomcat-embed-el/10.1.31/ab21108d69bb42160eff7b905f606a1d0c21ae67/tomcat-embed-el-10.1.31.jar')
 - '-H:ReflectionConfigurationResources' (origin(s): 'META-INF/native-image/com.networknt/json-schema-validator/native-image.properties' in 'file:///home/ubuntu/.gradle/caches/modules-2/files-2.1/com.networknt/json-schema-validator/2.0.8/44ff375e8959269386f740434731c94b5df51ceb/json-schema-validator-2.0.8.jar', 'META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-websocket/native-image.properties' in 'file:///home/ubuntu/.gradle/caches/modules-2/files-2.1/org.apache.tomcat.embed/tomcat-embed-websocket/10.1.31/6f755a475e44ece496ae3402de0c6c2dbea63921/tomcat-embed-websocket-10.1.31.jar', 'META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-core/native-image.properties' in 'file:///home/ubuntu/.gradle/caches/modules-2/files-2.1/org.apache.tomcat.embed/tomcat-embed-core/10.1.31/a91cc0a95157228d9cccb1a9e8fb9c77c048c937/tomcat-embed-core-10.1.31.jar', 'META-INF/native-image/org.apache.tomcat.embed/tomcat-embed-el/native-image.properties' in 'file:///home/ubuntu/.gradle/caches/modules-2/files-2.1/org.apache.tomcat.embed/tomcat-embed-el/10.1.31/ab21108d69bb42160eff7b905f606a1d0c21ae67/tomcat-embed-el-10.1.31.jar')
------------------------------------------------------------------------------------------------------------------------
Build resources:
 - 10.91GB of memory (71.1% of 15.35GB system memory, determined at start)
 - 14 thread(s) (100.0% of 14 available processor(s), determined at start)
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
[2/8] Performing analysis...  [*****]                                                                   (93.6s @ 2.10GB)
   18,001 reachable types   (90.3% of   19,944 total)
   28,351 reachable fields  (65.7% of   43,149 total)
   85,629 reachable methods (63.5% of  134,849 total)
    5,585 types,   624 fields, and 6,527 methods registered for reflection
       62 types,    63 fields, and    55 methods registered for JNI access
        4 native libraries: dl, pthread, rt, z
[3/8] Building universe...                                                                              (15.4s @ 1.53GB)
[4/8] Parsing methods...      [***]                                                                      (9.0s @ 1.57GB)
[5/8] Inlining methods...     [****]                                                                     (5.1s @ 1.69GB)
[6/8] Compiling methods...    [********]                                                                (76.4s @ 1.47GB)
[7/8] Layouting methods...    [***]                                                                      (8.6s @ 2.54GB)
[8/8] Creating image...       [***]                                                                     (11.2s @ 0.91GB)
  42.06MB (48.77%) for code area:    55,171 compilation units
  41.06MB (47.61%) for image heap:  427,180 objects and 312 resources
   3.12MB ( 3.62%) for other data
  86.25MB in total
------------------------------------------------------------------------------------------------------------------------
Top 10 origins of code area:                                Top 10 object types in image heap:
  14.47MB java.base                                           12.69MB byte[] for code metadata
   4.41MB tomcat-embed-core-10.1.31.jar                        6.06MB byte[] for java.lang.String
   3.74MB java.xml                                             4.29MB java.lang.Class
   2.07MB jackson-databind-2.17.2.jar                          4.12MB java.lang.String
   1.53MB spring-core-6.1.14.jar                               1.82MB byte[] for embedded resources
   1.51MB svm.jar (Native Image)                               1.51MB com.oracle.svm.core.hub.DynamicHubCompanion
   1.42MB spring-boot-3.3.5.jar                                1.11MB byte[] for reflection metadata
   1.35MB java.net.http                                      870.49kB java.lang.String[]
 958.19kB spring-beans-6.1.14.jar                            793.00kB byte[] for general heap data
 941.29kB spring-web-6.1.14.jar                              648.16kB c.o.svm.core.hub.DynamicHub$ReflectionMetadata
   9.32MB for 74 more packages                                 7.22MB for 3868 more object types
------------------------------------------------------------------------------------------------------------------------
Recommendations:
 INIT: Adopt '--strict-image-heap' to prepare for the next GraalVM release.
 HEAP: Set max heap for improved and more predictable memory usage.
 CPU:  Enable more CPU features with '-march=native' for improved performance.
------------------------------------------------------------------------------------------------------------------------
                       21.6s (8.9% of total time) in 192 GCs | Peak RSS: 4.72GB | CPU load: 10.35
------------------------------------------------------------------------------------------------------------------------
Produced artifacts:
 /safe4soc-gateway/build/native/nativeCompile/s4sgateway (executable)
========================================================================================================================
Finished generating 's4sgateway' in 3m 56s.
[native-image-plugin] Native Image written to: /safe4soc-gateway/build/native/nativeCompile

BUILD SUCCESSFUL in 5m 12s
10 actionable tasks: 10 executed
```
</details>

Then a binary is compiled which is later used for the docker build.
The compilation time takes about 5 minutes on a modern laptop.
You can do a test of the app as follows (expect missing configuration issues at this stage):
```
build/native/nativeCompile/s4sgateway
```
<details>
<summary>Click to see expected command line output:</summary>

Since this way the binary was started without proper configuration it should show the following errors, if it was compiled correctly:
```
build/native/nativeCompile/s4sgateway                                                                                                                                                 ─╯


  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.5)

2026-11-23T09:52:15.104+01:00  INFO 44465 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Starting AOT-processed Application using Java 21.0.2 with PID 44465 (/safe4soc-gateway/build/native/nativeCompile/s4sgateway started by ubuntu in /safe4soc-gateway)
2026-11-23T09:52:15.106+01:00 DEBUG 44465 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Running with Spring Boot v3.3.5, Spring v6.1.14
2026-11-23T09:52:15.107+01:00  INFO 44465 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : No active profile set, falling back to 1 default profile: "default"
2026-11-23T09:52:15.175+01:00  INFO 44465 --- [s4s-gateway] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2026-11-23T09:52:15.191+01:00  INFO 44465 --- [s4s-gateway] [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2026-11-23T09:52:15.193+01:00  INFO 44465 --- [s4s-gateway] [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.31]
2026-11-23T09:52:15.210+01:00  INFO 44465 --- [s4s-gateway] [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2026-11-23T09:52:15.211+01:00  INFO 44465 --- [s4s-gateway] [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 102 ms
2026-11-23T09:52:15.248+01:00 DEBUG 44465 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : JSON Schema path is: /schemas/idmefv2-schema.json
2026-11-23T09:52:15.249+01:00  WARN 44465 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : External JSON schema not found or not readable: /schemas/idmefv2-schema.json. Trying embedded schema inside binary instead.
2026-11-23T09:52:15.250+01:00  INFO 44465 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : Loading embedded JSON schema from JAR or Graal VM compiled binary: /schemas/idmefv2-schema.json
2026-11-23T09:52:15.251+01:00  WARN 44465 --- [s4s-gateway] [           main] w.s.c.ServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'controller': Unsatisfied dependency expressed through constructor parameter 1: Error creating bean with name 'jsonSchemaValidationService': Instantiation of supplied bean failed
2026-11-23T09:52:15.252+01:00  INFO 44465 --- [s4s-gateway] [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
Application run failed
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'controller': Unsatisfied dependency expressed through constructor parameter 1: Error creating bean with name 'jsonSchemaValidationService': Instantiation of supplied bean failed
        at org.springframework.beans.factory.aot.BeanInstanceSupplier.resolveAutowiredArgument(BeanInstanceSupplier.java:345)
        at org.springframework.beans.factory.aot.BeanInstanceSupplier.resolveArguments(BeanInstanceSupplier.java:265)
        at org.springframework.beans.factory.aot.BeanInstanceSupplier.get(BeanInstanceSupplier.java:204)
        at org.springframework.beans.factory.support.InstanceSupplier$1.get(InstanceSupplier.java:83)
        at org.springframework.beans.factory.support.DefaultListableBeanFactory.obtainInstanceFromSupplier(DefaultListableBeanFactory.java:949)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.obtainFromSupplier(AbstractAutowireCapableBeanFactory.java:1237)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1180)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:562)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522)
        at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:337)
        at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234)
        at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:335)
        at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200)
        at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:975)
        at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:971)
        at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:625)
        at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146)
        at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:754)
        at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:456)
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:335)
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1363)
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1352)
        at de.fraunhofer.aisec.s4sgateway.Application.main(Application.java:25)
        at java.base@21.0.2/java.lang.invoke.LambdaForm$DMH/sa346b79c.invokeStaticInit(LambdaForm$DMH)
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jsonSchemaValidationService': Instantiation of supplied bean failed
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.obtainFromSupplier(AbstractAutowireCapableBeanFactory.java:1243)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1180)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:562)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:522)
        at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:337)
        at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234)
        at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:335)
        at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200)
        at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:254)
        at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1443)
        at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1353)
        at org.springframework.beans.factory.support.ConstructorResolver.resolveAutowiredArgument(ConstructorResolver.java:904)
        at org.springframework.beans.factory.support.RegisteredBean.resolveAutowiredArgument(RegisteredBean.java:253)
        at org.springframework.beans.factory.aot.BeanInstanceSupplier.resolveAutowiredArgument(BeanInstanceSupplier.java:342)
        ... 23 more
Caused by: java.lang.IllegalStateException: Could not load or compile IDMEFv2 JSON schema
        at de.fraunhofer.aisec.s4sgateway.JsonSchemaValidationService.<init>(JsonSchemaValidationService.java:82)
        at de.fraunhofer.aisec.s4sgateway.JsonSchemaValidationService__BeanDefinitions.lambda$getJsonSchemaValidationServiceInstanceSupplier$0(JsonSchemaValidationService__BeanDefinitions.java:20)
        at org.springframework.util.function.ThrowingBiFunction.apply(ThrowingBiFunction.java:68)
        at org.springframework.util.function.ThrowingBiFunction.apply(ThrowingBiFunction.java:54)
        at org.springframework.beans.factory.aot.BeanInstanceSupplier.lambda$get$2(BeanInstanceSupplier.java:206)
        at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:58)
        at org.springframework.util.function.ThrowingSupplier.get(ThrowingSupplier.java:46)
        at org.springframework.beans.factory.aot.BeanInstanceSupplier.invokeBeanSupplier(BeanInstanceSupplier.java:214)
        at org.springframework.beans.factory.aot.BeanInstanceSupplier.get(BeanInstanceSupplier.java:206)
        at org.springframework.beans.factory.support.DefaultListableBeanFactory.obtainInstanceFromSupplier(DefaultListableBeanFactory.java:949)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.obtainFromSupplier(AbstractAutowireCapableBeanFactory.java:1237)
        ... 36 more
Caused by: java.io.FileNotFoundException: Embedded IDMEFv2 JSON schema not found: /schemas/idmefv2-schema.json
        at de.fraunhofer.aisec.s4sgateway.JsonSchemaValidationService.loadSchema(JsonSchemaValidationService.java:121)
        at de.fraunhofer.aisec.s4sgateway.JsonSchemaValidationService.<init>(JsonSchemaValidationService.java:60)
        ... 46 more
```
</details>

Then, you can build the docker container image:
```
docker build -t s4sgateway .  
```
<details>
<summary>Click to see expected command line output:</summary>

```
docker build -t s4sgateway .                                                                                                                                                          ─╯
[+] Building 2.9s (8/8) FINISHED                                                                                                                                             docker:default
 => [internal] load build definition from Dockerfile                                                                                                                                   0.1s
 => => transferring dockerfile: 261B                                                                                                                                                   0.1s
 => [internal] load metadata for docker.io/library/ubuntu:jammy                                                                                                                        0.0s
 => [internal] load .dockerignore                                                                                                                                                      0.1s
 => => transferring context: 2B                                                                                                                                                        0.1s
 => [internal] load build context                                                                                                                                                      2.0s
 => => transferring context: 90.46MB                                                                                                                                                   2.0s
 => CACHED [1/3] FROM docker.io/library/ubuntu:jammy                                                                                                                                   0.0s
 => [2/3] COPY build/native/nativeCompile/s4sgateway /s4sgateway                                                                                                                       0.2s
 => [3/3] COPY /src/main/resources/application.properties /application.properties                                                                                                      0.0s
 => exporting to image                                                                                                                                                                 0.3s
 => => exporting layers                                                                                                                                                                0.3s
 => => writing image sha256:8e9b9cf07ded2a1d8fd092f40eb74cdbebad372374b094a67fb98325192a28e6                                                                                           0.0s
 => => naming to docker.io/library/s4sgateway   
```
</details>

Testing keystores must be available for propper configuration which must be created by calling:
```
cd pki-test
./generate-mtls-certs.sh
cd ..
```
<details>
<summary>Click to see expected command line output (1st time):</summary>

```
./generate-mtls-certs.sh                                                                                                                                                              ─╯
Generating local development CA...
Certificate was added to keystore
Generating certificate for gateway-sender...
Certificate request self-signature ok
subject=C = DE, O = Safe4Soc, OU = Development, CN = gateway-sender
Created gateway-sender certificates and keystore.
Generating certificate for gateway-receiver...
Certificate request self-signature ok
subject=C = DE, O = Safe4Soc, OU = Development, CN = gateway-receiver
Created gateway-receiver certificates and keystore.

============================================
mTLS certificates generated successfully!
============================================

Output directory: /safe4soc-gateway/pki-test/mtls-certs

Truststore password: password
Keystore password:   password

Files:
/safe4soc-gateway/pki-test/mtls-certs/ca.crt
/safe4soc-gateway/pki-test/mtls-certs/ca.key
/safe4soc-gateway/pki-test/mtls-certs/ca.srl
/safe4soc-gateway/pki-test/mtls-certs/gateway-receiver/extensions.cnf
/safe4soc-gateway/pki-test/mtls-certs/gateway-receiver/gateway-receiver.crt
/safe4soc-gateway/pki-test/mtls-certs/gateway-receiver/gateway-receiver.csr
/safe4soc-gateway/pki-test/mtls-certs/gateway-receiver/gateway-receiver.key
/safe4soc-gateway/pki-test/mtls-certs/gateway-receiver/keystore.p12
/safe4soc-gateway/pki-test/mtls-certs/gateway-receiver/truststore.p12
/safe4soc-gateway/pki-test/mtls-certs/gateway-sender/extensions.cnf
/safe4soc-gateway/pki-test/mtls-certs/gateway-sender/gateway-sender.crt
/safe4soc-gateway/pki-test/mtls-certs/gateway-sender/gateway-sender.csr
/safe4soc-gateway/pki-test/mtls-certs/gateway-sender/gateway-sender.key
/safe4soc-gateway/pki-test/mtls-certs/gateway-sender/keystore.p12
/safe4soc-gateway/pki-test/mtls-certs/gateway-sender/truststore.p12
/safe4soc-gateway/pki-test/mtls-certs/truststore.p12

```
And if called a second time with key material already generated:
```
cd pki-test                                                                                                                                                                           ─╯
./generate-mtls-certs.sh
cd ..
Generating local development CA...
keytool error: java.lang.Exception: Certificate not imported, alias <safe4soc-dev-ca> already exists
```
If you want to create fresh key material rm the generated folder pki-test/mtls-certs.
```bash
rm -rf pki-test/mtls-certs
```
</details>

Once the image was built these commands can launch the gateways (each for a separate terminal).
```
docker compose up gateway-receiver
```
<details>
<summary>Click to see expected command line output:</summary>

```bash
docker compose up gateway-receiver                                                                                                                                                    ─╯

[+] up 2/2
 ✔ Network safe4soc-gateway_default              Created                                                                                                                                0.1s
 ✔ Container safe4soc-gateway-gateway-receiver-1 Created                                                                                                                                0.0s
Attaching to gateway-receiver-1
gateway-receiver-1  |
gateway-receiver-1  |   .   ____          _            __ _ _
gateway-receiver-1  |  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
gateway-receiver-1  | ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
gateway-receiver-1  |  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
gateway-receiver-1  |   '  |____| .__|_| |_|_| |_\__, | / / / /
gateway-receiver-1  |  =========|_|==============|___/=/_/_/_/
gateway-receiver-1  |
gateway-receiver-1  |  :: Spring Boot ::                (v3.3.5)
gateway-receiver-1  |
gateway-receiver-1  | 2026-11-23T09:07:17.589Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Starting AOT-processed Application using Java 21.0.2 with PID 1 (/s4sgateway started by root in /)
gateway-receiver-1  | 2026-11-23T09:07:17.589Z DEBUG 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Running with Spring Boot v3.3.5, Spring v6.1.14
gateway-receiver-1  | 2026-11-23T09:07:17.589Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : The following 1 profile is active: "docker"
gateway-receiver-1  | 2026-11-23T09:07:17.619Z  INFO 1 --- [s4s-gateway] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 10081 (https)
gateway-receiver-1  | 2026-11-23T09:07:17.639Z  INFO 1 --- [s4s-gateway] [           main] o.a.t.util.net.NioEndpoint.certificate   : Connector [https-jsse-nio-10081], TLS virtual host [_default_], certificate type [UNDEFINED] configured from keystore [/root/.keystore] using alias [1] with trust store [null]
gateway-receiver-1  | 2026-11-23T09:07:17.640Z  INFO 1 --- [s4s-gateway] [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
gateway-receiver-1  | 2026-11-23T09:07:17.640Z  INFO 1 --- [s4s-gateway] [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.31]
gateway-receiver-1  | 2026-11-23T09:07:17.644Z  INFO 1 --- [s4s-gateway] [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
gateway-receiver-1  | 2026-11-23T09:07:17.644Z  INFO 1 --- [s4s-gateway] [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 55 ms
gateway-receiver-1  | 2026-11-23T09:07:17.653Z DEBUG 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : JSON Schema path is: /idmefv2-schema.json
gateway-receiver-1  | 2026-11-23T09:07:17.653Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : Loading external JSON schema: /idmefv2-schema.json
gateway-receiver-1  | 2026-11-23T09:07:17.656Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : IDMEFv2 schema found in version: 2.D.V08
gateway-receiver-1  | 2026-11-23T09:07:17.659Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : IDMEFv2 schema successfully loaded
gateway-receiver-1  | 2026-11-23T09:07:17.678Z DEBUG 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Activating SSL for outbound connection!
gateway-receiver-1  | 2026-11-23T09:07:17.679Z  WARN 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Endpoint reloadconfig can be reached - only use in debug mode!
gateway-receiver-1  | 2026-11-23T09:07:17.679Z  WARN 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Endpoint setisa can be reached - only use in debug mode!
gateway-receiver-1  | 2026-11-23T09:07:17.692Z  INFO 1 --- [s4s-gateway] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 10081 (https) with context path '/'
gateway-receiver-1  | 2026-11-23T09:07:17.692Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Started Application in 0.12 seconds (process running for 0.159)
gateway-receiver-1  | 2026-11-23T09:07:17.692Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : SAFE4SOC-IS-Gateway version: 0.3
gateway-receiver-1  | 2026-11-23T09:07:17.692Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : HTTPS for Outbound connection active!
gateway-receiver-1  | 2026-11-23T09:07:17.692Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Loading Information Sharing Agreement and Filter settings ...
gateway-receiver-1  | 2026-11-23T09:07:17.693Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Loading external ISA: /isa.json
gateway-receiver-1  | 2026-11-23T09:07:17.694Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Loaded this ISA:{"data_usage_policies":{"use_until":"2024-12-12","use_after":"2024-09-12","share_further":"false"},"IDMEFv2_attributes":{"attributes_to_anonymize":[],"attributes_to_neglect":[],"attributes_to_pseudonemize":[],"attributes_to_transmit":[]},"sending_mode":"","encryption_requirements":[],"authentication_requirements":[],"requiredConfig":{"Status":"","Target":"","Version":"","StartTime":"","ID":"","Source":""},"enrichmentConfig":{"CreateTime":"","StartTime":""},"connection_requirements":[],"filterConfig":{"CreateTime":"","StartTime":""},"data_sender_SIEM_version":"Version 0.1","intended_use":"alert_gathering","dropConfig":{"Status":"","Category":"","Description":"","Version":"","Confidence":"","Priority":"High","CreateTime":"","Cause":"","StartTime":"","OrganisationName":"","ID":""},"data_sender_SIEM_brand":"Example Company A","provider_reason":"compliance"}
gateway-receiver-1  | 2026-11-23T09:07:17.695Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Checking given configuration for role=RECEIVER ...
gateway-receiver-1  | 2026-11-23T09:07:17.695Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Config gatewayRC=gateway-receiver:10081
gateway-receiver-1  | 2026-11-23T09:07:17.695Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Config kafkaLocal=rest-proxy:8082
gateway-receiver-1  | 2026-11-23T09:07:17.695Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Config relevantHeaders=[content-type, accept, user-Agent, accept-Encoding, safe4soc-receiver-hostname, safe4soc-receiver-port]
```
</details>

Launching gateway-sender (separate console recommended)
```
docker compose up gateway-sender 
```
<details>
<summary>Click to see expected command line output:</summary>

```bash
docker compose up gateway-sender                                                                                                                                                      ─╯

[+] up 1/1
 ✔ Container safe4soc-gateway-gateway-sender-1 Created                                                                                                                                  0.0s
Attaching to gateway-sender-1
gateway-sender-1  |
gateway-sender-1  |   .   ____          _            __ _ _
gateway-sender-1  |  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
gateway-sender-1  | ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
gateway-sender-1  |  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
gateway-sender-1  |   '  |____| .__|_| |_|_| |_\__, | / / / /
gateway-sender-1  |  =========|_|==============|___/=/_/_/_/
gateway-sender-1  |
gateway-sender-1  |  :: Spring Boot ::                (v3.3.5)
gateway-sender-1  |
gateway-sender-1  | 2026-11-23T09:08:44.153Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Starting AOT-processed Application using Java 21.0.2 with PID 1 (/s4sgateway started by root in /)
gateway-sender-1  | 2026-11-23T09:08:44.153Z DEBUG 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Running with Spring Boot v3.3.5, Spring v6.1.14
gateway-sender-1  | 2026-11-23T09:08:44.154Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : The following 1 profile is active: "docker"
gateway-sender-1  | 2026-11-23T09:08:44.184Z  INFO 1 --- [s4s-gateway] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 10080 (https)
gateway-sender-1  | 2026-11-23T09:08:44.188Z  WARN 1 --- [s4s-gateway] [           main] org.apache.tomcat.util.net.SSLUtilBase   : The JSSE TLS 1.3 implementation does not support post handshake authentication (PHA) and is therefore incompatible with optional certificate authentication
gateway-sender-1  | 2026-11-23T09:08:44.207Z  INFO 1 --- [s4s-gateway] [           main] o.a.t.util.net.NioEndpoint.certificate   : Connector [https-jsse-nio-10080], TLS virtual host [_default_], certificate type [UNDEFINED] configured from keystore [/root/.keystore] using alias [1] with trust store [null]
gateway-sender-1  | 2026-11-23T09:08:44.207Z  INFO 1 --- [s4s-gateway] [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
gateway-sender-1  | 2026-11-23T09:08:44.207Z  INFO 1 --- [s4s-gateway] [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.31]
gateway-sender-1  | 2026-11-23T09:08:44.211Z  INFO 1 --- [s4s-gateway] [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
gateway-sender-1  | 2026-11-23T09:08:44.211Z  INFO 1 --- [s4s-gateway] [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 57 ms
gateway-sender-1  | 2026-11-23T09:08:44.218Z DEBUG 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : JSON Schema path is: /idmefv2-schema.json
gateway-sender-1  | 2026-11-23T09:08:44.219Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : Loading external JSON schema: /idmefv2-schema.json
gateway-sender-1  | 2026-11-23T09:08:44.221Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : IDMEFv2 schema found in version: 2.D.V08
gateway-sender-1  | 2026-11-23T09:08:44.223Z  INFO 1 --- [s4s-gateway] [           main] d.f.a.s.JsonSchemaValidationService      : IDMEFv2 schema successfully loaded
gateway-sender-1  | 2026-11-23T09:08:44.241Z DEBUG 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Activating SSL for outbound connection!
gateway-sender-1  | 2026-11-23T09:08:44.242Z  WARN 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Endpoint reloadconfig can be reached - only use in debug mode!
gateway-sender-1  | 2026-11-23T09:08:44.242Z  WARN 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Endpoint setisa can be reached - only use in debug mode!
gateway-sender-1  | 2026-11-23T09:08:44.256Z  INFO 1 --- [s4s-gateway] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 10080 (https) with context path '/'
gateway-sender-1  | 2026-11-23T09:08:44.256Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Application         : Started Application in 0.119 seconds (process running for 0.122)
gateway-sender-1  | 2026-11-23T09:08:44.256Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : SAFE4SOC-IS-Gateway version: 0.3
gateway-sender-1  | 2026-11-23T09:08:44.256Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : HTTPS for Outbound connection active!
gateway-sender-1  | 2026-11-23T09:08:44.256Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Loading Information Sharing Agreement and Filter settings ...
gateway-sender-1  | 2026-11-23T09:08:44.257Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Loading external ISA: /isa.json
gateway-sender-1  | 2026-11-23T09:08:44.258Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Loaded this ISA:{"data_usage_policies":{"use_until":"2024-12-12","use_after":"2024-09-12","share_further":"false"},"IDMEFv2_attributes":{"attributes_to_anonymize":[],"attributes_to_neglect":[],"attributes_to_pseudonemize":[],"attributes_to_transmit":[]},"sending_mode":"","encryption_requirements":[],"authentication_requirements":[],"requiredConfig":{"Status":"","Target":"","Version":"","StartTime":"","ID":"","Source":""},"enrichmentConfig":{"CreateTime":"","StartTime":""},"connection_requirements":[],"filterConfig":{"CreateTime":"","StartTime":""},"data_sender_SIEM_version":"Version 0.1","intended_use":"alert_gathering","dropConfig":{"Status":"","Category":"","Description":"","Version":"","Confidence":"","Priority":"High","CreateTime":"","Cause":"","StartTime":"","OrganisationName":"","ID":""},"data_sender_SIEM_brand":"Example Company A","provider_reason":"compliance"}
gateway-sender-1  | 2026-11-23T09:08:44.258Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Checking given configuration for role=SENDER ...
gateway-sender-1  | 2026-11-23T09:08:44.258Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Config gatewaySD=gateway-sender:10080
gateway-sender-1  | 2026-11-23T09:08:44.258Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Config gatewayRC=gateway-receiver:10081
gateway-sender-1  | 2026-11-23T09:08:44.258Z  INFO 1 --- [s4s-gateway] [           main] d.f.aisec.s4sgateway.Controller          : Config relevantHeaders=[content-type, accept, user-Agent, accept-Encoding, safe4soc-receiver-hostname, safe4soc-receiver-port]
```
</details>

The Kafka backend must also be launched before the data transmission can be successful using: 

```bash
docker-compose up -d rest-proxy control-center schema-registry broker gateway-receiver gateway-sender
```

See Sections [Testing](testing.md) and [Demonstration](demonstration.md) for the next steps.

For shutdown use this command. 
```bash
docker compose down
```
<details>
<summary>Click to see expected command line output:</summary>

```docker compose down                                                                                                                                                                   ─╯
[+] down 3/3
 ✔ Container safe4soc-gateway-gateway-sender-1   Removed                                                                                                                                0.0s
 ✔ Container safe4soc-gateway-gateway-receiver-1 Removed                                                                                                                                0.0s
 ✔ Network safe4soc-gateway_default              Removed                                                                                                                                0.2s
 ```
</details>

## Building Instructions Using IntelliJ IDEA

The project can be opened using IntelliJ IDEA and the Spring Boot application can be launched and debugged directly using the run configuration profiles stored in folder ".run" in the project's root directory. One profile is used for the sender role and one for the receiver role. 

If natively launched within IntelliJ's execution, the behavior is the same as for the build using GraalVM, however, the startup time is slower and debugging is simplified.