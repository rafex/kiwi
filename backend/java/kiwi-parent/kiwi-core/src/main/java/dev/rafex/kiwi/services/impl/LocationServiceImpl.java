package dev.rafex.kiwi.services.impl;

import java.sql.SQLException;
import java.util.UUID;

import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.repository.LocationRepository;
import dev.rafex.kiwi.services.LocationService;

public class LocationServiceImpl implements LocationService {

    private final LocationRepository repo;

    public LocationServiceImpl(final LocationRepository repo) {
        this.repo = repo;
    }

    @Override
    public void create(final UUID locationId, final String name, final UUID parentId) throws KiwiError {
        try {
            repo.createLocation(locationId, name, parentId);
        } catch (final SQLException e) {
            Log.debug(getClass(), "DB error creating location: {} ", e.getMessage());
            if ("23503".equals(e.getSQLState())) {
                throw new KiwiError("E-001", "newLocationId does not exist", e);
            }
            throw new KiwiError("E-002", "DB error creating location", e);
        }
    }
}
