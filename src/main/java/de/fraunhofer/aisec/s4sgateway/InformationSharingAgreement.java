package de.fraunhofer.aisec.s4sgateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.InputStream;

public class InformationSharingAgreement {

    private static final Logger logger = LoggerFactory.getLogger(InformationSharingAgreement.class);

    private boolean isaSet=false;
    private JsonNode isaJsonNodeStored;
    private String isaJsonNodeStoredAsText;
    private final Resource jsonFile;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public InformationSharingAgreement(Resource jsonFile){
        this.jsonFile=jsonFile;
    }

    public void setIsaAsJsonNodeFromFile() throws Exception {
        logger.debug("Accessing isa file to read stored JSON at: {}", jsonFile);
        try (InputStream inputStream = jsonFile.getInputStream()) {
            this.isaJsonNodeStored = objectMapper.readTree(inputStream);
        }
        this.isaJsonNodeStoredAsText = objectMapper.writerWithDefaultPrettyPrinter().
                writeValueAsString(isaJsonNodeStoredAsText);
    }

    public JsonNode getIsaAsJsonNodeStored() throws Exception {
        if(!this.isaSet)
        {
            setIsaAsJsonNodeFromFile();
            this.isaSet=true;
        }
        return this.isaJsonNodeStored;
    }

    public String getIsaValueAsText(String key) throws Exception {
        return getIsaAsJsonNodeStored().get(key).asText();
    }

    public String getIsaAsJsonTextAll() throws Exception {
        return isaJsonNodeStoredAsText;
    }
}
