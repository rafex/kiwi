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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.security.JwtService;

public final class JwtAuthHandler extends Handler.Wrapper {

    public static final String REQ_ATTR_AUTH = "auth";

    record Rule(String method, PathSpec pathSpec) {
    }

    private final JwtService jwt;
    private final List<Rule> publicRules = new ArrayList<>();
    private final List<PathSpec> protectedPrefixes = new ArrayList<>();

    public JwtAuthHandler(final Handler delegate, final JwtService jwt) {
        super(delegate);
        this.jwt = Objects.requireNonNull(jwt);
    }

    /** Ruta pública exacta (método + pathspec). */
    public JwtAuthHandler publicPath(final String method, final String pathSpec) {
        publicRules.add(new Rule(method.toUpperCase(), PathSpec.from(pathSpec)));
        return this;
    }

    /** Prefijo protegido (cualquier método) */
    public JwtAuthHandler protectedPrefix(final String pathSpec) {
        protectedPrefixes.add(PathSpec.from(pathSpec));
        return this;
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
        final var method = request.getMethod().toUpperCase();
        final var path = request.getHttpURI() != null ? request.getHttpURI().getPath() : null;

        if (path == null) {
            HttpUtil.json(response, callback, HttpStatus.BAD_REQUEST_400,
                    java.util.Map.of("error", "bad_request", "message", "missing_path"));
            return true;
        }

        // 1) público -> pasa directo
        // 2) si no cae en zona protegida, pasa (si quieres default-deny, elimina esto)
        if (isPublic(method, path) || !isProtected(path)) {
            return super.handle(request, response, callback);
        }

        // 3) protegido -> exige bearer
        final var authz = request.getHeaders().get("authorization");
        if (authz == null || !authz.startsWith("Bearer ")) {
            HttpUtil.json(response, callback, HttpStatus.UNAUTHORIZED_401,
                    java.util.Map.of("error", "unauthorized", "code", "missing_bearer_token"));
            return true;
        }

        final var token = authz.substring("Bearer ".length()).trim();
        final var res = jwt.verify(token, Instant.now().getEpochSecond());
        if (!res.ok()) {
            HttpUtil.json(response, callback, HttpStatus.UNAUTHORIZED_401,
                    java.util.Map.of("error", "unauthorized", "code", res.code()));
            return true;
        }

        // 4) adjunta contexto y sigue
        request.setAttribute(REQ_ATTR_AUTH, res.ctx());
        return super.handle(request, response, callback);
    }

    private boolean isPublic(final String method, final String path) {
        for (final var r : publicRules) {
            if (!r.method().equals(method)) {
                continue;
            }
            if (r.pathSpec().matches(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProtected(final String path) {
        for (final var p : protectedPrefixes) {
            if (p.matches(path)) {
                return true;
            }
        }
        return false;
    }
}