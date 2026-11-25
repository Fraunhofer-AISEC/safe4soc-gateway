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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

public class Utils {
    static final Logger logger = LoggerFactory.getLogger(Controller.class);

    static ObjectNode requireObject(JsonNode node) {
        if (node instanceof ObjectNode object) {
            return object;
        }
        throw new IllegalArgumentException("Expected a JSON object");
    }

    private static ArrayNode requireArray(JsonNode node) {
        if (node instanceof ArrayNode array) {
            return array;
        }
        throw new IllegalArgumentException("Expected a JSON array");
    }

    private static String requireText(JsonNode node) {
        if (node.isTextual()) {
            return node.textValue();
        }
        throw new IllegalArgumentException("Expected a JSON string");
    }

    public static boolean isaComparison(ObjectNode isaReceived, ObjectNode isaStored) {
        return isaStored != null && isaReceived.equals((left, right) -> {
            if (left.isNumber() && right.isNumber()) {
                return left.decimalValue().compareTo(right.decimalValue());
            }
            return left.equals(right) ? 0 : 1;
        }, isaStored);
    }

    public static String applyFilter(ObjectNode data, ObjectNode filterConfig, JsonSchemaValidationService jsonSchemaValidator) {
        ObjectNode contentObject;
        ArrayNode contentArray;
        ObjectNode filterLevel2;
        for (Iterator<String> fields = filterConfig.fieldNames(); fields.hasNext(); ) {
            String keyLevel1 = fields.next();
            try {
                logger.debug("Filtering the key {}", keyLevel1);
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
                            putFromFilterOrEmpty(data, filterConfig, keyLevel1);
                            ifContentMandatoryDeleteWholeElement(data,keyLevel1,"",jsonSchemaValidator);
                        break;
                    //1. level key array pair
                    case "Category":
                        requireArray(data.path(keyLevel1)).removeAll();
                        break;
                    //1. level nested JSON objects
                    case "Analyzer":
                        filterLevel2 = requireObject(filterConfig.path(keyLevel1));
                        contentObject = requireObject(data.path(keyLevel1));
                        if (filterLevel2.isEmpty()) {
                            contentObject.removeAll();
                        } else {
                            for (Iterator<String> nestedFields = filterLevel2.fieldNames(); nestedFields.hasNext(); ) {
                                String keyLevel2 = nestedFields.next();
                                switch (keyLevel2) {
                                    //2. level key value pairs
                                    case "Name":
                                    case "Hostname":
                                    case "Type":
                                    case "Model":
                                    case "GeoLocation":
                                    case "UnLocation":
                                    case "Location":
                                    case "IP":
                                        if (!requireText(filterLevel2.path(keyLevel2)).isEmpty()) {
                                            contentObject.put(keyLevel2,
                                                    requireText(filterLevel2.path(keyLevel2)));
                                        } else {
                                            contentObject.put(keyLevel2, "");
                                            ifContentMandatoryDeleteWholeElement(contentObject,
                                                    keyLevel1,keyLevel2,jsonSchemaValidator);
                                        }
                                        break;
                                    //2. level key array pairs
                                    case "Category":
                                    case "Data":
                                    case "Method":
                                        requireArray(contentObject.path(keyLevel2)).removeAll();
                                        break;
                                }
                            }
                        }
                        break;
                    case "Sensor":
                        requireArray(data.path(keyLevel1)).removeAll();
                        break;
                    //TODO Ip Name
                    //TODO Capture Zone
                    case "Source":
                        filterLevel2 = requireObject(filterConfig.path(keyLevel1));
                        contentArray = requireArray(data.path(keyLevel1));
                        if (filterLevel2.isEmpty()) {
                            contentArray.removeAll();
                        } else {
                            for (int i = 0; i < contentArray.size(); i++) {
                                for (Iterator<String> nestedFields = filterLevel2.fieldNames(); nestedFields.hasNext(); ) {
                                    String keyLevel2 = nestedFields.next();
                                    switch (keyLevel2) {
                                        //2. level key value pairs
                                        case "IP":
                                            if (!requireText(filterLevel2.path(keyLevel2)).isEmpty()) {
                                                requireObject(contentArray.path(i)).put(keyLevel2,
                                                        requireText(filterLevel2.path(keyLevel2)));
                                            } else {
                                                requireObject(contentArray.path(i)).put(keyLevel2, "");
                                                ifContentMandatoryDeleteWholeElement(requireObject(contentArray.path(i)),
                                                        keyLevel1,keyLevel2,jsonSchemaValidator);
                                            }
                                            break;
                                        //2. level key array pairs
                                        case "Protocol":
                                            requireObject(contentArray.path(i)).putArray(keyLevel2);
                                            break;
                                    }
                                }
                            }
                        }
                        break;
                    case "Target":
                        requireArray(data.path(keyLevel1)).removeAll();
                        break;
                    //TODO Ip Port Location
                    default:
                        logger.debug("Found unknown filter instruction: {}", keyLevel1);
                }
            } catch (IllegalArgumentException e) {
                return keyLevel1;
            }
        }
        return "";
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

