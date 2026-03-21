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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

	void createUser(final UUID userId, String username, byte[] passwordHash, byte[] salt, int iterations);

	Optional<UserRow> findByUsername(String username);

	List<String> findRoleNamesByUserId(UUID userId);

	Optional<UserWithRoles> findByUsernameWithRoles(String username);

	int countUsers();

	public record UserRow(UUID userId, String username, byte[] passwordHash, byte[] salt, int iterations, String status,
			Instant createdAt, Instant updatedAt) {
	}

	public record UserWithRoles(UserRow user, List<String> roles) {
	}
}
