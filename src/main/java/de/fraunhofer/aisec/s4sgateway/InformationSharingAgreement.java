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
