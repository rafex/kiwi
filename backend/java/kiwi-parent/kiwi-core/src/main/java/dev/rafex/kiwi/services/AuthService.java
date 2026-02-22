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

import java.util.List;
import java.util.UUID;

public interface AuthService {

	AuthResult authenticate(String username, char[] password) throws Exception;

	public record AuthResult(boolean ok, UUID userId, String username, List<String> roles, String code) {
		public static AuthResult ok(final UUID userId, final String username, final List<String> roles) {
			return new AuthResult(true, userId, username, roles, null);
		}

		public static AuthResult bad(final String code) {
			return new AuthResult(false, null, null, List.of(), code);
		}
	}
}