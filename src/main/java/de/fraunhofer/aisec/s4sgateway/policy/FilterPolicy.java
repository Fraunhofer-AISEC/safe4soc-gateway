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

import java.util.Iterator;

import static de.fraunhofer.aisec.s4sgateway.policy.IsaRules.*;

final class FilterPolicy {
    private static final Logger logger = LoggerFactory.getLogger(FilterPolicy.class);

    private FilterPolicy() {
    }

    static String applyFilter(ObjectNode data, ObjectNode filterConfig, AlertFieldConstraints jsonSchemaValidator) {
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

    private static void putFromFilterOrEmpty(ObjectNode target, ObjectNode filter, String key) {
        if (filter != null && filter.hasNonNull(key)) {
            target.set(key, filter.get(key).deepCopy());
        }
    }

    private static void ifContentMandatoryDeleteWholeElement(ObjectNode target, String key1, String key2,
                                                     AlertFieldConstraints validator) {
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
}
