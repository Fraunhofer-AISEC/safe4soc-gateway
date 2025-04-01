package de.fraunhofer.aisec.s4sgateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    public static boolean isaComparison(JSONObject isaReceived, JSONObject isaStored) {
        if (isaStored.similar(isaReceived)) {
            return true;
        } else {
            return false;
        }
    }

    public static void applyFilter(JSONObject data, JSONObject filterConfig) {

        JSONObject contentObject;
        JSONArray contentArray;
        JSONObject filterLevel2;
        for (String keyLevel1 : filterConfig.keySet()) {
            logger.debug("Filtering the key {}", keyLevel1);
            switch (keyLevel1) {
                //1. level key value pairs
                case "Version":
                case "ID":
                case "Status":
                case "OrganisationName":
                case "CreateTime":
                case "StartTime":
                case "Priority":
                case "Confidence":
                case "Description":
                case "Cause":
                    data.put(keyLevel1, "");
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
                                case "IP":
                                case "Name":
                                case "Hostname":
                                case "Type":
                                case "Model":
                                case "GeoLocation":
                                case "UnLocation":
                                case "Location":
                                    contentObject.put(keyLevel2, "");
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
                                        contentArray.getJSONObject(i).put(keyLevel2, "");
                                        break;
                                    //2. level key array pairs
                                    case "Protocol":
                                        contentArray.getJSONObject(i).put(keyLevel2, new JSONArray());
                                        break;
                                }
                            }
                        }

                    }
                case "Target":
                    data.getJSONArray(keyLevel1).clear();
                    break;
                //TODO Ip Port Location
                default:
                    logger.debug("Found unknown filter instruction: {}", keyLevel1);
            }
        }
    }


    public static void applyEnrichment(JSONObject data, JSONObject enrichmentConfig) {
        JSONObject contentObject;
        JSONArray contentArray;
        JSONObject filterLevel2;

        for (String keyLevel1 : enrichmentConfig.keySet()) {
            logger.debug("Enriching the key {}", keyLevel1);
            switch (keyLevel1) {
                //1. level key value pairs
                case "CreateTime":
                case "StartTime":
                    if (data.getString(keyLevel1).isEmpty()) {
                        data.put(keyLevel1, TimestampFormatterIDMEFv2());
                    }
                    break;
                case "Version":
                case "ID":
                case "Status":
                case "OrganisationName":
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

    public static String TimestampFormatterIDMEFv2() {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        // Format it as the required ISO 8601 string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
                .withZone(java.time.ZoneOffset.UTC);
        return formatter.format(instant);
    }
}
