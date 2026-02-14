package dev.rafex.kiwi.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {

    public static final ObjectMapper MAPPER = createMapper();

    private JsonUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static ObjectMapper createMapper() {
        // configuración futura aquí
        return new ObjectMapper();
    }

    /**
     * Serializa cualquier objeto a JSON String.
     */
    public static String toJson(final Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Error serializing object to JSON", e);
        }
    }

    /**
     * Escapa correctamente un String para JSON usando Jackson.
     */
    public static String escapeJson(final String value) {
        try {
            // Jackson lo serializa con comillas, las removemos
            final var json = MAPPER.writeValueAsString(value);
            return json.substring(1, json.length() - 1);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Error escaping JSON string", e);
        }
    }
}