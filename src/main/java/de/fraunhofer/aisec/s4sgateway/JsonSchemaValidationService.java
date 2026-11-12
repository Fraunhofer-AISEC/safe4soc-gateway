
package de.fraunhofer.aisec.s4sgateway;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Error;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class JsonSchemaValidationService {

    private final Schema schema;

    public JsonSchemaValidationService() {

        SchemaRegistry registry =
                SchemaRegistry.withDefaultDialect(
                        SpecificationVersion.DRAFT_4
                );

        try (InputStream input =
                     getClass().getResourceAsStream(
                             "/schemas/idmefv2-schema.json")) {

            if (input == null) {
                throw new IllegalStateException(
                        "IDMEFv2 JSON schema not found"
                );
            }
            this.schema = registry.getSchema(
                    new String(input.readAllBytes(), StandardCharsets.UTF_8),
                    InputFormat.JSON
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not load IDMEFv2 JSON schema", e
            );
        }
    }

    public List<Error> validate(String json) {
        return schema.validate(json, InputFormat.JSON);
    }
}
