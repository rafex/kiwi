package dev.rafex.kiwi.services;

import java.util.Map;

import dev.rafex.kiwi.json.JsonUtil;
import dev.rafex.kiwi.logging.Log;

public class HelloServices {

    public String sayHello(final String name) {

        final var realName = name == null ? "kiwi" : name;

        Log.debug(getClass(), "sayHello called with name={}", realName);

        final var payload = Map.of(
                "name", realName,
                "status", "ok"
        );

        return JsonUtil.toJson(payload);
    }

    public String sayHello() {
        return sayHello(null);
    }
}
