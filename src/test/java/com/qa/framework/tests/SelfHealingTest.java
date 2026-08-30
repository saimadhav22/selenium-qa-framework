package com.qa.framework.tests;

import com.qa.framework.config.ConfigManager;
import com.qa.framework.config.DriverFactory;
import com.qa.framework.healing.SmartDriver;
import com.qa.framework.model.ElementProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Module 3: demonstrates the self-healing engine recovering from an
 * intentionally broken primary locator via sequential fallback evaluation,
 * without ever failing the test.
 */
public final class SelfHealingTest {

    private static final Logger LOGGER = LogManager.getLogger(SelfHealingTest.class);

    private SmartDriver smartDriver;
    private ConfigManager config;

    @BeforeClass
    public void setUp() {
        config = ConfigManager.getInstance();
        smartDriver = new SmartDriver(DriverFactory.getDriver(), config.explicitWait());
        smartDriver.navigateTo(config.baseUrl());
    }

    @Test
    public void healsThroughFallbackWhenPrimaryLocatorIsBroken() {
        // Primary locator is deliberately wrong (references an id that does not
        // exist on the page), simulating a real-world DOM change that would
        // otherwise break this test outright.
        ElementProfile heading = new ElementProfile(
                "PageHeading",
                By.id("this-id-does-not-exist-on-the-page"),
                java.util.List.of(
                        By.cssSelector("h3"),
                        By.xpath("//h3")
                ),
                null,
                java.util.List.of(),
                null,
                By.cssSelector("div.example")
        );

        WebElement resolvedElement = smartDriver.findElement(heading);

        Assert.assertNotNull(resolvedElement, "Self-healing engine failed to resolve any candidate element.");
        Assert.assertTrue(resolvedElement.isDisplayed(), "Resolved element is not visible on the page.");

        LOGGER.info("Test completed successfully; primary locator was broken but self-healing recovered " +
                "the target element without failing the run.");
    }

    @Test
    public void healsThroughSecondFallbackWhenFirstFallbackAlsoFails() {
        // The primary locator AND the first fallback are both deliberately invalid.
        // Only the second fallback candidate — a tag-name locator for the page's
        // single <table> element — will actually resolve, exercising the engine's
        // sequential (not just single-fallback) evaluation order.
        ElementProfile tableElement = new ElementProfile(
                "LargeGridTable",
                By.id("nonexistent-primary"),
                java.util.List.of(
                        By.cssSelector(".still-nonexistent-fallback-class"),
                        By.tagName("table")
                ),
                null,
                java.util.List.of(),
                null,
                null
        );

        WebElement resolvedElement = smartDriver.findElement(tableElement);
        Assert.assertNotNull(resolvedElement,
                "Self-healing engine failed to resolve via its second fallback candidate.");
        Assert.assertEquals(resolvedElement.getTagName().toLowerCase(), "table",
                "Resolved element was not the expected <table>.");
    }

    @AfterClass
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}