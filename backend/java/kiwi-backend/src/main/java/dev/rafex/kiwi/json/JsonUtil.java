package dev.rafex.kiwi.json;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String escapeJson(final String value) {
        return value.replace("\"", "\\\"");
    }
}