    static void applyEnrichment(ObjectNode data, ObjectNode enrichmentConfig) {
        ObjectNode contentObject;
        ArrayNode contentArray;
        ObjectNode filterLevel2;

        for (Iterator<String> fields = enrichmentConfig.fieldNames(); fields.hasNext(); ) {
            String keyLevel1 = fields.next();
            logger.debug("Enriching the key {}", keyLevel1);
            switch (keyLevel1) {
                //1. level key value pairs
                case "CreateTime":
                case "StartTime":
                    if (!data.has(keyLevel1)) {
                        data.put(keyLevel1, TimestampFormatterIDMEFv2());
                        break;
                    }
                    if (requireText(data.path(keyLevel1)).isEmpty()) {
                        data.put(keyLevel1, TimestampFormatterIDMEFv2());
                    }
                    break;
                case "Version":
                case "ID":
                case "Status":
                case "OrganisationName":
                case "OrganisationId":
                case "Priority":
                case "Confidence":
                case "Description":
                case "Cause":
                    //1. level key array pair
                case "Category":
                    //1. level nested JSON objects
                case "Analyzer":
                    filterLevel2 = requireObject(enrichmentConfig.path(keyLevel1));
                    contentObject = requireObject(data.path(keyLevel1));
                    if (filterLevel2.isEmpty()) {
                        contentObject.removeAll();
                    } else {
                        for (Iterator<String> nestedFields = filterLevel2.fieldNames(); nestedFields.hasNext(); ) {
                            String keyLevel2 = nestedFields.next();
                            switch (keyLevel2) {
                                //2. level key value pairs
                                case "IP":
                                case "Name":
                                case "Hostname":
                                case "Type":
                                case "Model":
                                case "GeoLocation":
                                case "UnLocation":
                                case "Location":
                                    //contentObject.put(keyLevel2,"");
                                    break;
                                //2. level key array pairs
                                case "Category":
                                case "Data":
                                case "Method":
                                    //requireArray(contentObject.path(keyLevel2)).removeAll();
                                    break;
                            }
                        }
                    }
                    break;
                case "Sensor":
                    requireArray(data.path(keyLevel1)).removeAll();
                    break;
                //TODO Ip Name
                //TODO Capture Zone
                case "Source":
                    filterLevel2 = requireObject(enrichmentConfig.path(keyLevel1));
                    contentArray = requireArray(data.path(keyLevel1));
                    if (filterLevel2.isEmpty()) {
                        contentArray.removeAll();
                    } else {
                        for (int i = 0; i < contentArray.size(); i++) {
                            for (Iterator<String> nestedFields = filterLevel2.fieldNames(); nestedFields.hasNext(); ) {
                                String keyLevel2 = nestedFields.next();
                                switch (keyLevel2) {
                                    //2. level key value pairs
                                    case "IP":
                                        //requireObject(contentArray.path(i)).put(keyLevel2, "");
                                        break;
                                    //2. level key array pairs
                                    case "Protocol":
                                        //requireObject(contentArray.path(i)).putArray(keyLevel2);
                                        break;
                                }
                            }
                        }

                    }
                case "Target":
                    //requireArray(data.path(keyLevel1)).removeAll();
                    break;
                //TODO Ip Port Location
                default:
                    logger.error("Found unknown filter instruction: {}", keyLevel1);
            }
        }
    }


    static String TimestampFormatterIDMEFv2() {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        // Format it as the required ISO 8601 string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
                .withZone(java.time.ZoneOffset.UTC);
        return formatter.format(instant);
    }


    static void putFromFilterOrEmpty(ObjectNode target, ObjectNode filter, String key) {
        if (filter != null && filter.hasNonNull(key)) {
            target.set(key, filter.get(key).deepCopy());
        }
    }

    static void ifContentMandatoryDeleteWholeElement(ObjectNode target, String key1, String key2,
                                                     JsonSchemaValidationService validator) {
        if (key2.isEmpty()){
            if (validator.isMandatory(key1) || validator.hasPattern(key1)){
                target.remove(key1);
            }
        } else {
            if (validator.isMandatory(key1,key2) || validator.hasPattern(key1,key2)){
                target.remove(key2);
            }
        }
    }

    static boolean checkFromDropConfig(
            ObjectNode target, ObjectNode dropConfig, String key) {

        if (dropConfig != null && dropConfig.hasNonNull(key)
                && target != null && target.hasNonNull(key)) {

            JsonNode value = target.get(key);
            return value.isValueNode() && value.equals(dropConfig.get(key));
        }

        return false;
    }

    public static String checkMandatoryFields(ObjectNode target, ObjectNode mandatoryFields) {
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
