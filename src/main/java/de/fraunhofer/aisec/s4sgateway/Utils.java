package de.fraunhofer.aisec.s4sgateway;

import org.json.JSONObject;

public class Utils{
    public static boolean  isaComparison(JSONObject isaReceived, JSONObject isaStored){
        if(isaStored.similar(isaReceived)){
            return true;
        } else {
            return false;
        }
    }

}
