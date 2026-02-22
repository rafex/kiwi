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
package dev.rafex.kiwi.services;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface UserProvisioningService {

	CreateUserResult createUser(final String username, final char[] password, final List<String> roles)
			throws SQLException;

	boolean existsAnyUser() throws SQLException;

	public record CreateUserResult(boolean ok, UUID userId, String code) {
		public static CreateUserResult ok(final UUID userId) {
			return new CreateUserResult(true, userId, null);
		}

		public static CreateUserResult bad(final String code) {
			return new CreateUserResult(false, null, code);
		}
	}

}
