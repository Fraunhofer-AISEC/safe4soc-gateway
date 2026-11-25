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

package de.fraunhofer.aisec.s4sgateway.policy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

import static de.fraunhofer.aisec.s4sgateway.policy.IsaRules.*;

final class EnrichmentPolicy {
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentPolicy.class);

    private EnrichmentPolicy() {
    }

    static void applyEnrichment(ObjectNode data, ObjectNode config) {
        for (Iterator<String> fields = config.fieldNames(); fields.hasNext(); ) {
            String field = fields.next();
            logger.debug("Enriching the key {}", field);
            switch (field) {
                case "CreateTime", "StartTime" -> {
                    if (!data.has(field) || requireText(data.path(field)).isEmpty()) {
                        data.put(field, timestamp());
                    }
                }
                case "Version", "ID", "Status", "OrganisationName", "OrganisationId",
                     "Priority", "Confidence", "Description", "Cause", "Category", "Analyzer" -> {
                    ObjectNode instructions = requireObject(config.path(field));
                    ObjectNode target = requireObject(data.path(field));
                    if (instructions.isEmpty()) {
                        target.removeAll();
                    }
                }
                case "Sensor" -> requireArray(data.path(field)).removeAll();
                case "Source" -> {
                    ObjectNode instructions = requireObject(config.path(field));
                    ArrayNode targets = requireArray(data.path(field));
                    if (instructions.isEmpty()) {
                        targets.removeAll();
                    }
                }
                case "Target" -> {
                    // No enrichment instructions are currently supported for Target.
                }
                default -> logger.error("Found unknown enrichment instruction: {}", field);
            }
        }
    }

    private static String timestamp() {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        // Format it as the required ISO 8601 string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
                .withZone(java.time.ZoneOffset.UTC);
        return formatter.format(instant);
    }
}
