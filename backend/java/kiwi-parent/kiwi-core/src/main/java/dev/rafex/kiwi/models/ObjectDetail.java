package dev.rafex.kiwi.models;

import java.time.Instant;
import java.util.UUID;

public record ObjectDetail(UUID objectId, String name, String description, String type, String status,
        UUID currentLocationId, String[] tags, String metadataJson, Instant createdAt, Instant updatedAt) {
}