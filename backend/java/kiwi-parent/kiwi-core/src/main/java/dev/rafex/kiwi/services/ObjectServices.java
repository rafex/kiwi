package dev.rafex.kiwi.services;

import java.util.List;
import java.util.UUID;

import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.models.FuzzyItem;
import dev.rafex.kiwi.models.SearchItem;

public interface ObjectServices {

    void create(UUID objectId, String name, String description, String type, String[] tags, String metadataJson, UUID locationId) throws Exception;

    void move(UUID objectId, UUID newLocationId) throws KiwiError;

    List<SearchItem> search(String query, String[] tags, UUID locationId, int limit);

    void updateTags(UUID objectId, String[] tags) throws KiwiError;

    void updateText(UUID objectId, String name, String description) throws KiwiError;

    List<FuzzyItem> fuzzy(String text, int limit) throws Exception;
}
