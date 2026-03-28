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

import java.util.Map;

/**
 * Servicio de saludo.
 * Proporciona funcionalidad básica para generar mensajes de saludo.
 */
public interface HelloService {

	/**
	 * Saluda a una persona específica.
	 *
	 * @param name Nombre de la persona a saludar.
	 * @return Mapa con el mensaje de saludo.
	 */
	Map<String, String> sayHello(String name);

	/**
	 * Saluda de manera general.
	 *
	 * @return Mapa con el mensaje de saludo genérico.
	 */
	Map<String, String> sayHello();
}
