package dev.rafex.kiwi.repository.impl;

import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import javax.sql.DataSource;

import dev.rafex.kiwi.repository.LocationRepository;

public class LocationRepositoryImpl implements LocationRepository {

    private final DataSource ds;

    public LocationRepositoryImpl(final DataSource ds) {
        this.ds = ds;
    }

    @Override
    public void createLocation(final UUID locationId, final String name, final UUID parentLocationId) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT api_create_location(?::uuid, ?, ?::uuid)")) {

            ps.setObject(1, locationId);
            ps.setString(2, name);

            if (parentLocationId == null) {
                ps.setNull(3, Types.OTHER);
            } else {
                ps.setObject(3, parentLocationId);
            }

            ps.execute();
        }
    }

    // TODO falta implementar
    @Override
    public boolean locationExists(final UUID locationId) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT 1 FROM locations WHERE location_id = ?::uuid")) {
            ps.setObject(1, locationId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

}
