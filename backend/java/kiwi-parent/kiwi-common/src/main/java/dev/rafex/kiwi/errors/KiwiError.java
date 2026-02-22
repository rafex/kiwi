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
package dev.rafex.kiwi.errors;

import dev.rafex.kiwi.logging.Log;

public class KiwiError extends Exception {

	private static final long serialVersionUID = 1L;

	private final String code;

	/* ===================== CONSTRUCTORS ===================== */

	public KiwiError(final String code, final String message) {
		super(message);
		this.code = code;
		Log.error(getClass(), "KiwiError [{}]: {}", code, message);
	}

	public KiwiError(final String code, final String message, final Object... args) {
		this(code, format(message, args));
	}

	public KiwiError(final String code, final String message, final Throwable cause) {
		super(message, cause);
		this.code = code;
		Log.error(getClass(), cause, "KiwiError [{}]: {}", code, message);
	}

	public KiwiError(final String code, final String message, final Throwable cause, final Object... args) {
		this(code, format(message, args), cause);
	}

	/* ===================== GETTERS ===================== */

	public String getCode() {
		return code;
	}

	/* ===================== INTERNAL FORMAT ===================== */

	private static String format(final String message, final Object... args) {
		if (args == null || args.length == 0) {
			return message;
		}
		final var sb = new StringBuilder(message.length() + 64);
		var argIdx = 0;
		var start = 0;
		int idx;
		while ((idx = message.indexOf("{}", start)) >= 0 && argIdx < args.length) {
			sb.append(message, start, idx);
			sb.append(args[argIdx] == null ? "null" : args[argIdx].toString());
			start = idx + 2;
			argIdx++;
		}
		sb.append(message, start, message.length());
		return sb.toString();
	}
}