package com.qa.framework.model;

import org.openqa.selenium.By;

import java.util.List;
import java.util.Objects;

/**
 * Describes everything the self-healing engine needs to know about a target
 * element: its primary locator, an ordered list of fallback locators to try
 * on failure, and descriptive metadata (dynamic ID pattern, CSS classes, text
 * content, and a structural anchor) that fallback-generation tooling can use
 * to build additional candidate locators at runtime.
 *
 * @param name                 human-readable identifier for logging (e.g. "SubmitButton")
 * @param primaryLocator       the preferred, first-attempted {@link By} locator
 * @param fallbackLocators     ordered fallback {@link By} locators, tried sequentially
 * @param dynamicIdPattern     a regex fragment matching this element's dynamic-id convention, or {@code null}
 * @param cssClasses           known CSS classes on the element, for diagnostic/rebuild purposes
 * @param expectedTextContent  expected visible text content, or {@code null} if not text-bearing
 * @param structuralAnchor     a locator for a stable parent/sibling used to re-derive the element, or {@code null}
 */
public record ElementProfile(
        String name,
        By primaryLocator,
        List<By> fallbackLocators,
        String dynamicIdPattern,
        List<String> cssClasses,
        String expectedTextContent,
        By structuralAnchor
) {

    public ElementProfile {
        Objects.requireNonNull(name, "Element profile name must not be null.");
        Objects.requireNonNull(primaryLocator, "Primary locator must not be null.");
        fallbackLocators = fallbackLocators == null ? List.of() : List.copyOf(fallbackLocators);
        cssClasses = cssClasses == null ? List.of() : List.copyOf(cssClasses);
    }

    /**
     * Convenience builder-style factory for the common case of a primary locator
     * plus one or more fallbacks, with no additional metadata.
     */
    public static ElementProfile of(String name, By primaryLocator, By... fallbacks) {
        return new ElementProfile(name, primaryLocator, List.of(fallbacks), null, List.of(), null, null);
    }
}