package dev.rafex.kiwi.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.rafex.kiwi.db.ObjectRepository;
import dev.rafex.kiwi.errors.KiwiError;

@ExtendWith(MockitoExtension.class)
public class ObjectServicesTest {

    @Mock
    private ObjectRepository repo;

    @InjectMocks
    private ObjectServices services;

    @Test
    void testCreateObject() throws SQLException, KiwiError {
        UUID objectId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        services.create(objectId, "Test Object", "Description", "item", new String[]{"tag1"}, "{}", locationId);

        verify(repo).createObject(eq(objectId), eq("Test Object"), eq("Description"), eq("item"), any(), eq("{}"), eq(locationId));
    }

    @Test
    void testMoveObject() throws SQLException, KiwiError {
        UUID objectId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        services.move(objectId, locationId);

        verify(repo).moveObject(objectId, locationId);
    }
}
