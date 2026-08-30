package com.qa.framework.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Thread-safe, lazily initialized singleton responsible for exposing strongly-typed
 * access to the framework's externalized configuration ({@code config.properties}).
 * <p>
 * Centralizing configuration access here keeps environment concerns (browser choice,
 * timeouts, target URLs) out of page objects and test classes, in line with the
 * Single Responsibility Principle.
 */
public final class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.properties";
    private static volatile ConfigManager instance;

    private final Properties properties;

    private ConfigManager() {
        this.properties = loadProperties();
    }

    public static ConfigManager getInstance() {
        ConfigManager result = instance;
        if (result == null) {
            synchronized (ConfigManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new ConfigManager();
                }
            }
        }
        return result;
    }

    private Properties loadProperties() {
        var props = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Unable to locate " + CONFIG_FILE + " on the classpath.");
            }
            props.load(inputStream);
            LOGGER.info("Configuration loaded successfully from {}", CONFIG_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load configuration file: " + CONFIG_FILE, e);
        }
        return props;
    }

    private String require(String key) {
        String value = properties.getProperty(key);
        Objects.requireNonNull(value, "Missing required configuration key: " + key);
        // Defensively trim: stray trailing/leading whitespace introduced by manual
        // editing or copy-paste into config.properties should never break numeric
        // or boolean parsing downstream.
        return value.trim();
    }

    public String browser() {
        return require("browser");
    }

    public boolean headless() {
        return Boolean.parseBoolean(require("headless"));
    }

    public String baseUrl() {
        return require("base.url");
    }

    public Duration implicitWait() {
        return Duration.ofSeconds(Long.parseLong(require("implicit.wait.seconds")));
    }

    public Duration explicitWait() {
        return Duration.ofSeconds(Long.parseLong(require("explicit.wait.seconds")));
    }

    public int benchmarkIterations() {
        return Integer.parseInt(require("benchmark.iterations"));
    }

    public String apiBaseUri() {
        return require("api.base.uri");
    }

    public String apiTableEndpoint() {
        return require("api.table.endpoint");
    }

    public int gridRows() {
        return Integer.parseInt(require("grid.rows"));
    }

    public int gridColumns() {
        return Integer.parseInt(require("grid.columns"));
    }
}