package dev.rafex.kiwi.services;

import java.sql.SQLException;
import java.util.UUID;

import org.postgresql.util.PSQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.errors.KiwiError;

public class LocationServices {

    private static final Logger LOG = LoggerFactory.getLogger(LocationServices.class);
    private final LocationRepository repo;

    public LocationServices(final LocationRepository repo) {
        this.repo = repo;
    }

    public void create(final UUID locationId, final String name, final UUID parentId) throws KiwiError {
        try {
            repo.createLocation(locationId, name, parentId);
        } catch (final PSQLException e) {
            LOG.debug("DB error creating location: {} ", e.getMessage());
            // FK violation: parent_location_id no existe
            if ("23503".equals(e.getSQLState())) {
                throw new KiwiError("E-001", "newLocationId does not exist", e);
            }

        } catch (final SQLException e) {
            LOG.error("Error creating location in DB", e);
            throw new KiwiError("E-002", "Error creating location in DB", e);
        }
    }
}
