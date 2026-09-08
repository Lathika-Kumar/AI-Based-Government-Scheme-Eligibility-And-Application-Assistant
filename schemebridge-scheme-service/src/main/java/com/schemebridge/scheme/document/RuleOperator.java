package com.schemebridge.scheme.document;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RuleOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    BETWEEN,
    IN,
    NOT_IN,
    CONTAINS,
    BOOLEAN_TRUE,
    BOOLEAN_FALSE,

    // Aliases
    EQ,
    NEQ,
    GT,
    GTE,
    LT,
    LTE,
    EQUALS_ANY;

    @JsonCreator
    public static RuleOperator fromString(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "EQ", "EQUALS", "==" -> EQUALS;
            case "NEQ", "NOT_EQUALS", "!=" -> NOT_EQUALS;
            case "GT", "GREATER_THAN", ">" -> GREATER_THAN;
            case "GTE", "GREATER_THAN_OR_EQUAL", ">=" -> GREATER_THAN_OR_EQUAL;
            case "LT", "LESS_THAN", "<" -> LESS_THAN;
            case "LTE", "LESS_THAN_OR_EQUAL", "<=" -> LESS_THAN_OR_EQUAL;
            case "BETWEEN" -> BETWEEN;
            case "IN", "EQUALS_ANY" -> IN;
            case "NOT_IN", "NIN" -> NOT_IN;
            case "CONTAINS" -> CONTAINS;
            case "BOOLEAN_TRUE", "TRUE" -> BOOLEAN_TRUE;
            case "BOOLEAN_FALSE", "FALSE" -> BOOLEAN_FALSE;
            default -> {
                try {
                    yield RuleOperator.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }
}
