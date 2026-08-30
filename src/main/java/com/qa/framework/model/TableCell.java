package com.qa.framework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Strongly-typed representation of a single grid cell, addressed by its
 * 1-based row/column coordinate.
 *
 * @param row   1-based row index
 * @param col   1-based column index
 * @param value the cell's textual content
 */
public record TableCell(
        @JsonProperty("row") int row,
        @JsonProperty("col") int col,
        @JsonProperty("value") String value
) {

    @JsonCreator
    public TableCell {
        if (row < 1 || col < 1) {
            throw new IllegalArgumentException("Row and column indices must be 1-based positive integers.");
        }
    }

    public String coordinateLabel() {
        return "(row=%d, col=%d)".formatted(row, col);
    }
}