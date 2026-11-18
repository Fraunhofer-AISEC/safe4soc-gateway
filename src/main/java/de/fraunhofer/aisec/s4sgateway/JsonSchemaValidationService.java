/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fraunhofer.aisec.s4sgateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class JsonSchemaValidationService {
    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaValidationService.class);

    private final Schema schema;
    private final String schemaPath;

    public JsonSchemaValidationService(
            @Value("${IDMEFv2SchemaPath}") String schemaPath
    ){
        this.schemaPath = schemaPath;

        SchemaRegistry registry =
                SchemaRegistry.withDefaultDialect(
                        SpecificationVersion.DRAFT_4
                );
        logger.debug("JSON Schema path is: {}", schemaPath);

        try {
            String schemaJson = loadSchema();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(schemaJson);

            String schemaVersion = root
                    .path("properties")
                    .path("Version")
                    .path("enum")
                    .path(0)
                    .asText();

            logger.info("IDMEFv2 schema found in version: {}", schemaVersion);

            this.schema = registry.getSchema(
                    schemaJson,
                    InputFormat.JSON
            );

            logger.info("IDMEFv2 schema successfully loaded");

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not load or compile IDMEFv2 JSON schema",
                    e
            );
        }
    }

    public List<Error> validate(String json) {
        return schema.validate(json, InputFormat.JSON);
    }

    private String loadSchema() throws IOException {

        // 1. Try loading an external filesystem schema
        if (schemaPath != null && !schemaPath.isBlank()) {

            Path path = Paths.get(schemaPath);

            if (Files.isRegularFile(path) && Files.isReadable(path)) {
                logger.info("Loading external JSON schema: {}", path.toAbsolutePath());

                return Files.readString(path, StandardCharsets.UTF_8);
            }

            logger.warn(
                    "External JSON schema not found or not readable: {}. "
                            + "Trying embedded schema inside binary instead.",
                    path.toAbsolutePath()
            );
        }

        // 2. Fall back to the embedded classpath resource
        String resourcePath = "/schemas/idmefv2-schema.json";

        logger.info("Loading embedded JSON schema from JAR or Graal VM compiled binary: {}", resourcePath);

        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {

            if (input == null) {
                throw new FileNotFoundException(
                        "Embedded IDMEFv2 JSON schema not found: " + resourcePath
                );
            }

            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
