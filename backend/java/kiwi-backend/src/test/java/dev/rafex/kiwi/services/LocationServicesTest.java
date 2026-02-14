package dev.rafex.kiwi.services;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.rafex.kiwi.db.LocationRepository;
import dev.rafex.kiwi.errors.KiwiError;

@ExtendWith(MockitoExtension.class)
public class LocationServicesTest {

    @Mock
    private LocationRepository repo;

    @InjectMocks
    private LocationServices services;

    @Test
    void testCreateLocation() throws SQLException, KiwiError {
        UUID locationId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();

        services.create(locationId, "Test Location", parentId);

        verify(repo).createLocation(eq(locationId), eq("Test Location"), eq(parentId));
    }
}
