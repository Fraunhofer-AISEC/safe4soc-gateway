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

package de.fraunhofer.aisec.s4sgateway.exception;

import de.fraunhofer.aisec.s4sgateway.controller.AlertController;
import de.fraunhofer.aisec.s4sgateway.processing.AlertRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AlertController.class)
public class AlertExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AlertExceptionHandler.class);

    @ExceptionHandler(AlertRejectedException.class)
    public ResponseEntity<String> alertRejected(AlertRejectedException rejection) {
        logger.warn("Alert rejected. Reason={}", rejection.reason());
        HttpStatus status = switch (rejection.reason()) {
            case MALFORMED_JSON -> HttpStatus.BAD_REQUEST;
            case INVALID_SCHEMA, ISA_DENIED -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(rejection.getMessage());
    }
}
