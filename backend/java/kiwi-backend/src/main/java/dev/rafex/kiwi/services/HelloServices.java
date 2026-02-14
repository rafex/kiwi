package dev.rafex.kiwi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.rafex.kiwi.json.JsonUtil;

public class HelloServices {

    private static final Logger LOG = LoggerFactory.getLogger(HelloServices.class);

    public record HelloResponse(String name, String status) {
    }

    public HelloResponse sayHello(final String name) {

        final var displayName = name == null ? "kiwi" : name;

        LOG.debug("sayHello called with name={}", displayName);

        return new HelloResponse(displayName, "ok");
    }

    public HelloResponse sayHello() {
        return sayHello(null);
    }
}
