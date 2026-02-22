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
package dev.rafex.kiwi.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.dtos.CreateObjectRequest;
import dev.rafex.kiwi.dtos.FuzzyResponse;
import dev.rafex.kiwi.dtos.MoveObjectRequest;
import dev.rafex.kiwi.dtos.SearchResponse;
import dev.rafex.kiwi.dtos.UpdateTagsRequest;
import dev.rafex.kiwi.dtos.UpdateTextRequest;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.ObjectService;

public class ObjectHandler extends Handler.Abstract {

    private final ObjectService service;
    private final ObjectMapper om;

    public ObjectHandler(final ObjectService service) {
        this.service = service;
        om = JsonUtil.MAPPER;
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        try {
            final var method = request.getMethod();
            final var path = request.getHttpURI().getPath();

            if ("POST".equals(method) && "/objects".equals(path)) {
                Log.info(getClass(), "Handling object creation request");
                return create(request, response, callback);
            }

            if ("PATCH".equals(method) && path.startsWith("/objects/")) {
                final var uuidStart = 9; // "/objects/".length()
                final var lastSlash = path.lastIndexOf('/');
                if (lastSlash > uuidStart) {
                    final var suffix = path.substring(lastSlash); // "/move", "/tags", "/text"
                    final var uuidStr = path.substring(uuidStart, lastSlash);
                    final UUID objectId;
                    try {
                        objectId = UUID.fromString(uuidStr);
                    } catch (final IllegalArgumentException e) {
                        HttpUtil.badRequest(response, callback, "invalid UUID in path");
                        return true;
                    }
                    return switch (suffix) {
                    case "/move" -> moveLocation(request, response, callback, objectId);
                    case "/tags" -> updateTags(request, response, callback, objectId);
                    case "/text" -> updateText(request, response, callback, objectId);
                    default -> {
                        HttpUtil.notFound(response, callback);
                        yield true;
                    }
                    };
                }
            }

            if ("GET".equals(method) && "/objects".equals(path)) {
                HttpUtil.notFound(response, callback);
                return true;
            }

            if ("GET".equals(method) && path.startsWith("/objects/") && !path.startsWith("/objects/search")
                    && !path.startsWith("/objects/fuzzy")) {
                final var objectIdStr = path.substring("/objects/".length());
                if (objectIdStr.isBlank() || objectIdStr.contains("/")) {
                    HttpUtil.notFound(response, callback);
                    return true;
                }

                final UUID objectId;
                try {
                    objectId = UUID.fromString(objectIdStr);
                } catch (final IllegalArgumentException e) {
                    HttpUtil.badRequest(response, callback, "invalid UUID in path");
                    return true;
                }

                return byId(request, response, callback, objectId);
            }

            if ("GET".equals(method) && "/objects/search".equals(path)) {
                return search(request, response, callback);
            }

            if ("GET".equals(method) && "/objects/fuzzy".equals(path)) {
                return fuzzySearch(request, response, callback);
            }

            HttpUtil.notFound(response, callback);
            return true;
        } catch (final Throwable t) {

            Log.error(getClass(), "Error handling request", t);

            response.setStatus(500);
            callback.failed(t);
            return true;
        }
    }

    private boolean byId(final Request request, final Response response, final Callback callback, final UUID objectId) {
        try {
            final var objectOpt = service.getById(objectId);
            if (objectOpt.isEmpty()) {
                HttpUtil.notFound(response, callback);
                return true;
            }

            final var object = objectOpt.get();
            final var responseBody = new LinkedHashMap<String, Object>();
            responseBody.put("object_id", object.objectId());
            responseBody.put("name", object.name());
            responseBody.put("description", object.description());
            responseBody.put("type", object.type());
            responseBody.put("status", object.status());
            responseBody.put("current_location_id", object.currentLocationId());
            responseBody.put("tags", object.tags() == null ? List.of() : Arrays.asList(object.tags()));
            if (object.metadataJson() == null || object.metadataJson().isBlank()) {
                responseBody.put("metadata", null);
            } else {
                responseBody.put("metadata", om.readTree(object.metadataJson()));
            }
            responseBody.put("created_at", object.createdAt());
            responseBody.put("updated_at", object.updatedAt());

            HttpUtil.ok(response, callback, om.writeValueAsString(responseBody));
            return true;
        } catch (final Exception e) {
            Log.error(getClass(), "Error getting object by id", e);
            HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
            return true;
        }
    }

