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
package dev.rafex.kiwi.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa los detalles completos de un objeto en el sistema.
 * Contiene identificador, nombre, descripción, tipo, estado, ubicación actual,
 * etiquetas, metadata y timestamps de creación/actualización.
 *
 * @param objectId            Identificador único del objeto.
 * @param name                Nombre del objeto.
 * @param description         Descripción del objeto.
 * @param type                Tipo de objeto (ej. "document", "image", "asset").
 * @param status              Estado del objeto (ej. "active", "archived", "draft").
 * @param currentLocationId   Identificador de la ubicación actual del objeto.
 * @param tags                Etiquetas asociadas al objeto.
 * @param metadataJson        Metadata en formato JSON.
 * @param createdAt           Fecha y hora de creación del objeto.
 * @param updatedAt           Fecha y hora de última actualización del objeto.
 */
public record ObjectDetail(UUID objectId, String name, String description, String type, String status,
		UUID currentLocationId, String[] tags, String metadataJson, Instant createdAt, Instant updatedAt) {
}