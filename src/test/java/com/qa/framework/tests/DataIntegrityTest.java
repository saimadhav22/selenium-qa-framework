package com.qa.framework.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.framework.api.TableApiClient;
import com.qa.framework.config.ConfigManager;
import com.qa.framework.config.DriverFactory;
import com.qa.framework.model.CellMismatch;
import com.qa.framework.model.TableSnapshot;
import com.qa.framework.util.JsonTableExtractor;
import com.qa.framework.util.TableDiffEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Module 2: validates the full 2,500-cell (50x50) grid by extracting the entire
 * UI table in a single {@code executeScript} round trip, retrieving the reference
 * payload via RestAssured, and performing an optimized deep-diff comparison.
 */
public final class DataIntegrityTest {

    private static final Logger LOGGER = LogManager.getLogger(DataIntegrityTest.class);

    private WebDriver driver;
    private ConfigManager config;
    private JsonTableExtractor jsonTableExtractor;
    private TableApiClient tableApiClient;
    private TableDiffEngine diffEngine;

    @BeforeClass
    public void setUp() {
        config = ConfigManager.getInstance();
        driver = DriverFactory.getDriver();

        var objectMapper = new ObjectMapper();
        jsonTableExtractor = new JsonTableExtractor(objectMapper);
        tableApiClient = new TableApiClient(config.apiBaseUri(), config.apiTableEndpoint(), objectMapper);
        diffEngine = new TableDiffEngine();

        driver.get(config.baseUrl());
    }

    @Test
    public void validateFullGridMatchesReferenceData() {
        int rows = config.gridRows();
        int cols = config.gridColumns();

        TableSnapshot uiSnapshot = jsonTableExtractor.extract(driver, rows, cols);
        Assert.assertTrue(uiSnapshot.isComplete(),
                "UI extraction did not yield the full %d-cell grid (got %d cells)."
                        .formatted(uiSnapshot.expectedCellCount(), uiSnapshot.cells().size()));

        TableSnapshot apiSnapshot = fetchReferenceSnapshot(rows, cols);

        List<CellMismatch> mismatches = diffEngine.deepDiff(apiSnapshot, uiSnapshot);
        String report = diffEngine.renderReport(mismatches, apiSnapshot.expectedCellCount());

        System.out.println(report);
        LOGGER.info(report);

        Assert.assertTrue(mismatches.isEmpty(),
                "%d cell mismatch(es) detected between API and UI grids.".formatted(mismatches.size()));
    }

    /**
     * Attempts to retrieve a live reference payload; falls back to a deterministic
     * synthesized snapshot (matching the site's known {@code "row,col"} cell
     * convention) if no reference API is reachable, keeping this test runnable
     * standalone against the public demo site.
     */
    private TableSnapshot fetchReferenceSnapshot(int rows, int cols) {
        try {
            return tableApiClient.fetchExpectedSnapshot(rows, cols);
        } catch (Exception e) {
            LOGGER.warn("Reference API unreachable ({}). Using synthesized reference snapshot instead.",
                    e.getMessage());
            return tableApiClient.synthesizeReferenceSnapshot(rows, cols);
        }
    }

    @AfterClass
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}