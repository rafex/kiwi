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

public interface AppClientAuthService {

	AuthResult authenticate(String clientId, char[] clientSecret) throws Exception;

	CreateClientResult createClient(String clientId, String name, char[] clientSecret, List<String> roles)
			throws Exception;

	public record AuthResult(boolean ok, UUID appClientId, String clientId, List<String> roles, String code) {
		public static AuthResult ok(final UUID appClientId, final String clientId, final List<String> roles) {
			return new AuthResult(true, appClientId, clientId, roles, null);
		}

		public static AuthResult bad(final String code) {
			return new AuthResult(false, null, null, List.of(), code);
		}
	}

	public record CreateClientResult(boolean ok, UUID appClientId, String clientId, String name, List<String> roles,
			String code) {
		public static CreateClientResult ok(final UUID appClientId, final String clientId, final String name,
				final List<String> roles) {
			return new CreateClientResult(true, appClientId, clientId, name, roles, null);
		}

		public static CreateClientResult bad(final String code) {
			return new CreateClientResult(false, null, null, null, List.of(), code);
		}
	}
}
