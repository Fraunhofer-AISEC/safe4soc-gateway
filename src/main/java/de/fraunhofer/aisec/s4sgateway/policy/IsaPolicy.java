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

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/** Applies an information sharing agreement using Jackson. */
public final class IsaPolicy {
    private final ObjectNode agreement;
    private final ObjectNode drop;
    private final ObjectNode required;
    private final ObjectNode filter;
    private final ObjectNode enrichment;

    public IsaPolicy(ObjectNode agreement) {
        this.agreement = agreement.deepCopy();
        drop = requireSection("dropConfig");
        required = requireSection("requiredConfig");
        filter = requireSection("filterConfig");
        enrichment = requireSection("enrichmentConfig");
    }

    private ObjectNode requireSection(String name) {
        if (agreement.path(name) instanceof ObjectNode section) {
            return section;
        }
        throw new IllegalArgumentException("Expected a JSON object for ISA section " + name);
    }

    public boolean matches(ObjectNode other) {
        return IsaRules.isaComparison(other, agreement);
    }

    public String asJson() {
        return agreement.toString();
    }

    /**
     * Applies the agreement to a caller-owned alert. The validator returns schema violations
     * for candidate blank replacements. Discard the alert if a rejection is returned.
     */
    public Optional<String> apply(ObjectNode alert, Function<ObjectNode, ? extends Collection<?>> validate) {
        String dropped = DropPolicy.applyDropCheck(alert, drop);
        if (!dropped.isEmpty()) {
            return Optional.of("Alert rejected: forbidden value found in " + dropped);
        }
        String missing = IsaRules.checkMandatoryFields(alert, required);
        if (!missing.isEmpty()) {
            return Optional.of("Alert rejected: missing mandatory field " + missing);
        }
        String unfilterable = FilterPolicy.applyFilter(alert, filter, validate);
        if (!unfilterable.isEmpty()) {
            return Optional.of("Alert cant be filtered: missing field " + unfilterable);
        }
        try {
            EnrichmentPolicy.applyEnrichment(alert, enrichment);
        } catch (IllegalArgumentException e) {
            return Optional.of("Alert cannot be enriched: " + e.getMessage());
        }
        return Optional.empty();
    }
}
