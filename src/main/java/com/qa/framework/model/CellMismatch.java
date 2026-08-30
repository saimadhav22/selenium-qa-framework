package com.qa.framework.model;

/**
 * Describes a single discrepancy discovered during a deep-diff comparison
 * between two {@link TableSnapshot} instances at a given coordinate.
 *
 * @param row          1-based row index of the mismatch
 * @param col          1-based column index of the mismatch
 * @param expectedValue value present in the reference (API) snapshot, or {@code null} if absent
 * @param actualValue   value present in the comparison (UI) snapshot, or {@code null} if absent
 * @param mismatchType  classification of the discrepancy
 */
public record CellMismatch(int row, int col, String expectedValue, String actualValue, MismatchType mismatchType) {

    public enum MismatchType {
        VALUE_DIFFERENCE,
        MISSING_IN_ACTUAL,
        MISSING_IN_EXPECTED
    }

    public String describe() {
        return switch (mismatchType) {
            case VALUE_DIFFERENCE -> "Mismatch at (row=%d, col=%d): expected='%s' actual='%s'"
                    .formatted(row, col, expectedValue, actualValue);
            case MISSING_IN_ACTUAL -> "Missing in UI snapshot at (row=%d, col=%d): expected='%s'"
                    .formatted(row, col, expectedValue);
            case MISSING_IN_EXPECTED -> "Unexpected extra cell in UI snapshot at (row=%d, col=%d): actual='%s'"
                    .formatted(row, col, actualValue);
        };
    }
}