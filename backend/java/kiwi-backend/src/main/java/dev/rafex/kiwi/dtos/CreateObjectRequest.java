package dev.rafex.kiwi.dtos;

import java.util.List;

public record CreateObjectRequest(String name, String description, String type, List<String> tags, Object metadata, String locationId) {
}
