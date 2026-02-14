package dev.rafex.kiwi.services;

import java.sql.SQLException;
import java.util.UUID;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.postgresql.util.PSQLException;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.dtos.CreateLocationRequest;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.http.HttpUtil;
import dev.rafex.kiwi.logging.Log;

public class LocationServices {

    private final LocationRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    public LocationServices(final LocationRepository repo) {
        this.repo = repo;
    }

    public void create(final UUID locationId, final String name, final UUID parentId) throws KiwiError {
        try {
            repo.createLocation(locationId, name, parentId);
        } catch (final PSQLException e) {
            Log.debug(getClass(), "DB error creating location: {} ", e.getMessage());
            // FK violation: parent_location_id no existe
            if ("23503".equals(e.getSQLState())) {
                throw new KiwiError("E-001", "newLocationId does not exist", e);
            }

        } catch (final SQLException e) {
            Log.error(getClass(), "Error creating location in DB", e);
            throw new KiwiError("E-002", "Error creating location in DB", e);
        }
    }
}
