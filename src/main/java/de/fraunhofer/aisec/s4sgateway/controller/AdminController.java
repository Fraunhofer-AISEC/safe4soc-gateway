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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fraunhofer.aisec.s4sgateway.config.GatewayRole;
import de.fraunhofer.aisec.s4sgateway.policy.IsaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final IsaService isa;
    private final GatewayRole role;
    private final boolean reloadEnabled;
    private final boolean setIsaEnabled;

    public AdminController(IsaService isa, @Value("${gatewayRole}") GatewayRole role,
                           @Value("${endpoints.reloadconfig.enabled}") boolean reloadEnabled,
                           @Value("${endpoints.setisa.enabled}") boolean setIsaEnabled) {
        this.isa = isa;
        this.role = role;
        this.reloadEnabled = reloadEnabled;
        this.setIsaEnabled = setIsaEnabled;
        if (reloadEnabled) {
            logger.warn("Endpoint reloadconfig can be reached - only use in debug mode!");
        }
        if (setIsaEnabled) {
            logger.warn("Endpoint setisa can be reached - only use in debug mode!");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Gateway " + role + " online.\nISA: " + isa.current().asJson());
    }

    @GetMapping("/reloadconfig")
    public ResponseEntity<String> reloadConfig() {
        if (!reloadEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Configuration reload endpoint deactivated.");
        }
        try {
            isa.reload();
        } catch (IllegalArgumentException | IOException e) {
            logger.warn("ISA reload failed", e);
            return ResponseEntity.internalServerError().body("Could not reload ISA configuration.");
        }
        return ResponseEntity.ok("Configuration reload attempt was processed, please see internal logs for results.");
    }

    @PostMapping("/setisa")
    public ResponseEntity<String> setIsa(@RequestBody String json) {
        if (!setIsaEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("SetISA configuration endpoint deactivated.");
        }
        try {
            isa.update(json);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid ISA JSON: " + e.getMessage());
        }
        return ResponseEntity.ok("Processed ISA update.");
    }

    @PostMapping("/isacheck")
    public ResponseEntity<String> isaCheck(@RequestBody String json) {
        try {
            if (isa.matches(json)) {
                return ResponseEntity.ok("ISA compatible.");
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body("ISA incompatible.");
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid ISA JSON: " + e.getMessage());
        }
    }
}
