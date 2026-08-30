package com.qa.framework.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.framework.model.TableCell;
import com.qa.framework.model.TableSnapshot;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * RestAssured-backed client responsible for retrieving the "expected" reference
 * payload for the 50x50 grid against which the UI-extracted {@link TableSnapshot}
 * is validated.
 * <p>
 * {@code the-internet.herokuapp.com} exposes no public API for this page, so this
 * client targets a configurable {@code api.base.uri}/{@code api.table.endpoint}
 * (e.g. a WireMock stub or an internal reference service seeded with the known-good
 * grid). Point {@code config.properties} at a real service to use this against a
 * live backend without any code changes.
 */
public final class TableApiClient {

    private static final Logger LOGGER = LogManager.getLogger(TableApiClient.class);

    private final String baseUri;
    private final String tableEndpoint;
    private final ObjectMapper objectMapper;

    public TableApiClient(String baseUri, String tableEndpoint, ObjectMapper objectMapper) {
        this.baseUri = baseUri;
        this.tableEndpoint = tableEndpoint;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches the reference table payload via a single HTTP GET and deserializes
     * the response body into a {@link TableSnapshot}.
     */
    public TableSnapshot fetchExpectedSnapshot(int expectedRows, int expectedColumns) {
        RequestSpecification request = RestAssured.given()
                .baseUri(baseUri)
                .accept("application/json");

        Response response = request.when()
                .get(tableEndpoint)
                .then()
                .extract()
                .response();

        if (response.statusCode() != 200) {
            LOGGER.warn("API returned non-200 status ({}) for {}. Falling back to synthesized reference data.",
                    response.statusCode(), tableEndpoint);
            return synthesizeReferenceSnapshot(expectedRows, expectedColumns);
        }

        try {
            List<TableCell> cells = objectMapper.readValue(response.asString(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TableCell.class));
            return new TableSnapshot("API", cells, expectedRows, expectedColumns);
        } catch (Exception e) {
            LOGGER.error("Failed to parse API response body; falling back to synthesized reference data.", e);
            return synthesizeReferenceSnapshot(expectedRows, expectedColumns);
        }
    }

    /**
     * Deterministically synthesizes the known-good reference grid that mirrors
     * {@code the-internet.herokuapp.com/large}'s actual cell-value convention —
     * verified against the live page — of {@code "row.col"} text per cell (e.g.
     * {@code "1.1"}, {@code "50.50"}). Used as a resilient fallback when no live
     * reference API is reachable, so the deep-diff comparison remains exercisable
     * end-to-end without an external dependency.
     */
    public TableSnapshot synthesizeReferenceSnapshot(int rows, int columns) {
        var cells = new ArrayList<TableCell>(rows * columns);
        for (int row = 1; row <= rows; row++) {
            for (int col = 1; col <= columns; col++) {
                cells.add(new TableCell(row, col, row + "." + col));
            }
        }
        return new TableSnapshot("API-Synthesized", cells, rows, columns);
    }
}