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
    private final JsonNode schemaRoot;

    public JsonSchemaValidationService(
            @Value("${IDMEFv2SchemaPath}") String schemaPath,
            ObjectMapper objectMapper
    ) {
        this.schemaPath = schemaPath;

        SchemaRegistry registry =
                SchemaRegistry.withDefaultDialect(
                        SpecificationVersion.DRAFT_4
                );
        logger.debug("JSON Schema path is: {}", schemaPath);

        try {
            String schemaJson = loadSchema();

            this.schemaRoot = objectMapper.readTree(schemaJson);

            String schemaVersion = schemaRoot
                    .path("properties")
                    .path("Version")
                    .path("enum")
                    .path(0)
                    .asText();

            logger.info("IDMEFv2 schema found in version: {}", schemaVersion);

            this.schema = registry.getSchema(schemaRoot);

            logger.info("IDMEFv2 schema successfully loaded");

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not load or compile IDMEFv2 JSON schema",
                    e
            );
        }
    }

    public List<Error> validate(JsonNode json) {
        return schema.validate(json);
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

    public boolean isMandatory(String keyLevel1) {
        return containsRequired(resolveReference(schemaRoot), keyLevel1);
    }

    public boolean isMandatory(String keyLevel1, String keyLevel2) {

        JsonNode property = resolveReference(schemaRoot)
                .path("properties")
                .path(keyLevel1);

        property = resolveReference(property);

        // Handle arrays of JSON objects
        if (property.has("items")) {
            property = resolveReference(property.path("items"));
        }

        return containsRequired(property, keyLevel2);
    }

    private boolean containsRequired(JsonNode node, String key) {

        JsonNode required = node.path("required");

        if (required.isArray()) {
            for (JsonNode element : required) {
                if (key.equals(element.asText())) {
                    return true;
                }
            }
        }

        return false;
    }

    private JsonNode resolveReference(JsonNode node) {

        int depth = 0;

        while (node.has("$ref")) {

            String ref = node.path("$ref").asText();

            if (!ref.startsWith("#/")) {
                throw new IllegalArgumentException(
                        "Unsupported schema reference: " + ref
                );
            }

            if (++depth > 100) {
                throw new IllegalStateException(
                        "Circular schema reference detected: " + ref
                );
            }

            node = schemaRoot.at(ref.substring(1));

            if (node.isMissingNode()) {
                throw new IllegalArgumentException(
                        "Schema reference not found: " + ref
                );
            }
        }

        return node;
    }

    public boolean hasPattern(String keyLevel1) {

        JsonNode property = resolveReference(schemaRoot)
                .path("properties")
                .path(keyLevel1);

        return resolveReference(property).has("pattern");
    }

    public boolean hasPattern(String keyLevel1, String keyLevel2) {

        JsonNode property = resolveReference(schemaRoot)
                .path("properties")
                .path(keyLevel1);

        property = resolveReference(property);

        // Handle arrays of JSON objects
        if (property.has("items")) {
            property = resolveReference(property.path("items"));
        }

        JsonNode nestedProperty = property
                .path("properties")
                .path(keyLevel2);

        return resolveReference(nestedProperty).has("pattern");
    }



}
