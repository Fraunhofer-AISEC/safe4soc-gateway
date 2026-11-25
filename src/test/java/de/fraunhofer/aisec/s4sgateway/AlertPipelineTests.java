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
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
        IDMEFValidator validator = new IDMEFValidator("", mapper);
        AlertProcessingService processing = new AlertProcessingService(reader, validator, isa);
        mvc = MockMvcBuilders.standaloneSetup(
                        new AlertController(processing, transport),
                        new AdminController(isa, GatewayRole.SENDER, true, true))
                .setControllerAdvice(new AlertExceptionHandler())
                .build();
        alert = new ClassPathResource("alerts/test_idmefv2_minimal.json")
                .getContentAsString(StandardCharsets.UTF_8);
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
        agreement.putObject("filterConfig").put("Description", "Filtered description");
        isa.update(agreement.toString());
        when(transport.forward(anyString(), anyString(), anyMap(), any()))
                .thenReturn(new AlertTransport.ForwardingResponse(202, "Accepted"));

        mvc.perform(post("/alerts").content(alert))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Accepted"));
        verify(transport).forward(eq("POST"), eq("/alerts"), anyMap(),
                argThat(forwarded -> "Filtered description".equals(forwarded.path("Description").asText())));
    }
}
