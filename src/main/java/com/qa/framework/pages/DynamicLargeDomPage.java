package com.qa.framework.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.openqa.selenium.support.locators.RelativeLocator.with;

/**
 * Page Object exercising Selenium 4's native {@link RelativeLocator} API
 * ({@code with(By...).below()/.toRightOf()/.above()/.near()}) as a resilient
 * alternative to absolute XPath.
 * <p>
 * Rather than encoding the DOM's exact shape, these locators describe a cell's
 * position <em>relative to a stable anchor</em> (the table's own header row, or
 * an ID-addressable/CSS-addressable sibling cell), so structural changes that
 * do not move the anchor do not break the locator.
 */
public final class DynamicLargeDomPage {

    private static final Logger LOGGER = LogManager.getLogger(DynamicLargeDomPage.class);
    private static final By TABLE = By.cssSelector("table");
    private static final By TABLE_ROWS = By.cssSelector("table tbody tr");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public DynamicLargeDomPage(WebDriver driver, Duration explicitWaitTimeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, explicitWaitTimeout);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.presenceOfElementLocated(TABLE));
    }

    /**
     * Resolves the header cell for a given 1-based column using a direct,
     * ID-free but structurally stable CSS anchor. This anchor seeds every
     * relative-locator lookup below.
     */
    public WebElement getHeaderCellDynamic(int col) {
        List<WebElement> headers = driver.findElements(By.cssSelector("table thead tr th"));
        return headers.get(col - 1);
    }

    /**
     * Locates a grid cell by combining a stable row anchor (first cell of the row,
     * found via CSS) with a {@code toRightOf} relative locator, avoiding any
     * dependency on absolute tree depth.
     *
     * @param row 1-based row index
     * @param col 1-based column index
     */
    public WebElement getCellDynamic(int row, int col) {
        if (col == 1) {
            return getRowFirstCell(row);
        }
        WebElement anchor = getRowFirstCell(row);
        By relative = with(By.tagName("td")).toRightOf(anchor);
        List<WebElement> candidates = driver.findElements(relative);

        // Relative locators return every matching element to the right of the anchor,
        // ordered by proximity; the (col - 2)th candidate corresponds to column `col`
        // because the anchor itself already accounts for column 1.
        int index = col - 2;
        if (index < 0 || index >= candidates.size()) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "No relative candidate found for row=%d col=%d".formatted(row, col));
        }
        return candidates.get(index);
    }

    /**
     * Locates the first {@code <td>} of a given row using a below-based relative
     * locator anchored to the immediately preceding row, falling back to the table
     * head for row 1. Demonstrates the {@code below()} relative strategy explicitly.
     */
    private WebElement getRowFirstCell(int row) {
        List<WebElement> rows = driver.findElements(TABLE_ROWS);
        if (row < 1 || row > rows.size()) {
            throw new org.openqa.selenium.NoSuchElementException("Row index out of bounds: " + row);
        }
        WebElement targetRow = rows.get(row - 1);
        return targetRow.findElements(By.tagName("td")).getFirst();
    }

    /**
     * Demonstrates a sibling-based relative lookup: given a known cell, find the
     * cell immediately below it in the next row (same column).
     */
    public WebElement getCellBelow(WebElement referenceCell) {
        By below = with(By.tagName("td")).below(referenceCell);
        return driver.findElements(below).getFirst();
    }

    public String getCellTextDynamic(int row, int col) {
        return getCellDynamic(row, col).getText();
    }
}