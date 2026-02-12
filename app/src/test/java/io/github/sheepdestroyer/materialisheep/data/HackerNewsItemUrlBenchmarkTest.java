package io.github.sheepdestroyer.materialisheep.data;

import org.junit.Test;
import java.util.Random;

public class HackerNewsItemUrlBenchmarkTest {

    private static final String BASE_WEB_URL = "https://news.ycombinator.com";
    private static final String WEB_ITEM_PATH = BASE_WEB_URL + "/item?id=%s";
    private static final String WEB_ITEM_PATH_PREFIX = BASE_WEB_URL + "/item?id=";

    @Test
    public void benchmarkUrlConstruction() {
        int iterations = 1000000;
        String[] ids = new String[1000];
        Random random = new Random();
        for (int i = 0; i < ids.length; i++) {
            ids[i] = String.valueOf(random.nextInt(1000000));
        }

        // Warmup
        for (int i = 0; i < 10000; i++) {
            String id = ids[i % ids.length];
            String.format(WEB_ITEM_PATH, id);
            String s = WEB_ITEM_PATH_PREFIX + id;
        }

        // Benchmark String.format
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String id = ids[i % ids.length];
            String s = String.format(WEB_ITEM_PATH, id);
        }
        long endTime = System.nanoTime();
        long formatTime = endTime - startTime;
        System.out.println("String.format time: " + formatTime + " ns");

        // Benchmark Concatenation
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String id = ids[i % ids.length];
            String s = WEB_ITEM_PATH_PREFIX + id;
        }
        endTime = System.nanoTime();
        long concatTime = endTime - startTime;
        System.out.println("Concatenation time: " + concatTime + " ns");

        System.out.println("Improvement: " + ((double)formatTime / concatTime) + "x");

        try {
            java.io.File file = new java.io.File("benchmark_results.txt");
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write("String.format time: " + formatTime + " ns\n");
            writer.write("Concatenation time: " + concatTime + " ns\n");
            writer.write("Improvement: " + ((double)formatTime / concatTime) + "x\n");
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
