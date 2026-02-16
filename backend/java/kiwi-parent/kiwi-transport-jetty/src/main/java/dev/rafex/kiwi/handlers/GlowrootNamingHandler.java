package dev.rafex.kiwi.handlers;

import java.util.regex.Pattern;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.glowroot.agent.api.Glowroot;

import dev.rafex.kiwi.logging.Log;

/**
 * Handler que normaliza el nombre de la transacción para Glowroot y añade
 * atributos útiles para su trazabilidad.
 *
 * Mejoras realizadas: - Precompila patrones regex para evitar recompilación por
 * petición. - Extrae y hace package-private {@code normalizePath} para
 * facilitar pruebas unitarias. - Añade atributo con la ruta normalizada para
 * facilitar búsquedas en Glowroot. - Colapsa slashes consecutivos y soporta
 * ObjectId (24 hex) además de UUID y números.
 */
public class GlowrootNamingHandler extends Handler.Wrapper {

    // Precompilación de patrones que se usan por petición.
    private static final Pattern UUID_PATTERN = Pattern.compile("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
    private static final Pattern OBJECTID_PATTERN = Pattern.compile("/[0-9a-fA-F]{24}(?=/|$)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("/\\d{2,}(?=/|$)");
    private static final Pattern MULTIPLE_SLASHES_PATTERN = Pattern.compile("/{2,}");

    public GlowrootNamingHandler(final Handler delegate) {
        super(delegate);
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

        final var method = request.getMethod();
        final var path = request.getHttpURI() != null ? request.getHttpURI().getPath() : null;

        // Normalizamos la ruta lo antes posible y la usamos para nombrar la transacción
        final var normalized = normalizePath(path);

        glowroot(method, path, normalized);

        // (Opcional) usuario, si tú lo conoces (header, token, etc.)
        // Glowroot.setTransactionUser(userId);

        try {
            return super.handle(request, response, callback);
        } catch (final Throwable t) {
            // En esta API no siempre hay setError(). Si el exception se propaga,
            // Glowroot suele capturar el error igual. Si quieres forzar detalle:
            try {
                Glowroot.addTransactionAttribute("error", t.getClass().getName());
                Glowroot.addTransactionAttribute("error.message", safeMsg(t.getMessage()));
            } catch (final Throwable ignore) {
                // No queremos que un error aquí afecte la petición.
                Log.error(getClass(), "Error setting Glowroot error attributes", ignore);
            }
            throw t;
        }
    }

    private void glowroot(final String method, final String path, final String normalized) {

        try {
            // Tipo y nombre para agregación en Glowroot
            Glowroot.setTransactionType("Web");
            Glowroot.setTransactionName(method + " " + normalized);

            // Atributos útiles para ver en Trace Explorer
            Glowroot.addTransactionAttribute("http.method", method);
            Glowroot.addTransactionAttribute("http.path", path == null ? "unknown" : path);
            Glowroot.addTransactionAttribute("http.normalized_path", normalized);
        } catch (final Throwable t) {
            // En caso de error en Glowroot, no queremos afectar la petición.
            // Logueamos el error y seguimos adelante.
            Log.error(getClass(), "Error setting Glowroot transaction name", t);
        }
    }

    private static String safeMsg(final String s) {
        return s == null ? "" : s;
    }

    /**
     * Normaliza la ruta HTTP reemplazando identificadores por tokens: - UUIDs =>
     * :id - ObjectId (24 hex) => :id - Números largos (>=2 dígitos) => :n También
     * colapsa múltiples slashes consecutivos en uno solo.
     *
     * Se deja package-private para facilitar pruebas unitarias.
     */
    static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }

        // Colapsar slashes repetidos (e.g. //foo///bar -> /foo/bar)
        path = MULTIPLE_SLASHES_PATTERN.matcher(path).replaceAll("/");

        // Reemplazos por patrones precompilados
        path = UUID_PATTERN.matcher(path).replaceAll("/:id");
        path = OBJECTID_PATTERN.matcher(path).replaceAll("/:id");
        return NUMBER_PATTERN.matcher(path).replaceAll("/:n");
    }
}