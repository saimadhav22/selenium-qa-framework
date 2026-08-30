package com.qa.framework.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object exercising a deliberately brittle, absolute-path XPath locator
 * strategy against the 50x50 grid on {@code the-internet.herokuapp.com/large}.
 * <p>
 * This class exists as the "control" implementation for the Module 1 benchmark —
 * it is intentionally fragile (index-coupled, structure-coupled) and is never
 * intended to be used as a template for production locators. See
 * {@link DynamicLargeDomPage} for the resilient counterpart.
 */
public final class BrittleLargeDomPage {

    private static final Logger LOGGER = LogManager.getLogger(BrittleLargeDomPage.class);

    private final WebDriver driver;
    private final WebDriverWait wait;

    public BrittleLargeDomPage(WebDriver driver, Duration explicitWaitTimeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, explicitWaitTimeout);
    }

    /**
     * Locates a single grid cell using a fully absolute, row/column-index-coupled
     * XPath. Any reordering of table rows, insertion of a wrapper {@code <div>},
     * or a header/footer change will silently break this locator.
     *
     * @param row 1-based row index within {@code <tbody>}
     * @param col 1-based column (td) index within the row
     * @return the located {@link WebElement}
     */
    public WebElement getCellBrittle(int row, int col) {
        // Deliberately brittle: absolute, position-coupled XPath with no
        // descriptive attributes. It is written against the page's single
        // <table> using a "first table on the page" assumption rather than
        // any id/class — precisely the kind of locator that silently breaks
        // the moment an unrelated table is added anywhere earlier in the DOM.
        String xpath = "(//table)[1]/tbody/tr[%d]/td[%d]".formatted(row, col);
        LOGGER.debug("Resolving brittle locator: {}", xpath);
        WebElement cell = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
        return wait.until(ExpectedConditions.visibilityOf(cell));
    }

    /**
     * Locates the table header cell using an equally brittle, position-coupled XPath.
     */
    public WebElement getHeaderCellBrittle(int col) {
        String xpath = "(//table)[1]/thead/tr/th[%d]".formatted(col);
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
    }

    public String getCellTextBrittle(int row, int col) {
        return getCellBrittle(row, col).getText();
    }

    public void open(String baseUrl) {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("(//table)[1]")));
    }
}