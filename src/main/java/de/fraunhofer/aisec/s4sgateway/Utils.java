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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Utils {
    static final Logger logger = LoggerFactory.getLogger(Controller.class);

    public static boolean isaComparison(JSONObject isaReceived, JSONObject isaStored) {
        return isaStored.similar(isaReceived);
    }

    public static String applyFilter(JSONObject data, JSONObject filterConfig, JsonSchemaValidationService jsonSchemaValidator) {
        JSONObject contentObject;
        JSONArray contentArray;
        JSONObject filterLevel2;
        for (String keyLevel1 : filterConfig.keySet()) {
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
                        data.getJSONArray(keyLevel1).clear();
                        break;
                    //1. level nested JSON objects
                    case "Analyzer":
                        filterLevel2 = filterConfig.getJSONObject(keyLevel1);
                        contentObject = data.getJSONObject(keyLevel1);
                        if (filterLevel2.isEmpty()) {
                            contentObject.clear();
                        } else {
                            for (String keyLevel2 : filterLevel2.keySet()) {
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
                                        if (!filterLevel2.getString(keyLevel2).isEmpty()) {
                                            contentObject.put(keyLevel2,
                                                    filterLevel2.getString(keyLevel2));
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
                                        contentObject.getJSONArray(keyLevel2).clear();
                                        break;
                                }
                            }
                        }
                        break;
                    case "Sensor":
                        data.getJSONArray(keyLevel1).clear();
                        break;
                    //TODO Ip Name
                    //TODO Capture Zone
                    case "Source":
                        filterLevel2 = filterConfig.getJSONObject(keyLevel1);
                        contentArray = data.getJSONArray(keyLevel1);
                        if (filterLevel2.isEmpty()) {
                            contentArray.clear();
                        } else {
                            for (int i = 0; i < contentArray.length(); i++) {
                                for (String keyLevel2 : filterLevel2.keySet()) {
                                    switch (keyLevel2) {
                                        //2. level key value pairs
                                        case "IP":
                                            if (!filterLevel2.getString(keyLevel2).isEmpty()) {
                                                contentArray.getJSONObject(i).put(keyLevel2,
                                                        filterLevel2.getString(keyLevel2));
                                            } else {
                                                contentArray.getJSONObject(i).put(keyLevel2, "");
                                                ifContentMandatoryDeleteWholeElement(contentArray.getJSONObject(i),
                                                        keyLevel1,keyLevel2,jsonSchemaValidator);
                                            }
                                            break;
                                        //2. level key array pairs
                                        case "Protocol":
                                            contentArray.getJSONObject(i).put(keyLevel2, new JSONArray());
                                            break;
                                    }
                                }
                            }
                        }
                        break;
                    case "Target":
                        data.getJSONArray(keyLevel1).clear();
                        break;
                    //TODO Ip Port Location
                    default:
                        logger.debug("Found unknown filter instruction: {}", keyLevel1);
                }
            } catch (JSONException e) {
                return keyLevel1;
            }
        }
        return "";
    }

    static String applyDropCheck(JSONObject data, JSONObject dropConfig) {
        for (String keyLevel1 : dropConfig.keySet()) {
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

    static void applyEnrichment(JSONObject data, JSONObject enrichmentConfig) {
        JSONObject contentObject;
        JSONArray contentArray;
        JSONObject filterLevel2;

        for (String keyLevel1 : enrichmentConfig.keySet()) {
            logger.debug("Enriching the key {}", keyLevel1);
            switch (keyLevel1) {
                //1. level key value pairs
                case "CreateTime":
                case "StartTime":
                    if (!data.has(keyLevel1)) {
                        data.put(keyLevel1, TimestampFormatterIDMEFv2());
                        break;
                    }
                    if (data.getString(keyLevel1).isEmpty()) {
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
                    filterLevel2 = enrichmentConfig.getJSONObject(keyLevel1);
                    contentObject = data.getJSONObject(keyLevel1);
                    if (filterLevel2.isEmpty()) {
                        contentObject.clear();
                    } else {
                        for (String keyLevel2 : filterLevel2.keySet()) {
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
                                    //contentObject.getJSONArray(keyLevel2).clear();
                                    break;
                            }
                        }
                    }
                    break;
                case "Sensor":
                    data.getJSONArray(keyLevel1).clear();
                    break;
                //TODO Ip Name
                //TODO Capture Zone
                case "Source":
                    filterLevel2 = enrichmentConfig.getJSONObject(keyLevel1);
                    contentArray = data.getJSONArray(keyLevel1);
                    if (filterLevel2.isEmpty()) {
                        contentArray.clear();
                    } else {
                        for (int i = 0; i < contentArray.length(); i++) {
                            for (String keyLevel2 : filterLevel2.keySet()) {
                                switch (keyLevel2) {
                                    //2. level key value pairs
                                    case "IP":
                                        //contentArray.getJSONObject(i).put(keyLevel2, "");
                                        break;
                                    //2. level key array pairs
                                    case "Protocol":
                                        //contentArray.getJSONObject(i).put(keyLevel2, new JSONArray());
                                        break;
                                }
                            }
                        }

                    }
                case "Target":
                    //data.getJSONArray(keyLevel1).clear();
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


    static void putFromFilterOrEmpty(JSONObject target, JSONObject filter, String key) {
        if (filter != null && filter.has(key) && !filter.isNull(key)) {
            target.put(key, filter.get(key));
        }
    }

    static void ifContentMandatoryDeleteWholeElement(JSONObject target, String key1, String key2,
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
            JSONObject target, JSONObject dropConfig, String key) {

        if (dropConfig != null && dropConfig.has(key) && !dropConfig.isNull(key)
                && target != null && target.has(key) && !target.isNull(key)) {

            return target.get(key).equals(dropConfig.get(key));
        }

        return false;
    }

    public static String checkMandatoryFields(JSONObject target, JSONObject mandatoryFields) {
        if (target == null || mandatoryFields == null) {
            return "";
        }
        for (String key : mandatoryFields.keySet()) {
            logger.debug("Checking mandatory field config at key: {}", key);
            if (!target.has(key) || target.isNull(key)) {
                return key;
            }
            Object mandatoryValue = mandatoryFields.get(key);
            Object targetValue = target.get(key);

            // Recursively check nested JSON objects
            if (mandatoryValue instanceof JSONObject mandatoryObject) {
                if (!(targetValue instanceof JSONObject targetObject)) {
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
