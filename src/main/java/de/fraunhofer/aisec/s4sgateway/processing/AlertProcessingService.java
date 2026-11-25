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

package de.fraunhofer.aisec.s4sgateway.processing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.fraunhofer.aisec.s4sgateway.json.JsonObjectReader;
import de.fraunhofer.aisec.s4sgateway.policy.IsaService;
import de.fraunhofer.aisec.s4sgateway.validation.IDMEFValidator;
import org.springframework.stereotype.Service;

import static de.fraunhofer.aisec.s4sgateway.processing.AlertRejectedException.Reason.*;

@Service
public class AlertProcessingService {
    private final JsonObjectReader reader;
    private final IDMEFValidator validator;
    private final IsaService isa;

    public AlertProcessingService(JsonObjectReader reader, IDMEFValidator validator, IsaService isa) {
        this.reader = reader;
        this.validator = validator;
        this.isa = isa;
    }

    public ObjectNode process(String body) {
        ObjectNode alert;
        try {
            alert = reader.read(body);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new AlertRejectedException(MALFORMED_JSON, "Invalid JSON: " + e.getMessage());
        }

        validate(alert, "Incoming");
        isa.current().apply(alert, validator).ifPresent(rejection -> {
            throw new AlertRejectedException(ISA_DENIED, rejection);
        });
        validate(alert, "Outgoing");
        return alert;
    }

    private void validate(ObjectNode alert, String direction) {
        var errors = validator.validate(alert);
        if (!errors.isEmpty()) {
            throw new AlertRejectedException(INVALID_SCHEMA,
                    direction + " alert is not valid against JSON Schema\n" + errors);
        }
    }
}
