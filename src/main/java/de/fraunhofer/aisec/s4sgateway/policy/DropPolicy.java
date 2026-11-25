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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

final class DropPolicy {
    private static final Logger logger = LoggerFactory.getLogger(DropPolicy.class);

    private DropPolicy() {
    }

    static String applyDropCheck(ObjectNode data, ObjectNode dropConfig) {
        for (Iterator<String> fields = dropConfig.fieldNames(); fields.hasNext(); ) {
            String keyLevel1 = fields.next();
            logger.debug("Checking the key {} for drop instructions.", keyLevel1);
            switch (keyLevel1) {
                //1. level key value pairs
                case "Version":
                case "ID":
                case "Status":
                case "OrganisationName":
                case "OrganisationId":
                case "CreateTime":
                case "StartTime":
                case "Priority":
                case "Confidence":
                case "Description":
                case "Cause":
                case "Category":
                    if (checkFromDropConfig(data, dropConfig, keyLevel1)) {
                        logger.info("Dropped alert due to triggered drop configuration at key: {}", keyLevel1);
                        return keyLevel1;
                    } else {
                        break;
                    }
                default:
                    logger.debug("No drop was applied.");
            }
        }
        return "";
    }

    private static boolean checkFromDropConfig(
            ObjectNode target, ObjectNode dropConfig, String key) {

        if (dropConfig != null && dropConfig.hasNonNull(key)
                && target != null && target.hasNonNull(key)) {

            JsonNode value = target.get(key);
            return value.isValueNode() && value.equals(dropConfig.get(key));
        }

        return false;
    }
}
