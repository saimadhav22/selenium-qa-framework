package com.qa.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Creates {@link WebDriver} instances per the active {@link ConfigManager} settings.
 * <p>
 * Relies on Selenium 4's built-in Selenium Manager for driver binary resolution —
 * no third-party driver-management dependency is required.
 * <p>
 * Each thread receives its own {@link WebDriver} instance via a {@link ThreadLocal},
 * making the factory safe for parallel TestNG execution.
 */
public final class DriverFactory {

    private static final Logger LOGGER = LogManager.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver == null) {
            driver = createDriver();
            DRIVER_THREAD_LOCAL.set(driver);
        }
        return driver;
    }

    private static WebDriver createDriver() {
        var config = ConfigManager.getInstance();
        String browser = config.browser().toLowerCase();

        WebDriver driver = switch (browser) {
            case "firefox" -> createFirefoxDriver(config.headless());
            case "chrome" -> createChromeDriver(config.headless());
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };

        driver.manage().timeouts().implicitlyWait(config.implicitWait());
        driver.manage().window().maximize();
        LOGGER.info("Initialized {} WebDriver instance (headless={})", browser, config.headless());
        return driver;
    }

    private static WebDriver createChromeDriver(boolean headless) {
        var options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*", "--disable-notifications", "--no-sandbox");
        if (headless) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        return new ChromeDriver(options);
    }

    private static WebDriver createFirefoxDriver(boolean headless) {
        var options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        return new FirefoxDriver(options);
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            try {
                driver.quit();
                LOGGER.info("WebDriver session terminated cleanly.");
            } finally {
                DRIVER_THREAD_LOCAL.remove();
            }
        }
    }
}