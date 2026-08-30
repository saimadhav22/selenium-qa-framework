package com.qa.framework.util;

import com.qa.framework.model.CellMismatch;
import com.qa.framework.model.TableCell;
import com.qa.framework.model.TableSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs an optimized deep-diff comparison between an "expected" (API) and an
 * "actual" (UI) {@link TableSnapshot}.
 * <p>
 * Rather than nested-loop comparison (O(n²)), both snapshots are first indexed
 * into {@code (row, col) -> value} maps, giving overall O(n) comparison cost for
 * the full 2,500-cell grid.
 */
public final class TableDiffEngine {

    public List<CellMismatch> deepDiff(TableSnapshot expected, TableSnapshot actual) {
        Map<Long, String> expectedIndex = indexByCoordinate(expected.cells());
        Map<Long, String> actualIndex = indexByCoordinate(actual.cells());

        var mismatches = new ArrayList<CellMismatch>();

        for (Map.Entry<Long, String> expectedEntry : expectedIndex.entrySet()) {
            long key = expectedEntry.getKey();
            String expectedValue = expectedEntry.getValue();
            String actualValue = actualIndex.get(key);

            int row = extractRow(key);
            int col = extractCol(key);

            if (actualValue == null) {
                mismatches.add(new CellMismatch(row, col, expectedValue, null,
                        CellMismatch.MismatchType.MISSING_IN_ACTUAL));
            } else if (!expectedValue.equals(actualValue)) {
                mismatches.add(new CellMismatch(row, col, expectedValue, actualValue,
                        CellMismatch.MismatchType.VALUE_DIFFERENCE));
            }
        }

        for (Map.Entry<Long, String> actualEntry : actualIndex.entrySet()) {
            long key = actualEntry.getKey();
            if (!expectedIndex.containsKey(key)) {
                mismatches.add(new CellMismatch(extractRow(key), extractCol(key), null, actualEntry.getValue(),
                        CellMismatch.MismatchType.MISSING_IN_EXPECTED));
            }
        }

        mismatches.sort((a, b) -> a.row() != b.row() ? Integer.compare(a.row(), b.row())
                : Integer.compare(a.col(), b.col()));

        return mismatches;
    }

    /**
     * Packs a (row, col) coordinate pair into a single {@code long} key for
     * constant-time hash lookups without allocating a wrapper coordinate object
     * per cell.
     */
    private long coordinateKey(int row, int col) {
        return (((long) row) << 32) | (col & 0xFFFFFFFFL);
    }

    private int extractRow(long key) {
        return (int) (key >> 32);
    }

    private int extractCol(long key) {
        return (int) key;
    }

    private Map<Long, String> indexByCoordinate(List<TableCell> cells) {
        Map<Long, String> index = new HashMap<>(cells.size() * 2);
        for (TableCell cell : cells) {
            index.put(coordinateKey(cell.row(), cell.col()), cell.value());
        }
        return index;
    }

    public String renderReport(List<CellMismatch> mismatches, int totalExpectedCells) {
        if (mismatches.isEmpty()) {
            return """
                    Data integrity check PASSED: all %d cells matched between API and UI.""".formatted(totalExpectedCells);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Data integrity check FAILED: %d/%d cells mismatched.%n"
                .formatted(mismatches.size(), totalExpectedCells));
        for (CellMismatch mismatch : mismatches) {
            builder.append("  - ").append(mismatch.describe()).append(System.lineSeparator());
        }
        return builder.toString();
    }
}