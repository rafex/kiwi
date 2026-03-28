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
 * Servicio de autenticación para clientes de la aplicación.
 * Gestiona la creación y autenticación de clientes (app clients).
 */
public interface AppClientAuthService {

	/**
	 * Autentica un cliente de la aplicación usando su ID y secreto.
	 *
	 * @param clientId Identificador del cliente.
	 * @param clientSecret Secreto del cliente.
	 * @return Resultado de la autenticación con información del cliente.
	 */
	AuthResult authenticate(String clientId, char[] clientSecret);

	/**
	 * Crea un nuevo cliente de la aplicación.
	 *
	 * @param clientId Identificador del cliente.
	 * @param name Nombre del cliente.
	 * @param clientSecret Secreto del cliente.
	 * @param roles Lista de roles asignados al cliente.
	 * @return Resultado de la creación con información del cliente.
	 */
	CreateClientResult createClient(String clientId, String name, char[] clientSecret, List<String> roles);

	/**
	 * Resultado de la autenticación de un cliente.
	 *
	 * @param ok Si la autenticación fue exitosa.
	 * @param appClientId Identificador del cliente de la aplicación.
	 * @param clientId Identificador del cliente.
	 * @param roles Lista de roles del cliente.
	 * @param code Código de error (si aplica).
	 */
	public record AuthResult(boolean ok, UUID appClientId, String clientId, List<String> roles, String code) {
		/**
		 * Crea un resultado de autenticación exitoso.
		 *
		 * @param appClientId Identificador del cliente de la aplicación.
		 * @param clientId Identificador del cliente.
		 * @param roles Lista de roles del cliente.
		 * @return Resultado de autenticación exitoso.
		 */
		public static AuthResult ok(final UUID appClientId, final String clientId, final List<String> roles) {
			return new AuthResult(true, appClientId, clientId, roles, null);
		}

		/**
		 * Crea un resultado de autenticación fallido.
		 *
		 * @param code Código de error que describe la falla.
		 * @return Resultado de autenticación fallido.
		 */
		public static AuthResult bad(final String code) {
			return new AuthResult(false, null, null, List.of(), code);
		}
	}

	/**
	 * Resultado de la creación de un cliente.
	 *
	 * @param ok Si la creación fue exitosa.
	 * @param appClientId Identificador del cliente creado.
	 * @param clientId Identificador del cliente.
	 * @param name Nombre del cliente.
	 * @param roles Lista de roles asignados.
	 * @param code Código de error (si aplica).
	 */
	public record CreateClientResult(boolean ok, UUID appClientId, String clientId, String name, List<String> roles,
			String code) {
		/**
		 * Crea un resultado de creación exitoso.
		 *
		 * @param appClientId Identificador del cliente creado.
		 * @param clientId Identificador del cliente.
		 * @param name Nombre del cliente.
		 * @param roles Lista de roles asignados.
		 * @return Resultado de creación exitoso.
		 */
		public static CreateClientResult ok(final UUID appClientId, final String clientId, final String name,
				final List<String> roles) {
			return new CreateClientResult(true, appClientId, clientId, name, roles, null);
		}

		/**
		 * Crea un resultado de creación fallido.
		 *
		 * @param code Código de error que describe la falla.
		 * @return Resultado de creación fallido.
		 */
		public static CreateClientResult bad(final String code) {
			return new CreateClientResult(false, null, null, null, List.of(), code);
		}
	}
}
