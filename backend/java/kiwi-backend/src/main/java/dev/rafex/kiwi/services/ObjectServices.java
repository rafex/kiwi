package dev.rafex.kiwi.services;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.postgresql.util.PSQLException;

import dev.rafex.kiwi.db.ObjectRepository;
import dev.rafex.kiwi.dtos.SearchItemDto;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.logging.Log;

public class ObjectServices {

    private final ObjectRepository repo;

    public ObjectServices(final ObjectRepository repo) {
        this.repo = repo;
    }

    public void create(final UUID objectId, final String name, final String description, final String type, final String[] tags, final String metadataJson, final UUID locationId)
            throws KiwiError {
        try {
            repo.createObject(objectId, name, description, type, tags, metadataJson, locationId);
        } catch (final SQLException e) {
            Log.error(getClass(), "Error creating object in DB", e);
            throw new KiwiError("E-003", "Error creating object in DB", e);
        }
    }

    public void move(final UUID objectId, final UUID newLocationId) throws KiwiError {
        try {
            repo.moveObject(objectId, newLocationId);
        } catch (final PSQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new KiwiError("E-001", "newLocationId does not exist", e);
            }
            Log.error(getClass(), "DB error moving object", e);

        } catch (final SQLException e) {
            Log.error(getClass(), "Error moving object", e);
            throw new KiwiError("E-002", "SQLException", e);
        }

    }

    public List search(final String trim, final String[] tags, final UUID locationId, final int limit) {

        final var itemsList = new ArrayList<SearchItemDto>();
        try {
            final var rows = repo.search(trim, tags, locationId, limit);
            itemsList.ensureCapacity(rows.size());
            
            for (final var r : rows) {
                itemsList.add(new SearchItemDto(r.objectId(), r.name(), r.rank()));
            }

        } catch (final SQLException e) {
            Log.error(getClass(), "Error searching for objects", e);
        }
        return itemsList;
    }

}