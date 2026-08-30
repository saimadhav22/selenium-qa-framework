package com.qa.framework.model;

import java.util.List;

/**
 * Immutable container for a full table extraction — either from the UI (via a
 * single {@code executeScript} call) or from the API — expressed as a flat list
 * of {@link TableCell} records.
 *
 * @param source     descriptive origin of this snapshot, e.g. {@code "UI"} or {@code "API"}
 * @param cells      the flattened set of extracted cells
 * @param rowCount   number of rows represented
 * @param columnCount number of columns represented
 */
public record TableSnapshot(String source, List<TableCell> cells, int rowCount, int columnCount) {

    public int expectedCellCount() {
        return rowCount * columnCount;
    }

    public boolean isComplete() {
        return cells.size() == expectedCellCount();
    }
}