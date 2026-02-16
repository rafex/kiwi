package dev.rafex.kiwi.dtos;

import java.util.List;

import dev.rafex.kiwi.models.SearchItem;

public record SearchResponse(List<SearchItem> items) {

}
