package com.qa.framework.util;

import java.util.Arrays;
import java.util.List;

/**
 * Immutable statistical summary (mean, median, standard deviation, min, max) over
 * a series of timing samples expressed in nanoseconds.
 *
 * @param sampleCount       number of samples used to derive this summary
 * @param meanMillis        arithmetic mean, in milliseconds
 * @param medianMillis      median, in milliseconds
 * @param stdDeviationMillis population standard deviation, in milliseconds
 * @param minMillis         fastest sample, in milliseconds
 * @param maxMillis         slowest sample, in milliseconds
 */
public record BenchmarkStats(
        int sampleCount,
        double meanMillis,
        double medianMillis,
        double stdDeviationMillis,
        double minMillis,
        double maxMillis
) {

    public static BenchmarkStats fromNanoSamples(List<Long> nanoSamples) {
        if (nanoSamples == null || nanoSamples.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute statistics over an empty sample set.");
        }

        double[] millis = nanoSamples.stream()
                .mapToDouble(nanos -> nanos / 1_000_000.0)
                .sorted()
                .toArray();

        double mean = Arrays.stream(millis).average().orElseThrow();
        double median = computeMedian(millis);
        double stdDev = computeStdDeviation(millis, mean);
        double min = millis[0];
        double max = millis[millis.length - 1];

        return new BenchmarkStats(millis.length, mean, median, stdDev, min, max);
    }

    private static double computeMedian(double[] sortedMillis) {
        int n = sortedMillis.length;
        int mid = n / 2;
        return (n % 2 == 0)
                ? (sortedMillis[mid - 1] + sortedMillis[mid]) / 2.0
                : sortedMillis[mid];
    }

    private static double computeStdDeviation(double[] millis, double mean) {
        double sumSquaredDiffs = Arrays.stream(millis)
                .map(value -> Math.pow(value - mean, 2))
                .sum();
        return Math.sqrt(sumSquaredDiffs / millis.length);
    }

    /**
     * Renders a fixed-width console table row for this statistic set, labeled by
     * the given strategy name.
     */
    public String toReportRow(String strategyLabel) {
        return """
                | %-20s | %8d | %10.3f | %10.3f | %10.3f | %10.3f | %10.3f |""".formatted(
                strategyLabel, sampleCount, meanMillis, medianMillis, stdDeviationMillis, minMillis, maxMillis);
    }

    public static String reportHeader() {
        return """
                | %-20s | %8s | %10s | %10s | %10s | %10s | %10s |
                %s""".formatted(
                "Strategy", "Samples", "Mean(ms)", "Median(ms)", "StdDev(ms)", "Min(ms)", "Max(ms)",
                "-".repeat(100));
    }
}