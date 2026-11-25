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

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static de.fraunhofer.aisec.s4sgateway.policy.IsaRules.*;

final class FilterPolicy {
    private static final Logger logger = LoggerFactory.getLogger(FilterPolicy.class);

    private FilterPolicy() {
    }

    static String applyFilter(ObjectNode data, ObjectNode filterConfig,
                              Function<ObjectNode, ? extends Collection<?>> validate) {
        List<JsonPointer> blankFields = new ArrayList<>();
        var fields = filterConfig.fields();
        while (fields.hasNext()) {
            var instruction = fields.next();
            String field = instruction.getKey();
            JsonNode replacement = instruction.getValue();
            JsonPointer path = JsonPointer.empty().appendProperty(field);
            try {
                logger.debug("Filtering the key {}", field);
                switch (field) {
                    case "Version", "ID", "Status", "OrganisationName", "OrganisationId",
                         "CreateTime", "StartTime", "Priority", "Confidence", "Description", "Cause" -> {
                        if (isBlank(replacement)) {
                            blankFields.add(path);
                        } else if (!replacement.isNull()) {
                            data.set(field, replacement.deepCopy());
                        }
                    }
                    case "Category" -> requireArray(data.path(field)).removeAll();
                    case "Analyzer" -> {
                        if (data.has(field)) {
                            applyObjectFilter(requireObject(data.path(field)), requireObject(replacement), path, blankFields);
                        }
                    }
                    case "Sensor", "Source", "Target" -> {
                        if (data.has(field)) {
                            applyArrayFilter(requireArray(data.path(field)), replacement, path, blankFields);
                        }
                    }
                    default -> logger.debug("Found unknown filter instruction: {}", field);
                }
            } catch (IllegalArgumentException e) {
                return field;
            }
        }
        applyBlanks(data, blankFields, validate);
        return "";
    }

    private static void applyArrayFilter(ArrayNode entries, JsonNode replacement,
                                         JsonPointer path, List<JsonPointer> blankFields) {
        if (replacement.isTextual()) {
            if (replacement.textValue().isEmpty()) {
                entries.removeAll();
            } else {
                for (JsonNode entry : entries) {
                    ObjectNode target = requireObject(entry);
                    if (target.has("IP")) {
                        target.set("IP", replacement);
                    }
                }
            }
            return;
        }

        ObjectNode fields = requireObject(replacement);
        if (fields.isEmpty()) {
            entries.removeAll();
        } else {
            for (int index = 0; index < entries.size(); index++) {
                applyObjectFilter(requireObject(entries.get(index)), fields, path.appendIndex(index), blankFields);
            }
        }
    }

    private static void applyObjectFilter(ObjectNode target, ObjectNode fields,
                                          JsonPointer path, List<JsonPointer> blankFields) {
        if (fields.isEmpty()) {
            target.removeAll();
            return;
        }
        var instructions = fields.fields();
        while (instructions.hasNext()) {
            var instruction = instructions.next();
            String field = instruction.getKey();
            JsonNode replacement = instruction.getValue();
            JsonNode current = target.get(field);
            if (current == null || replacement.isNull()) {
                continue;
            }
            if (isBlank(replacement)) {
                if (current instanceof ArrayNode array) {
                    array.removeAll();
                } else {
                    blankFields.add(path.appendProperty(field));
                }
            } else {
                target.set(field, replacement.deepCopy());
            }
        }
    }

    private static boolean isBlank(JsonNode replacement) {
        return replacement.isTextual() && replacement.textValue().isEmpty();
    }

    private static void applyBlanks(ObjectNode data, List<JsonPointer> fields,
                                    Function<ObjectNode, ? extends Collection<?>> validate) {
        if (fields.isEmpty()) {
            return;
        }

        // Probe a detached alert so a rejected replacement never lands in the outgoing alert.
        ObjectNode candidate = data.deepCopy();
        var currentErrors = new HashSet<>(validate.apply(candidate));
        for (JsonPointer fieldPath : fields) {
            String field = fieldPath.last().getMatchingProperty();
            ObjectNode trial = requireObject(candidate.at(fieldPath.head()));
            ObjectNode target = requireObject(data.at(fieldPath.head()));
            trial.put(field, "");
            var blankErrors = new HashSet<>(validate.apply(candidate));
            if (currentErrors.containsAll(blankErrors)) {
                target.put(field, "");
                currentErrors = blankErrors;
            } else {
                trial.remove(field);
                target.remove(field);
                currentErrors = new HashSet<>(validate.apply(candidate));
            }
        }
    }
}
