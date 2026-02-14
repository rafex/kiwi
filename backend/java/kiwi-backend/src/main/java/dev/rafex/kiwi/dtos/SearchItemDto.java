package dev.rafex.kiwi.dtos;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchItemDto(@JsonProperty("object_id") UUID objectId, String name, double rank) {
}