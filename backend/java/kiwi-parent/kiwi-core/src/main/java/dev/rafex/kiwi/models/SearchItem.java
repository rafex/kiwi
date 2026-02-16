package dev.rafex.kiwi.models;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchItem(@JsonProperty("object_id") UUID objectId, String name, double rank) {
}