package de.fraunhofer.aisec.s4sgateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Set;

@ConfigurationProperties("gateway.security")
public record GatewayAccessProperties(
        @DefaultValue({"127.0.0.0/8", "::1/128"}) List<String> internalNetworks,
        List<CertificateGrant> certificates) {

    public GatewayAccessProperties {
        certificates = certificates == null ? List.of() : List.copyOf(certificates);
    }

    public record CertificateGrant(String sha256, Set<Permission> permissions) {}

    public enum Permission {
        SOC,
        ISA_ADMIN,
        GATEWAY_ADMIN,
        MONITORING_SERVICE
    }
}
