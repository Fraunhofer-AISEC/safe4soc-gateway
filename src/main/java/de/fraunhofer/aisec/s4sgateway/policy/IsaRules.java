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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

final class IsaRules {
    private IsaRules() {
    }

    static final Logger logger = LoggerFactory.getLogger(IsaRules.class);

    static ObjectNode requireObject(JsonNode node) {
        if (node instanceof ObjectNode object) {
            return object;
        }
        throw new IllegalArgumentException("Expected a JSON object");
    }

    static ArrayNode requireArray(JsonNode node) {
        if (node instanceof ArrayNode array) {
            return array;
        }
        throw new IllegalArgumentException("Expected a JSON array");
    }

    static String requireText(JsonNode node) {
        if (node.isTextual()) {
            return node.textValue();
        }
        throw new IllegalArgumentException("Expected a JSON string");
    }

    static boolean isaComparison(ObjectNode isaReceived, ObjectNode isaStored) {
        return isaStored != null && isaReceived.equals((left, right) -> {
            if (left.isNumber() && right.isNumber()) {
                return left.decimalValue().compareTo(right.decimalValue());
            }
            return left.equals(right) ? 0 : 1;
        }, isaStored);
    }

    static String checkMandatoryFields(ObjectNode target, ObjectNode mandatoryFields) {
        if (target == null || mandatoryFields == null) {
            return "";
        }
        for (Iterator<String> fields = mandatoryFields.fieldNames(); fields.hasNext(); ) {
            String key = fields.next();
            logger.debug("Checking mandatory field config at key: {}", key);
            if (!target.hasNonNull(key)) {
                return key;
            }
            JsonNode mandatoryValue = mandatoryFields.get(key);
            JsonNode targetValue = target.get(key);

            // Recursively check nested JSON objects
            if (mandatoryValue instanceof ObjectNode mandatoryObject) {
                if (!(targetValue instanceof ObjectNode targetObject)) {
                    return key;
                }
                if (!checkMandatoryFields(targetObject, mandatoryObject).isEmpty()) {
                    return key;
                }
            }
        }
        return "";
    }
}
