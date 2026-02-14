package dev.rafex.kiwi.db;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

public class ObjectRepository {

    private final DataSource ds;

    public ObjectRepository(final DataSource ds) {
        this.ds = ds;
    }

    // --- Commands (RETURNS void) ---

    public void createObject(final UUID objectId, final String name, final String description, final String type, final String[] tags, final String metadataJson,
            final UUID locationId) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT api_create_object(?::uuid, ?, ?, ?, ?::text[], ?::jsonb, ?::uuid)")) {

            ps.setObject(1, objectId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, type);

            if (tags == null) {
                ps.setNull(5, Types.ARRAY);
            } else {
                ps.setArray(5, c.createArrayOf("text", tags));
            }

            if (metadataJson == null) {
                ps.setNull(6, Types.OTHER);
            } else {
                ps.setString(6, metadataJson);
            }

            ps.setObject(7, locationId);

            ps.execute(); // void
        }
    }

    public void moveObject(final UUID objectId, final UUID newLocationId) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT api_move_object(?::uuid, ?::uuid)")) {
            ps.setObject(1, objectId);
            ps.setObject(2, newLocationId);
            ps.execute();
        }
    }

    public void updateTags(final UUID objectId, final String[] tags) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT api_update_tags(?::uuid, ?::text[])")) {
            ps.setObject(1, objectId);
            if (tags == null) {
                ps.setNull(2, Types.ARRAY);
            } else {
                ps.setArray(2, c.createArrayOf("text", tags));
            }
            ps.execute();
        }
    }

    public void updateText(final UUID objectId, final String name, final String description) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT api_update_text(?::uuid, ?, ?)")) {
            ps.setObject(1, objectId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.execute();
        }
    }

    // actualiza metadata JSONB
    public void updateMetadata(final UUID objectId, final String metadataJson) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT api_update_metadata(?::uuid, ?::jsonb)")) {
            ps.setObject(1, objectId);
            if (metadataJson == null) {
                ps.setNull(2, Types.OTHER);
            } else {
                ps.setString(2, metadataJson);
            }
            ps.execute();
        }
    }

    // --- Queries (RETURNS TABLE) ---

    public List<SearchRow> search(final String query, final String[] tags, final UUID locationId, final int limit) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT object_id, name, rank FROM api_search_objects(?, ?::text[], ?::uuid, ?)")) {

            ps.setString(1, query);

            if (tags == null) {
                ps.setNull(2, Types.ARRAY);
            } else {
                ps.setArray(2, c.createArrayOf("text", tags));
            }

            if (locationId == null) {
                ps.setNull(3, Types.OTHER);
            } else {
                ps.setObject(3, locationId);
            }

            ps.setInt(4, limit);

            try (var rs = ps.executeQuery()) {
                final List<SearchRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new SearchRow((UUID) rs.getObject("object_id"), rs.getString("name"), rs.getFloat("rank")));
                }
                return out;
            }
        }
    }

    public List<FuzzyRow> fuzzy(final String text, final int limit) throws SQLException {
        try (var c = ds.getConnection(); var ps = c.prepareStatement("SELECT object_id, name, score FROM api_fuzzy_search(?, ?)")) {
            ps.setString(1, text);
            ps.setInt(2, limit);

            try (var rs = ps.executeQuery()) {
                final List<FuzzyRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new FuzzyRow((UUID) rs.getObject("object_id"), rs.getString("name"), rs.getFloat("score")));
                }
                return out;
            }
        }
    }

    public record SearchRow(UUID objectId, String name, float rank) {
    }

    public record FuzzyRow(UUID objectId, String name, float score) {
    }

}
