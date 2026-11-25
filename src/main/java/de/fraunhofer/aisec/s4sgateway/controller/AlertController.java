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

package de.fraunhofer.aisec.s4sgateway.controller;

import de.fraunhofer.aisec.s4sgateway.processing.AlertProcessingService;
import de.fraunhofer.aisec.s4sgateway.transport.AlertTransport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AlertController {
    private final AlertProcessingService processing;
    private final AlertTransport transport;

    public AlertController(AlertProcessingService processing, AlertTransport transport) {
        this.processing = processing;
        this.transport = transport;
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> forward(@RequestHeader Map<String, String> headers,
                                          @RequestBody String body,
                                          HttpServletRequest request) {
        var alert = processing.process(body);
        var response = transport.forward(request.getMethod(), request.getRequestURI(), headers, alert);
        return ResponseEntity.status(response.status()).body(response.body());
    }
}
