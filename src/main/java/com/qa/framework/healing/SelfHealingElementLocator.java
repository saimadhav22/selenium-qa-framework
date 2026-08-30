package com.qa.framework.healing;

import com.qa.framework.model.ElementProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Core self-healing lookup engine.
 * <p>
 * Given an {@link ElementProfile}, this class first attempts the profile's
 * primary locator under an explicit {@link WebDriverWait}. If that attempt
 * fails with {@link NoSuchElementException}, {@link TimeoutException}, or
 * {@link StaleElementReferenceException}, it sequentially evaluates each
 * fallback locator (and, if present, a structural-anchor-relative lookup)
 * until one succeeds.
 * <p>
 * A successful heal is logged with the exact fallback that resolved the
 * element, but never fails the test — the calling test continues to
 * completion using whichever element was ultimately found. Only when every
 * candidate locator is exhausted does this class raise an exception.
 */
public final class SelfHealingElementLocator {

    private static final Logger LOGGER = LogManager.getLogger(SelfHealingElementLocator.class);

    private final WebDriver driver;
    private final Duration perLocatorTimeout;

    public SelfHealingElementLocator(WebDriver driver, Duration perLocatorTimeout) {
        this.driver = driver;
        this.perLocatorTimeout = perLocatorTimeout;
    }

    /**
     * Resolves the element described by {@code profile}, healing through
     * fallback locators as needed.
     *
     * @throws NoSuchElementException if the primary locator and every
     *                                 configured fallback all fail to resolve an element
     */
    public WebElement locate(ElementProfile profile) {
        HealAttempt primaryAttempt = tryLocator("primary", profile.primaryLocator());
        if (primaryAttempt.element() != null) {
            return primaryAttempt.element();
        }

        LOGGER.warn("Primary locator failed for element '{}' ({}): {}. Evaluating {} fallback candidate(s)...",
                profile.name(), profile.primaryLocator(), primaryAttempt.failureReason(),
                profile.fallbackLocators().size() + (profile.structuralAnchor() != null ? 1 : 0));

        List<By> candidates = new ArrayList<>(profile.fallbackLocators());

        for (int i = 0; i < candidates.size(); i++) {
            By fallback = candidates.get(i);
            HealAttempt attempt = tryLocator("fallback[" + i + "]", fallback);
            if (attempt.element() != null) {
                logSuccessfulHeal(profile, fallback, i, attempt);
                return attempt.element();
            }
        }

        if (profile.structuralAnchor() != null) {
            HealAttempt anchorAttempt = tryAnchorRelativeLookup(profile);
            if (anchorAttempt.element() != null) {
                logSuccessfulHeal(profile, profile.structuralAnchor(), -1, anchorAttempt);
                return anchorAttempt.element();
            }
        }

        String message = "Self-healing exhausted all candidates for element '%s'. Primary: %s. Fallbacks tried: %d."
                .formatted(profile.name(), profile.primaryLocator(), candidates.size());
        LOGGER.error(message);
        throw new NoSuchElementException(message);
    }

    private HealAttempt tryAnchorRelativeLookup(ElementProfile profile) {
        try {
            WebElement anchor = new WebDriverWait(driver, perLocatorTimeout)
                    .until(ExpectedConditions.presenceOfElementLocated(profile.structuralAnchor()));
            List<WebElement> children = anchor.findElements(By.xpath(".//*"));
            for (WebElement candidate : children) {
                if (matchesExpectedText(profile, candidate) || matchesExpectedCssClass(profile, candidate)) {
                    return new HealAttempt(candidate, null);
                }
            }
            return new HealAttempt(null, "no descendant of structural anchor matched profile metadata");
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException e) {
            return new HealAttempt(null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private boolean matchesExpectedText(ElementProfile profile, WebElement candidate) {
        if (profile.expectedTextContent() == null) {
            return false;
        }
        try {
            return profile.expectedTextContent().equals(candidate.getText());
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private boolean matchesExpectedCssClass(ElementProfile profile, WebElement candidate) {
        if (profile.cssClasses().isEmpty()) {
            return false;
        }
        try {
            String classAttribute = candidate.getAttribute("class");
            if (classAttribute == null) {
                return false;
            }
            return profile.cssClasses().stream().anyMatch(classAttribute::contains);
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private HealAttempt tryLocator(String label, By locator) {
        try {
            WebElement element = new WebDriverWait(driver, perLocatorTimeout)
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
            return new HealAttempt(element, null);
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException e) {
            LOGGER.debug("Locator attempt '{}' ({}) failed: {}", label, locator, e.getMessage());
            return new HealAttempt(null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void logSuccessfulHeal(ElementProfile profile, By successfulLocator, int fallbackIndex, HealAttempt attempt) {
        String candidateLabel = fallbackIndex >= 0
                ? "fallbackLocators[%d]".formatted(fallbackIndex)
                : "structuralAnchor-derived candidate";

        LOGGER.info("""
                SELF-HEAL SUCCESS | element='{}' | primaryLocator='{}' | healedVia='{}' \
                | resolvedLocator='{}' | dynamicIdPattern='{}'""",
                profile.name(), profile.primaryLocator(), candidateLabel, successfulLocator,
                profile.dynamicIdPattern());
    }

    /**
     * Internal result wrapper for a single locator attempt.
     */
    private record HealAttempt(WebElement element, String failureReason) {
    }
}