package dev.rafex.kiwi.dtos;

import java.util.List;

import dev.rafex.kiwi.models.FuzzyItem;

public record FuzzyResponse(List<FuzzyItem> items) {

}
