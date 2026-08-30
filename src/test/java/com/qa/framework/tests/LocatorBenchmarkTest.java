package com.qa.framework.tests;

import com.qa.framework.config.ConfigManager;
import com.qa.framework.config.DriverFactory;
import com.qa.framework.pages.BrittleLargeDomPage;
import com.qa.framework.pages.DynamicLargeDomPage;
import com.qa.framework.util.BenchmarkStats;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Module 1 benchmark suite: executes {@code benchmark.iterations} lookups of the
 * same target cell using the brittle absolute-XPath strategy versus the Selenium 4
 * relative-locator strategy, then reports mean/median/standard-deviation timings
 * for each.
 */
public final class LocatorBenchmarkTest {

    private static final Logger LOGGER = LogManager.getLogger(LocatorBenchmarkTest.class);

    private WebDriver driver;
    private BrittleLargeDomPage brittlePage;
    private DynamicLargeDomPage dynamicPage;
    private ConfigManager config;

    @BeforeClass
    public void setUp() {
        config = ConfigManager.getInstance();
        driver = DriverFactory.getDriver();
        brittlePage = new BrittleLargeDomPage(driver, config.explicitWait());
        dynamicPage = new DynamicLargeDomPage(driver, config.explicitWait());
    }

    @Test
    public void benchmarkBrittleVersusRelativeLocatorPerformance() {
        int iterations = config.benchmarkIterations();
        int targetRow = config.gridRows() / 2;
        int targetCol = config.gridColumns() / 2;

        brittlePage.open(config.baseUrl());
        List<Long> brittleSamples = timeIterations(iterations,
                () -> brittlePage.getCellBrittle(targetRow, targetCol));

        dynamicPage.open(config.baseUrl());
        List<Long> dynamicSamples = timeIterations(iterations,
                () -> dynamicPage.getCellDynamic(targetRow, targetCol));

        BenchmarkStats brittleStats = BenchmarkStats.fromNanoSamples(brittleSamples);
        BenchmarkStats dynamicStats = BenchmarkStats.fromNanoSamples(dynamicSamples);

        printReport(brittleStats, dynamicStats);
    }

    private List<Long> timeIterations(int iterations, Runnable lookup) {
        var samples = new ArrayList<Long>(iterations);
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            lookup.run();
            long elapsed = System.nanoTime() - start;
            samples.add(elapsed);
        }
        return samples;
    }

    private void printReport(BenchmarkStats brittleStats, BenchmarkStats dynamicStats) {
        String report = """

                =================== LOCATOR STRATEGY BENCHMARK REPORT ===================
                %s
                %s
                %s
                ===========================================================================
                """.formatted(
                BenchmarkStats.reportHeader(),
                brittleStats.toReportRow("Brittle XPath"),
                dynamicStats.toReportRow("Relative Locator")
        );

        System.out.println(report);
        LOGGER.info(report);
    }

    @AfterClass
    public void tearDown() {
        DriverFactory.quitDriver();
    }
}