package de.fraunhofer.aisec.s4sgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.aisec.s4sgateway.security.PartnerIdentityService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sender")
class GatewaySecurityTests {
    private static final X509Certificate SOC = certificate("soc");
    private static final X509Certificate ISA = certificate("isa");
    private static final X509Certificate GATEWAY = certificate("gateway");
    private static final X509Certificate MONITOR = certificate("monitor");
    private static final X509Certificate UNKNOWN = certificate("unknown");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @DynamicPropertySource
    static void grants(DynamicPropertyRegistry registry) {
        grant(registry, 0, SOC, "SOC");
        grant(registry, 1, ISA, "ISA_ADMIN");
        grant(registry, 2, GATEWAY, "GATEWAY_ADMIN");
        grant(registry, 3, MONITOR, "MONITORING_SERVICE");
    }

    @Test
    void onlyAuthorizedSocCertificatesCanSubmitAlerts() throws Exception {
        String body = "{";
        mvc.perform(post("/alerts").header("Authorization", "Basic dXNlcjpwYXNz")
                        .header("X-Client-Cert", PartnerIdentityService.fingerprint(SOC)).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/alerts").with(client(UNKNOWN)).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/alerts").with(client(ISA)).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/alerts").with(client(SOC)).header("Origin", "https://external.example").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/alerts").with(client(SOC)).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("Invalid JSON")));
    }

    @Test
    void adminReadsAndWritesRequireSeparateGrantsAndCsrfForChanges() throws Exception {
        mvc.perform(get("/isa").with(client(SOC))).andExpect(status().isForbidden());
        mvc.perform(get("/status").with(client(SOC))).andExpect(status().isForbidden());
        mvc.perform(get("/status").with(client(ISA))).andExpect(status().isOk());
        String agreement = mvc.perform(get("/isa").with(client(ISA))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/setisa").with(client(ISA)).content(agreement)).andExpect(status().isForbidden());

        var isaToken = csrf(ISA);
        mvc.perform(post("/setisa").with(client(ISA)).with(isaToken).content(agreement))
                .andExpect(status().isOk());
        mvc.perform(post("/reloadconfig").with(client(ISA)).with(isaToken)).andExpect(status().isForbidden());
        mvc.perform(get("/reloadconfig").with(client(SOC))).andExpect(status().isForbidden());

        var gatewayToken = csrf(GATEWAY);
        mvc.perform(post("/setisa").with(client(GATEWAY)).with(gatewayToken).content(agreement))
                .andExpect(status().isForbidden());
        mvc.perform(post("/reloadconfig").with(client(GATEWAY)).with(gatewayToken)).andExpect(status().isOk());
    }

    @Test
    void healthIsNetworkRestrictedAndMetricsAlsoRequireMonitoringPermission() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health").with(externalAddress()).header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics").with(client(SOC))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").with(client(MONITOR)).with(externalAddress()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").with(client(MONITOR))).andExpect(status().isOk());
        mvc.perform(get("/actuator/env").with(client(SOC))).andExpect(status().isForbidden());
    }

    private RequestPostProcessor csrf(X509Certificate certificate) throws Exception {
        MvcResult result = mvc.perform(get("/csrf").with(client(certificate)))
                .andExpect(status().isOk()).andReturn();
        Cookie cookie = Objects.requireNonNull(result.getResponse().getCookie("XSRF-TOKEN"));
        var token = mapper.readTree(result.getResponse().getContentAsString());
        return request -> {
            request.setCookies(cookie);
            request.addHeader(token.path("headerName").asText(), token.path("token").asText());
            return request;
        };
    }

    private static RequestPostProcessor client(X509Certificate certificate) {
        return request -> {
            request.setSecure(true);
            request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[]{certificate});
            return request;
        };
    }

    private static RequestPostProcessor externalAddress() {
        return request -> {
            request.setRemoteAddr("192.0.2.10");
            return request;
        };
    }

    private static void grant(DynamicPropertyRegistry registry, int index, X509Certificate certificate, String permission) {
        String key = "gateway.security.certificates[" + index + "]";
        registry.add(key + ".sha256", () -> PartnerIdentityService.fingerprint(certificate));
        registry.add(key + ".permissions[0]", () -> permission);
    }

    private static X509Certificate certificate(String encoded) {
        X509Certificate certificate = mock(X509Certificate.class);
        try {
            when(certificate.getEncoded()).thenReturn(encoded.getBytes(StandardCharsets.UTF_8));
        } catch (CertificateEncodingException e) {
            throw new AssertionError(e);
        }
        return certificate;
    }
}
