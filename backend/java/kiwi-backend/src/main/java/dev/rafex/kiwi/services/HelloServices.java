package dev.rafex.kiwi.services;

import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.logging.Log;

public class HelloServices {

    public String sayHello(final String name) {

        final var safeName = name == null ? "kiwi" : JsonUtil.escapeJson(name);

        Log.debug(getClass(), "sayHello called with name={}", safeName);

        return """
                {
                  "name": "%s",
                  "status": "ok"
                }
                """.formatted(safeName);
    }

    public String sayHello() {
        return sayHello(null);
    }
}
