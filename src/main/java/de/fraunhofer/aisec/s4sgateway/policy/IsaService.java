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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fraunhofer.aisec.s4sgateway.json.JsonObjectReader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class IsaService {
	private static final Logger logger = LoggerFactory.getLogger(IsaService.class);

	private final JsonObjectReader reader;
	private final String isaPath;
	private volatile IsaPolicy active;

	public IsaService(JsonObjectReader reader, @Value("${isaPath}") String isaPath) {
		this.reader = reader;
		this.isaPath = isaPath;
	}

	public IsaPolicy current() {
		return active;
	}

	public void update(String json) throws JsonProcessingException {
		active = new IsaPolicy(reader.read(json));
	}

	public boolean matches(String json) throws JsonProcessingException {
		return current().matches(reader.read(json));
	}

	@PostConstruct
	public void reload() throws IOException {
		update(loadIsaFromFile());
	}

	private String loadIsaFromFile() throws IOException {

		// 1. Try loading an external ISA
		if (isaPath != null && !isaPath.isBlank()) {

			Path path = Paths.get(isaPath);

			if (Files.isRegularFile(path) && Files.isReadable(path)) {
				logger.info("Loading external ISA: {}", path.toAbsolutePath());

				return Files.readString(path, StandardCharsets.UTF_8);
			}

			logger.warn(
					"External ISA not found or not readable: {}. "
							+ "Trying embedded ISA inside binary instead.",
					path.toAbsolutePath()
			);
		}

		// 2. Fall back to the embedded classpath resource
		String resourcePath = "/isa/isa.json";

		logger.info("Loading embedded ISA from JAR or Graal VM compiled binary: {}", resourcePath);

		try (InputStream input = getClass().getResourceAsStream(resourcePath)) {

			if (input == null) {
				throw new FileNotFoundException(
						"Embedded ISA not found: " + resourcePath
				);
			}

			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
