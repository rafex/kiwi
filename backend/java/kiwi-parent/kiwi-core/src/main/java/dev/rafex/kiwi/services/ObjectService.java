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
import dev.rafex.kiwi.models.FuzzyItem;
import dev.rafex.kiwi.models.ObjectDetail;
import dev.rafex.kiwi.models.SearchItem;
import dev.rafex.kiwi.query.QuerySpec;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de gestión de objetos.
 * Maneja operaciones CRUD, búsqueda y movimientos de objetos en el sistema.
 */
public interface ObjectService {

	/**
	 * Crea un nuevo objeto.
	 *
	 * @param objectId Identificador único del objeto.
	 * @param name Nombre del objeto.
	 * @param description Descripción del objeto.
	 * @param type Tipo del objeto.
	 * @param tags Etiquetas asociadas al objeto.
	 * @param metadataJson Metadatos del objeto en formato JSON.
	 * @param locationId Identificador de la ubicación donde se encuentra el objeto.
	 * @throws Exception Si ocurre un error durante la creación.
	 */
	void create(UUID objectId, String name, String description, String type, String[] tags, String metadataJson,
			UUID locationId) throws Exception;

	/**
	 * Mueve un objeto a una nueva ubicación.
	 *
	 * @param objectId Identificador del objeto a mover.
	 * @param newLocationId Identificador de la nueva ubicación.
	 * @throws KiwiError Si el objeto no existe o no se puede mover.
	 */
	void move(UUID objectId, UUID newLocationId) throws KiwiError;

	/**
	 * Busca objetos según una especificación de consulta.
	 *
	 * @param querySpec Especificación de la consulta con filtros, ordenamiento y paginación.
	 * @return Lista de elementos de búsqueda que coinciden con los criterios.
	 */
	List<SearchItem> search(QuerySpec querySpec);

	/**
	 * Actualiza las etiquetas de un objeto.
	 *
	 * @param objectId Identificador del objeto.
	 * @param tags Nuevas etiquetas para el objeto.
	 * @throws KiwiError Si el objeto no existe o no se pueden actualizar las etiquetas.
	 */
	void updateTags(UUID objectId, String[] tags) throws KiwiError;

	/**
	 * Actualiza el nombre y descripción de un objeto.
	 *
	 * @param objectId Identificador del objeto.
	 * @param name Nuevo nombre del objeto.
	 * @param description Nueva descripción del objeto.
	 * @throws KiwiError Si el objeto no existe o no se puede actualizar el texto.
	 */
	void updateText(UUID objectId, String name, String description) throws KiwiError;

	/**
	 * Actualiza los metadatos de un objeto.
	 *
	 * @param objectId Identificador del objeto.
	 * @param metadataJson Nuevos metadatos en formato JSON.
	 * @throws KiwiError Si el objeto no existe o no se pueden actualizar los metadatos.
	 */
	void updateMetadata(UUID objectId, String metadataJson) throws KiwiError;

	/**
	 * Realiza una búsqueda aproximada (fuzzy) de objetos por texto.
	 *
	 * @param text Texto a buscar.
	 * @param limit Límite máximo de resultados.
	 * @param offset Desplazamiento para paginación.
	 * @return Lista de elementos encontrados mediante búsqueda aproximada.
	 * @throws Exception Si ocurre un error durante la búsqueda.
	 */
	List<FuzzyItem> fuzzy(String text, int limit, int offset) throws Exception;

	/**
	 * Obtiene los detalles de un objeto por su identificador.
	 *
	 * @param objectId Identificador del objeto.
	 * @return Detalles del objeto si existe, vacío si no se encuentra.
	 * @throws Exception Si ocurre un error al recuperar los detalles.
	 */
	Optional<ObjectDetail> getById(UUID objectId) throws Exception;

}
