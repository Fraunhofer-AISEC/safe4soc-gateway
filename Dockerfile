FROM ubuntu:jammy
COPY build/native/nativeCompile/s4sgateway /s4sgateway
COPY /src/main/resources/application.properties /application.properties
CMD ["/s4sgateway","--spring.config.location=/application.properties"]

