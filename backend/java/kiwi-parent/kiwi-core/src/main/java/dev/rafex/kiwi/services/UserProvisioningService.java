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

import dev.rafex.kiwi.errors.KiwiError;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de aprovisionamiento de usuarios.
 * Maneja la creación y verificación de existencia de usuarios en el sistema.
 */
public interface UserProvisioningService {

	/**
	 * Crea un nuevo usuario.
	 *
	 * @param username Nombre de usuario.
	 * @param password Contraseña del usuario.
	 * @param roles Lista de roles asignados al usuario.
	 * @return Resultado de la creación del usuario.
	 * @throws KiwiError Si ocurre un error durante la creación.
	 */
	CreateUserResult createUser(final String username, final char[] password, final List<String> roles)
			throws KiwiError;

	/**
	 * Verifica si existe al menos un usuario en el sistema.
	 *
	 * @return {@code true} si existe al menos un usuario, {@code false} en caso contrario.
	 * @throws KiwiError Si ocurre un error al verificar la existencia de usuarios.
	 */
	boolean existsAnyUser() throws KiwiError;

	/**
	 * Resultado de la creación de un usuario.
	 *
	 * @param ok Si la creación fue exitosa.
	 * @param userId Identificador del usuario creado.
	 * @param code Código de error (si aplica).
	 */
	public record CreateUserResult(boolean ok, UUID userId, String code) {
		/**
		 * Crea un resultado de creación de usuario exitoso.
		 *
		 * @param userId Identificador del usuario creado.
		 * @return Resultado de creación exitoso.
		 */
		public static CreateUserResult ok(final UUID userId) {
			return new CreateUserResult(true, userId, null);
		}

		/**
		 * Crea un resultado de creación de usuario fallido.
		 *
		 * @param code Código de error que describe la falla.
		 * @return Resultado de creación fallido.
		 */
		public static CreateUserResult bad(final String code) {
			return new CreateUserResult(false, null, code);
		}
	}

}
