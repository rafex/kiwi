package dev.rafex.kiwi.services;

import java.sql.SQLException;
import java.util.UUID;

import org.postgresql.util.PSQLException;

import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.logging.Log;

public class LocationServices {

    private final LocationRepository repo;

    public LocationServices(final LocationRepository repo) {
        this.repo = repo;
    }

    public void create(final UUID locationId, final String name, final UUID parentId) throws KiwiError {
        try {
            repo.createLocation(locationId, name, parentId);
        } catch (final PSQLException e) {
            Log.debug(getClass(), "DB error creating location: {} ", e.getMessage());
            if ("23503".equals(e.getSQLState())) {
                throw new KiwiError("E-001", "newLocationId does not exist", e);
            }
            throw new KiwiError("E-002", "DB error creating location", e);
        } catch (final SQLException e) {
            Log.error(getClass(), "Error creating location in DB", e);
            throw new KiwiError("E-002", "Error creating location in DB", e);
        }
    }
}
