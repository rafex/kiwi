package dev.rafex.kiwi.repository;

import java.sql.SQLException;
import java.util.UUID;

public interface LocationRepository {

    void createLocation(UUID locationId, String name, UUID parentLocationId) throws SQLException;

    boolean locationExists(UUID locationId) throws SQLException;

}
