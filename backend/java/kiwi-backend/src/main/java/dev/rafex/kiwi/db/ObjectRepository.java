package dev.rafex.kiwi.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

public class ObjectRepository {

	private final DataSource ds;

	public ObjectRepository(DataSource ds) {
		this.ds = ds;
	}

	// --- Commands (RETURNS void) ---

	public void createObject(UUID objectId, String name, String description, String type, String[] tags,
			String metadataJson, UUID locationId) throws SQLException {
		try (Connection c = ds.getConnection();
				PreparedStatement ps = c
						.prepareStatement("SELECT api_create_object(?::uuid, ?, ?, ?, ?::text[], ?::jsonb, ?::uuid)")) {

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

	public void moveObject(UUID objectId, UUID newLocationId) throws SQLException {
		try (Connection c = ds.getConnection();
				PreparedStatement ps = c.prepareStatement("SELECT api_move_object(?::uuid, ?::uuid)")) {
			ps.setObject(1, objectId);
			ps.setObject(2, newLocationId);
			ps.execute();
		}
	}

	public void updateTags(UUID objectId, String[] tags) throws SQLException {
		try (Connection c = ds.getConnection();
				PreparedStatement ps = c.prepareStatement("SELECT api_update_tags(?::uuid, ?::text[])")) {
			ps.setObject(1, objectId);
			if (tags == null) {
				ps.setNull(2, Types.ARRAY);
			} else {
				ps.setArray(2, c.createArrayOf("text", tags));
			}
			ps.execute();
		}
	}

	public void updateText(UUID objectId, String name, String description) throws SQLException {
		try (Connection c = ds.getConnection();
				PreparedStatement ps = c.prepareStatement("SELECT api_update_text(?::uuid, ?, ?)")) {
			ps.setObject(1, objectId);
			ps.setString(2, name);
			ps.setString(3, description);
			ps.execute();
		}
	}

	// --- Queries (RETURNS TABLE) ---

	public List<SearchRow> search(String query, String[] tags, UUID locationId, int limit) throws SQLException {
		try (Connection c = ds.getConnection();
				PreparedStatement ps = c.prepareStatement(
						"SELECT object_id, name, rank FROM api_search_objects(?, ?::text[], ?::uuid, ?)")) {

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

			try (ResultSet rs = ps.executeQuery()) {
				List<SearchRow> out = new ArrayList<>();
				while (rs.next()) {
					out.add(new SearchRow((UUID) rs.getObject("object_id"), rs.getString("name"), rs.getFloat("rank")));
				}
				return out;
			}
		}
	}

	public List<FuzzyRow> fuzzy(String text, int limit) throws SQLException {
		try (Connection c = ds.getConnection();
				PreparedStatement ps = c
						.prepareStatement("SELECT object_id, name, score FROM api_fuzzy_search(?, ?)")) {
			ps.setString(1, text);
			ps.setInt(2, limit);

			try (ResultSet rs = ps.executeQuery()) {
				List<FuzzyRow> out = new ArrayList<>();
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

	public boolean locationExists(UUID locationId) throws SQLException {
		try (Connection c = ds.getConnection();
				PreparedStatement ps = c.prepareStatement("SELECT 1 FROM locations WHERE location_id = ?::uuid")) {
			ps.setObject(1, locationId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

}
