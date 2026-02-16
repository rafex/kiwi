package dev.rafex.kiwi.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface ObjectRepository {

    void createObject(UUID objectId, String name, String description, String type, String[] tags, String metadataJson, UUID locationId) throws SQLException;

    void moveObject(UUID objectId, UUID newLocationId) throws SQLException;

    void updateTags(UUID objectId, String[] tags) throws SQLException;

    void updateText(UUID objectId, String name, String description) throws SQLException;

    void updateMetadata(UUID objectId, String metadataJson) throws SQLException;

    List<SearchRow> search(String query, String[] tags, UUID locationId, int limit) throws SQLException;

    List<FuzzyRow> fuzzy(String text, int limit) throws SQLException;

    record SearchRow(UUID objectId, String name, float rank) {
    }

    record FuzzyRow(UUID objectId, String name, float score) {
    }

}