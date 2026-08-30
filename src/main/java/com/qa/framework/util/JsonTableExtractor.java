package com.qa.framework.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.framework.model.TableCell;
import com.qa.framework.model.TableSnapshot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Extracts the entire large-DOM table in a single {@link JavascriptExecutor#executeScript}
 * round trip, completely bypassing {@code driver.findElements()} plus a client-side
 * iteration/{@code getText()} loop — which would otherwise require one Selenium wire
 * command per row (or per cell, depending on implementation).
 * <p>
 * The injected script walks {@code table.rows} natively in the browser and returns a
 * single JSON string, which is then deserialized into strongly-typed {@link TableCell}
 * records via Jackson.
 */
public final class JsonTableExtractor {

    private static final Logger LOGGER = LogManager.getLogger(JsonTableExtractor.class);

    private static final String EXTRACTION_SCRIPT = """
            const table = document.querySelector('table');
            if (!table) { return '[]'; }
            const cells = [];
            const rows = table.querySelectorAll('tbody tr');
            rows.forEach((row, rowIndex) => {
                const tds = row.querySelectorAll('td');
                tds.forEach((td, colIndex) => {
                    cells.push({
                        row: rowIndex + 1,
                        col: colIndex + 1,
                        value: td.textContent.trim()
                    });
                });
            });
            return JSON.stringify(cells);
            """;

    private final ObjectMapper objectMapper;

    public JsonTableExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the extraction script and deserializes the result into a
     * {@link TableSnapshot} in a single browser round trip.
     */
    public TableSnapshot extract(WebDriver driver, int expectedRows, int expectedColumns) {
        if (!(driver instanceof JavascriptExecutor jsExecutor)) {
            throw new IllegalArgumentException("Driver instance does not support JavaScript execution.");
        }

        long start = System.nanoTime();
        Object rawResult = jsExecutor.executeScript(EXTRACTION_SCRIPT);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        LOGGER.info("Single executeScript table extraction completed in {} ms", elapsedMillis);

        String json = String.valueOf(rawResult);
        List<TableCell> cells;
        try {
            cells = objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, TableCell.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize UI table extraction JSON payload.", e);
        }

        var snapshot = new TableSnapshot("UI", cells, expectedRows, expectedColumns);
        if (!snapshot.isComplete()) {
            LOGGER.warn("UI extraction returned {} cells; expected {}.",
                    cells.size(), snapshot.expectedCellCount());
        }
        return snapshot;
    }
}