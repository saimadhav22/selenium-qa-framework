package com.qa.framework.healing;

import com.qa.framework.model.ElementProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin facade wrapping a {@link WebDriver} instance with self-healing element
 * resolution. Delegates all locate-by-{@link ElementProfile} calls to a
 * {@link SelfHealingElementLocator}, while still exposing the underlying
 * {@link WebDriver} for calls that don't need healing (navigation, JS
 * execution, window/session management, etc.).
 * <p>
 * This class deliberately favors composition over inheriting from
 * {@code WebDriver} directly: re-implementing the full {@code WebDriver}
 * interface surface would violate the Interface Segregation Principle for a
 * framework whose only added value is the self-healing lookup path.
 */
public final class SmartDriver {

    private static final Logger LOGGER = LogManager.getLogger(SmartDriver.class);

    private final WebDriver delegate;
    private final SelfHealingElementLocator selfHealingElementLocator;

    public SmartDriver(WebDriver delegate, Duration perLocatorTimeout) {
        this.delegate = delegate;
        this.selfHealingElementLocator = new SelfHealingElementLocator(delegate, perLocatorTimeout);
    }

    /**
     * Resolves a single element via the self-healing engine, per the supplied
     * {@link ElementProfile}.
     */
    public WebElement findElement(ElementProfile profile) {
        LOGGER.debug("Resolving element '{}' via self-healing lookup.", profile.name());
        return selfHealingElementLocator.locate(profile);
    }

    /**
     * Resolves every matching element for the profile's primary locator without
     * healing (used for cases where the caller expects a collection and treats
     * an empty result as valid, rather than as a failure condition worth
     * self-healing).
     */
    public List<WebElement> findElements(By locator) {
        return delegate.findElements(locator).stream().collect(Collectors.toList());
    }

    public void navigateTo(String url) {
        delegate.get(url);
    }

    public String currentTitle() {
        return delegate.getTitle();
    }

    public String currentUrl() {
        return delegate.getCurrentUrl();
    }

    public WebDriverWait newWait(Duration timeout) {
        return new WebDriverWait(delegate, timeout);
    }

    /**
     * Exposes the underlying raw {@link WebDriver} for operations
     * {@code SmartDriver} does not itself wrap (JavaScript execution, cookie
     * management, window handles, alerts, etc.).
     */
    public WebDriver raw() {
        return delegate;
    }

    public void quit() {
        delegate.quit();
    }
}