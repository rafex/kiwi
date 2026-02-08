package dev.rafex.kiwi.db;

import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import javax.sql.DataSource;

public class LocationRepository {
	private final DataSource ds;

	public LocationRepository(final DataSource ds) {
		this.ds = ds;
	}

	public void createLocation(final UUID locationId, final String name, final UUID parentLocationId) throws SQLException {
		try (var c = ds.getConnection();
				var ps = c.prepareStatement("SELECT api_create_location(?::uuid, ?, ?::uuid)")) {

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
}
