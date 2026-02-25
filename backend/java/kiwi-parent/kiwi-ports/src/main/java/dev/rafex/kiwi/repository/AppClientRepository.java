/*
 * Copyright 2026 Raúl Eduardo González Argote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.rafex.kiwi.repository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppClientRepository {

	void createClient(UUID appClientId, String clientId, String name, byte[] secretHash, byte[] salt, int iterations,
			List<String> roles) throws SQLException;

	Optional<AppClientRow> findByClientId(String clientId) throws SQLException;

	void touchLastUsed(UUID appClientId) throws SQLException;

	public record AppClientRow(UUID appClientId, String clientId, String name, byte[] secretHash, byte[] salt,
			int iterations, List<String> roles, String status, Instant lastUsedAt, Instant createdAt, Instant updatedAt) {
	}
}
