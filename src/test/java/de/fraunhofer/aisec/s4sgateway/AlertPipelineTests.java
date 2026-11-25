package de.fraunhofer.aisec.s4sgateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.aisec.s4sgateway.config.GatewayRole;
import de.fraunhofer.aisec.s4sgateway.controller.AdminController;
import de.fraunhofer.aisec.s4sgateway.controller.AlertController;
import de.fraunhofer.aisec.s4sgateway.exception.AlertExceptionHandler;
import de.fraunhofer.aisec.s4sgateway.json.JsonObjectReader;
import de.fraunhofer.aisec.s4sgateway.policy.IsaService;
import de.fraunhofer.aisec.s4sgateway.processing.AlertProcessingService;
import de.fraunhofer.aisec.s4sgateway.transport.AlertTransport;
import de.fraunhofer.aisec.s4sgateway.validation.IDMEFValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlertPipelineTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonObjectReader reader = new JsonObjectReader(mapper);
    private final IsaService isa = new IsaService(reader, "");
    private final AlertTransport transport = mock(AlertTransport.class);
    private MockMvc mvc;
    private String alert;

    @BeforeEach
    void setUp() throws IOException {
        isa.reload();
        mvc = gatewayWith(new IDMEFValidator("", mapper));
        alert = new ClassPathResource("alerts/test_idmefv2_minimal.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private MockMvc gatewayWith(IDMEFValidator validator) {
        AlertProcessingService processing = new AlertProcessingService(reader, validator, isa);
        return MockMvcBuilders.standaloneSetup(
                        new AlertController(processing, transport),
                        new AdminController(isa, GatewayRole.SENDER, true, true))
                .setControllerAdvice(new AlertExceptionHandler())
                .build();
    }

    @Test
    void malformedOrSchemaInvalidAlertStopsBeforeTransport() throws Exception {
        mvc.perform(post("/alerts").content("{")).andExpect(status().isBadRequest());
        mvc.perform(post("/alerts").content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Incoming alert is not valid")));
        verifyNoInteractions(transport);
    }

    @Test
    void rejectedIsaUpdatePreservesThePolicyThatDeniesTheAlert() throws Exception {
        var agreement = reader.read(isa.current().asJson());
        agreement.putObject("dropConfig").set("ID", reader.read(alert).get("ID"));
        mvc.perform(post("/setisa").content(agreement.toString())).andExpect(status().isOk());
        mvc.perform(post("/setisa").content("{}")).andExpect(status().isBadRequest());

        mvc.perform(post("/alerts").content(alert))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("forbidden value found in ID")));
        verifyNoInteractions(transport);
    }

    @Test
    void transformedAlertMustStillPassSchemaValidation() throws Exception {
        var agreement = reader.read(isa.current().asJson());
        agreement.putObject("filterConfig").put("ID", "");
        isa.update(agreement.toString());

        mvc.perform(post("/alerts").content(alert))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Outgoing alert is not valid")));
        verifyNoInteractions(transport);
    }

    @Test
    void validFilteredAlertReachesTransport() throws Exception {
        var agreement = reader.read(isa.current().asJson());
        agreement.putObject("filterConfig")
                .put("Description", "Filtered description")
                .put("ID", "550e8400-e29b-41d4-a716-446655440001");
        isa.update(agreement.toString());
        when(transport.forward(anyString(), anyString(), anyMap(), any()))
                .thenReturn(new AlertTransport.ForwardingResponse(202, "Accepted"));

        mvc.perform(post("/alerts").content(alert))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Accepted"));
        verify(transport).forward(eq("POST"), eq("/alerts"), anyMap(),
                argThat(forwarded -> "Filtered description".equals(forwarded.path("Description").asText())
                        && "550e8400-e29b-41d4-a716-446655440001".equals(forwarded.path("ID").asText())));
    }

    @Test
    void filtersAddressesInAnalyzerAndEachArrayEntry() throws Exception {
        MockMvc gateway = gatewayWith(new IDMEFValidator(
                "src/main/resources/schemas/idmefv2-schema0.9.json", mapper));
        var incoming = reader.read(alert);
        incoming.putObject("Analyzer").put("IP", "192.0.2.10").put("Name", "analyzer").put("UnLocation", "DE BER");
        incoming.putArray("Source").addObject().put("IP", "192.0.2.11").put("Name", "source");
        incoming.putArray("Sensor").addObject().put("IP", "192.0.2.12").put("Name", "sensor");
        var targets = incoming.putArray("Target");
        targets.addObject().put("IP", "192.0.2.13").put("Name", "first");
        targets.addObject().put("IP", "192.0.2.14").put("Name", "second");
        targets.addObject().put("Name", "no address");

        var agreement = reader.read(isa.current().asJson());
        var filters = agreement.putObject("filterConfig");
        filters.putObject("Analyzer").put("IP", "").put("UnLocation", "");
        filters.putObject("Source").put("IP", "");
        filters.putObject("Sensor").put("IP", "").put("Name", "");
        filters.put("Target", "192.129.213.213");
        isa.update(agreement.toString());
        when(transport.forward(anyString(), anyString(), anyMap(), any()))
                .thenReturn(new AlertTransport.ForwardingResponse(202, "Accepted"));

        gateway.perform(post("/alerts").content(incoming.toString())).andExpect(status().isAccepted());
        verify(transport).forward(eq("POST"), eq("/alerts"), anyMap(),
                argThat(forwarded -> forwarded.path("Analyzer").toString().equals("{\"Name\":\"analyzer\"}")
                        && forwarded.at("/Source/0").toString().equals("{\"Name\":\"source\"}")
                        && forwarded.at("/Sensor/0").toString().equals("{\"Name\":\"\"}")
                        && forwarded.path("Target").size() == 3
                        && forwarded.at("/Target/0/IP").asText().equals("192.129.213.213")
                        && forwarded.at("/Target/1/IP").asText().equals("192.129.213.213")
                        && forwarded.at("/Target/2/IP").isMissingNode()));
    }

    @Test
    void outgoingSchemaRejectsInvalidReplacementIpAndType() throws Exception {
        var incoming = reader.read(alert);
        incoming.putArray("Source").addObject().put("IP", "192.0.2.10");
        var agreement = reader.read(isa.current().asJson());
        agreement.putObject("filterConfig").putObject("Source").put("IP", "invalid");
        isa.update(agreement.toString());

        mvc.perform(post("/alerts").content(incoming.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Outgoing alert is not valid")))
                .andExpect(content().string(containsString("/Source/0/IP")));

        agreement.putObject("filterConfig").put("Description", 42);
        isa.update(agreement.toString());
        mvc.perform(post("/alerts").content(incoming.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Outgoing alert is not valid")))
                .andExpect(content().string(containsString("/Description")));
        verifyNoInteractions(transport);
    }

    @Test
    void compiledExternalSchemaEnforcesItsConditionalRequirementAfterFiltering(@TempDir Path directory)
            throws Exception {
        Path schema = directory.resolve("conditional.json");
        Files.writeString(schema, """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "if": {"required": ["Status"]},
                  "then": {"properties": {"Analyzer": {"$ref": "#/$defs/withIp"}}},
                  "$defs": {"withIp": {"required": ["IP"], "properties": {"IP": {"minLength": 1}}}}
                }
                """);
        MockMvc conditionalGateway = gatewayWith(new IDMEFValidator(schema.toString(), mapper));
        Files.delete(schema);

        var agreement = reader.read(isa.current().asJson());
        agreement.putObject("filterConfig").putObject("Analyzer").put("IP", "");
        isa.update(agreement.toString());

        conditionalGateway.perform(post("/alerts").content("""
                {"Status":"active","Analyzer":{"IP":"192.0.2.10"}}
                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Outgoing alert is not valid")))
                .andExpect(content().string(containsString("IP")));
        verifyNoInteractions(transport);
    }

    @Test
    void blankFilteringFollowsTheConfiguredSchemaInsteadOfFieldNames(@TempDir Path directory) throws Exception {
        Path schema = directory.resolve("custom.json");
        Files.writeString(schema, """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "Analyzer": {
                      "type": "object",
                      "required": ["IP"],
                      "properties": {"IP": {"type": "string"}, "Name": {"pattern": ".+"}}
                    }
                  }
                }
                """);
        MockMvc gateway = gatewayWith(new IDMEFValidator(schema.toString(), mapper));
        var agreement = reader.read(isa.current().asJson());
        agreement.putObject("filterConfig").putObject("Analyzer").put("IP", "").put("Name", "");
        isa.update(agreement.toString());
        when(transport.forward(anyString(), anyString(), anyMap(), any()))
                .thenReturn(new AlertTransport.ForwardingResponse(202, "Accepted"));

        gateway.perform(post("/alerts").content("""
                {"Analyzer":{"IP":"192.0.2.10","Name":"analyzer"}}
                """)).andExpect(status().isAccepted());
        verify(transport).forward(eq("POST"), eq("/alerts"), anyMap(),
                argThat(forwarded -> "".equals(forwarded.at("/Analyzer/IP").textValue())
                        && forwarded.at("/Analyzer/Name").isMissingNode()));
    }
}
