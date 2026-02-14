package dev.rafex.kiwi.dtos;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FuzzyItemDto(@JsonProperty("object_id") UUID objectId, String name, float score) {

}
