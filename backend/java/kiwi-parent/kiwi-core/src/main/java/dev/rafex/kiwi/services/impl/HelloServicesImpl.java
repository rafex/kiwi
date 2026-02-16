package dev.rafex.kiwi.services.impl;

import java.util.Map;

import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.services.HelloServices;

public class HelloServicesImpl implements HelloServices {

    @Override
    public Map<String, String> sayHello(final String name) {

        final var realName = name == null ? "kiwi" : name;

        Log.debug(getClass(), "sayHello called with name={}", realName);

        return Map.of("name", realName, "status", "ok");
    }

    @Override
    public Map<String, String> sayHello() {
        return sayHello(null);
    }
}
