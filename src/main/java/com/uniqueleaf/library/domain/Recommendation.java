package com.uniqueleaf.library.domain;

public record Recommendation(Book book, String explanation, int score, boolean availableAtPreferredBranch) {
}
