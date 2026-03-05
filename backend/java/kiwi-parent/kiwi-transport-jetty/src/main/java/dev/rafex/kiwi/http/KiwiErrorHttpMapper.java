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
package dev.rafex.kiwi.http;

import dev.rafex.kiwi.errors.KiwiError;

public final class KiwiErrorHttpMapper {

	private KiwiErrorHttpMapper() {
	}

	public static MappedHttpError map(final KiwiError error) {
		return map(error, null);
	}

	public static MappedHttpError map(final KiwiError error, final String operation) {
		if (error == null) {
			return new MappedHttpError(500, "internal_server_error", "internal_error", "internal_error");
		}

		final var code = error.getCode();
		if ("object.move".equals(operation)) {
			if ("E-001".equals(code)) {
				return new MappedHttpError(400, "bad_request", code, "newLocationId does not exist");
			}
			if ("E-002".equals(code)) {
				return new MappedHttpError(400, "bad_request", code, "invalid new location");
			}
		}

		if ("location.create".equals(operation)) {
			return new MappedHttpError(500, "internal_server_error", code, "db_error");
		}

		return new MappedHttpError(400, "bad_request", code, "domain_error");
	}

}

