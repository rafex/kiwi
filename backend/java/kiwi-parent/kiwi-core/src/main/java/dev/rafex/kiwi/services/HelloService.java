package dev.rafex.kiwi.services;

import java.util.Map;

public interface HelloService {

	Map<String, String> sayHello(String name);

	Map<String, String> sayHello();
}