    private boolean search(final Request request, final Response response, final Callback callback) {
        try {
            final var qp = parseQuery(request.getHttpURI().getQuery());
            final var q = queryParam(qp, "q");

            if (q == null || q.isBlank()) {
                HttpUtil.badRequest(response, callback, "q is required");
                return true;
            }

            final var tagsParam = queryParam(qp, "tags"); // "a,b,c"
            final var locationParam = queryParam(qp, "locationId"); // uuid
            final var limitParam = queryParam(qp, "limit"); // int

            final var tags = parseTags(tagsParam);
            final var locationId = parseUuidOrNull(locationParam);
            final var limit = parseLimit(limitParam, 20, 1, 200);

            final var search = service.search(q.trim(), tags, locationId, limit);

            HttpUtil.ok(response, callback, om.writeValueAsString(new SearchResponse(search)));
            return true;

        } catch (final IllegalArgumentException e) {
            HttpUtil.badRequest(response, callback, e.getMessage());
            return true;

        } catch (final Exception e) {
            Log.error(getClass(), "Error searching objects", e);
            HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
            return true;
        }
    }

    // -------- helpers (sin libs extra) --------

    private static MultiMap<String> parseQuery(final String rawQuery) {
        final var params = new MultiMap<String>();
        if (rawQuery != null && !rawQuery.isEmpty()) {
            UrlEncoded.decodeTo(rawQuery, params, StandardCharsets.UTF_8);
        }
        return params;
    }

    private static String queryParam(final MultiMap<String> params, final String key) {
        final var v = params.getValue(key);
        return v == null || v.isBlank() ? null : v;
    }

    private static String[] parseTags(final String tagsParam) {
        if (tagsParam == null) {
            return null;
        }
        final var t = tagsParam.trim();
        if (t.isEmpty()) {
            return null;
        }

        final var parts = t.split(",");
        final var list = new ArrayList<String>(parts.length);
        for (final String p : parts) {
            final var s = p.trim();
            if (!s.isEmpty()) {
                list.add(s);
            }
        }
        return list.isEmpty() ? null : list.toArray(new String[0]);
    }

