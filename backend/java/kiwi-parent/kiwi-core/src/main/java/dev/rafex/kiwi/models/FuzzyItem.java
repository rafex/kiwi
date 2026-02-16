package dev.rafex.kiwi.models;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FuzzyItem(@JsonProperty("object_id") UUID objectId, String name, float score) {

}
