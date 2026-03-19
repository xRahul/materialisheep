package io.github.sheepdestroyer.materialisheep;

import org.junit.Test;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexBenchmarkTest {

    private static final String REGEX_FUZZY_URL = "(.*)((http|https)://[^\\s]*)$";
    private static final Pattern COMPILED_PATTERN = Pattern.compile(REGEX_FUZZY_URL);

    @Test
    public void benchmarkRegex() {
        int iterations = 100000;
        String text = "Check out this link: https://news.ycombinator.com";

        // Warmup
        for (int i = 0; i < 10000; i++) {
            Pattern.compile(REGEX_FUZZY_URL).matcher(text).find();
            COMPILED_PATTERN.matcher(text).find();
        }

        // Benchmark Uncompiled
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Matcher matcher = Pattern.compile(REGEX_FUZZY_URL).matcher(text);
            matcher.find();
        }
        long endTime = System.nanoTime();
        long uncompiledTime = endTime - startTime;
        System.out.println("Uncompiled time: " + uncompiledTime + " ns");

        // Benchmark Compiled
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Matcher matcher = COMPILED_PATTERN.matcher(text);
            matcher.find();
        }
        endTime = System.nanoTime();
        long compiledTime = endTime - startTime;
        System.out.println("Compiled time: " + compiledTime + " ns");

        System.out.println("Improvement: " + ((double)uncompiledTime / compiledTime) + "x");

        try {
            java.io.File file = new java.io.File("regex_benchmark_results.txt");
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write("Uncompiled time: " + uncompiledTime + " ns\n");
            writer.write("Compiled time: " + compiledTime + " ns\n");
            writer.write("Improvement: " + ((double)uncompiledTime / compiledTime) + "x\n");
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