    private static UUID parseUuidOrNull(final String uuidStr) {
        if (uuidStr == null) {
            return null;
        }
        final var v = uuidStr.trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(v);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid UUID in locationId");
        }
    }

    private static int parseLimit(final String limitStr, final int def, final int min, final int max) {
        if (limitStr == null || limitStr.trim().isEmpty()) {
            return def;
        }
        try {
            final var v = Integer.parseInt(limitStr.trim());
            if (v < min) {
                return min;
            }
            if (v > max) {
                return max;
            }
            return v;
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("limit must be an integer");
        }
    }

    private boolean moveLocation(final Request request, final Response response, final Callback callback,
            final UUID objectId) {

        try {
            Log.info(getClass(), "Handling object move request");

            final var r = om.readValue(Request.asInputStream(request), MoveObjectRequest.class);

            if (r.newLocationId() == null || r.newLocationId().isBlank()) {
                HttpUtil.badRequest(response, callback, "newLocationId is required");
                return true;
            }

            UUID newLocationId;
            try {
                newLocationId = UUID.fromString(r.newLocationId());
            } catch (final IllegalArgumentException e) {
                HttpUtil.badRequest(response, callback, "invalid UUID in newLocationId");
                return true;
            }

            service.move(objectId, newLocationId);

            // 204 No Content
            HttpUtil.ok_noContent(response, callback);
            return true;

        } catch (final KiwiError e) {

            return switch (e.getCode()) {
            case "E-001" -> {
                HttpUtil.badRequest(response, callback, "newLocationId does not exist");
                yield true;
            }
            case "E-002" -> {
                HttpUtil.badRequest(response, callback, "invalid new location");
                yield true;
            }
            default -> {
                Log.error(getClass(), "KiwiError moving object", e);
                HttpUtil.json(response, callback, 400, "{\"error\":\"" + e.getCode() + "\"}");
                yield true;
            }
            };

        } catch (final IOException e) {
            Log.error(getClass(), "Error moving object", e);
            HttpUtil.badRequest(response, callback, "invalid request body");
            return true;
        }
    }

    private boolean create(final Request request, final Response response, final Callback callback) {
        try {

            final var r = om.readValue(Request.asInputStream(request), CreateObjectRequest.class);

            // validaciones mínimas
            if (r.name() == null || r.name().isBlank()) {
                HttpUtil.badRequest(response, callback, "name is required");
                return true;
            }
            if (r.locationId() == null || r.locationId().isBlank()) {
                HttpUtil.badRequest(response, callback, "locationId is required");
                return true;
            }

            final var objectId = UUID.randomUUID();
            final var locationId = UUID.fromString(r.locationId());

            final var tags = r.tags() == null ? null : r.tags().toArray(new String[0]);
            final var metadataJson = r.metadata() == null ? null : om.writeValueAsString(r.metadata());

            service.create(objectId, r.name(), r.description(), r.type(), tags, metadataJson, locationId);

            HttpUtil.json(response, callback, 201, "{\"object_id\":\"" + objectId + "\"}");
            return true;

        } catch (final KiwiError e) {
            Log.error(getClass(), "KiwiError creating object", e);
            HttpUtil.json(response, callback, 400, "{\"error\":\"" + e.getCode() + "\"}");
            return true;
        } catch (final IllegalArgumentException e) {
            Log.error(getClass(), "Invalid UUID format", e);
            HttpUtil.badRequest(response, callback, "invalid UUID");
            return true;
        } catch (final Exception e) {
            Log.error(getClass(), "Error creating object", e);
            HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
            return true;
        }
    }

    private boolean updateTags(final Request request, final Response response, final Callback callback,
            final UUID objectId) {
        try {
            Log.info(getClass(), "Handling object tag update request");

            final var r = om.readValue(Request.asInputStream(request), UpdateTagsRequest.class);

            if (r.tags() == null) {
                HttpUtil.badRequest(response, callback, "tags is required");
                return true;
            }

            final var tags = r.tags().toArray(String[]::new);
            service.updateTags(objectId, tags);

            // 204 No Content
            HttpUtil.ok_noContent(response, callback);
            return true;

        } catch (final KiwiError e) {
            Log.error(getClass(), "KiwiError updating tags", e);
            HttpUtil.json(response, callback, 400, "{\"error\":\"" + e.getCode() + "\"}");
            return true;
        } catch (final IOException e1) {
            Log.error(getClass(), "Error updating tags", e1);
            HttpUtil.json(response, callback, 400, "{\"error\":\"" + e1.getMessage() + "\"}");
            return true;
        }
    }

    private boolean updateText(final Request request, final Response response, final Callback callback,
            final UUID objectId) {

        try {
            Log.info(getClass(), "Handling object text update request");

            final var r = om.readValue(Request.asInputStream(request), UpdateTextRequest.class);

            if ((r.name() == null || r.name().isBlank()) && (r.description() == null || r.description().isBlank())) {
                HttpUtil.badRequest(response, callback, "name or description is required");
                return true;
            }

            service.updateText(objectId, r.name(), r.description());

            // 204 No Content
            HttpUtil.ok_noContent(response, callback);
            return true;

        } catch (final KiwiError e) {
            Log.error(getClass(), "KiwiError updating text", e);
            HttpUtil.json(response, callback, 400, "{\"error\":\"" + e.getCode() + "\"}");
            return true;
        } catch (final IOException e1) {
            Log.error(getClass(), "Error updating text", e1);
            HttpUtil.json(response, callback, 400, "{\"error\":\"" + e1.getMessage() + "\"}");
            return true;
        }

    }

    private boolean fuzzySearch(final Request request, final Response response, final Callback callback) {
        try {
            Log.info(getClass(), "Handling object fuzzy search request");

            final var qp = parseQuery(request.getHttpURI().getQuery());
            final var name = queryParam(qp, "name");
            if (name == null || name.isBlank()) {
                HttpUtil.badRequest(response, callback, "name is required");
                return true;
            }

            final var search = service.fuzzy(name, 20);

            HttpUtil.ok(response, callback, om.writeValueAsString(new FuzzyResponse(search)));

            return true;

        } catch (final Exception e) {
            Log.error(getClass(), "Error handling fuzzy search", e);
            HttpUtil.json(response, callback, 500, "{\"error\":\"internal_error\"}");
            return true;
        }
    }
}
