package dev.rafex.kiwi.services;

import java.util.UUID;

public interface LocationService {

	void create(UUID locationId, String name, UUID parentId) throws Exception;
}
