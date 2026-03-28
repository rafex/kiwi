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

/**
 * Servicio de autenticación de usuarios.
 * <p>
 * Gestiona la autenticación de usuarios mediante nombre de usuario y contraseña.
 * Utiliza PBKDF2 para verificación segura de contraseñas.
 */
public interface AuthService {

	/**
	 * Autentica un usuario usando nombre de usuario y contraseña.
	 *
	 * @param username Nombre de usuario.
	 * @param password Contraseña en formato char array (se limpia después del uso).
	 * @return Resultado de la autenticación con información del usuario.
	 */
	AuthResult authenticate(String username, char[] password);

	/**
	 * Resultado de una operación de autenticación.
	 *
	 * @param ok       {@code true} si la autenticación fue exitosa.
	 * @param userId   Identificador del usuario autenticado.
	 * @param username Nombre de usuario.
	 * @param roles    Roles asignados al usuario.
	 * @param code     Código de error en caso de fallo.
	 */
	public record AuthResult(boolean ok, UUID userId, String username, List<String> roles, String code) {
		/**
		 * Crea un resultado de autenticación exitosa.
		 *
		 * @param userId   Identificador del usuario autenticado.
		 * @param username Nombre de usuario.
		 * @param roles    Roles asignados al usuario.
		 * @return Resultado de autenticación exitosa.
		 */
		public static AuthResult ok(final UUID userId, final String username, final List<String> roles) {
			return new AuthResult(true, userId, username, roles, null);
		}

		/**
		 * Crea un resultado de autenticación fallida.
		 *
		 * @param code Código de error que describe la razón del fallo.
		 * @return Resultado de autenticación fallida.
		 */
		public static AuthResult bad(final String code) {
			return new AuthResult(false, null, null, List.of(), code);
		}
	}
}