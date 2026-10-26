package com.mycompany.paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for the activity log and its background writer thread. */
public class ActionLoggerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    @Test
    public void linesLookLikeDateFileTimeMessage() {
        String line = ActionLogger.formatLine(LocalDateTime.of(2026, 10, 9, 11, 33, 3), "cat.png", "Line tool selected");
        assertEquals("10/09/2026 [cat.png] 11:33:03 Line tool selected", line);
    }

    @Test
    public void everyEventIsOneLineAppendedInOrder() throws IOException {
        Path file = folder.getRoot().toPath().resolve("log.txt");
        try (ActionLogger logger = new ActionLogger(file)) {
            logger.log("new tab", "Program started");
            logger.log("new tab", "Line tool selected");
            logger.log("cat.png", "SaveAs");
        }
        List<String> lines = readLines(file);
        assertEquals(3, lines.size());
        Pattern shape = Pattern.compile("\\d\\d/\\d\\d/\\d{4} \\[[^\\]]+\\] \\d\\d:\\d\\d:\\d\\d .+");
        for (String line : lines) {
            assertTrue(line, shape.matcher(line).matches());
        }
        assertTrue(lines.get(0).contains("[new tab]") && lines.get(0).endsWith("Program started"));
        assertTrue(lines.get(1).endsWith("Line tool selected"));
        assertTrue(lines.get(2).contains("[cat.png]") && lines.get(2).endsWith("SaveAs"));
    }

    @Test
    public void newRunsAppendToTheEndOfAnExistingLog() throws IOException {
        Path file = folder.getRoot().toPath().resolve("log.txt");
        try (ActionLogger first = new ActionLogger(file)) {
            first.log("a.png", "first run");
        }
        try (ActionLogger second = new ActionLogger(file)) {
            second.log("a.png", "second run");
        }
        List<String> lines = readLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).endsWith("first run"));
        assertTrue(lines.get(1).endsWith("second run"));
    }

    @Test
    public void writingHappensOnAnotherThread() throws IOException {
        Path file = folder.getRoot().toPath().resolve("log.txt");
        try (ActionLogger logger = new ActionLogger(file)) {
            assertEquals("paint-logger", logger.getWorkerName());
            assertNotEquals(Thread.currentThread().getName(), logger.getWorkerName());
        }
    }

    @Test
    public void manyThreadsLoggingAtOnceLoseNothingAndKeepEachThreadsOrder() throws Exception {
        Path file = folder.getRoot().toPath().resolve("log.txt");
        int threads = 6;
        int perThread = 200;
        try (ActionLogger logger = new ActionLogger(file)) {
            List<Thread> workers = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int id = t;
                Thread thread = new Thread(() -> {
                    for (int i = 0; i < perThread; i++) {
                        logger.log("tab" + id, "event " + i);
                    }
                });
                workers.add(thread);
                thread.start();
            }
            for (Thread thread : workers) {
                thread.join();
            }
        }
        List<String> lines = readLines(file);
        assertEquals(threads * perThread, lines.size());

        Set<String> seen = new HashSet<>();
        int[] next = new int[threads];
        for (String line : lines) {
            assertTrue("duplicate line: " + line, seen.add(line));
            int id = Integer.parseInt(line.substring(line.indexOf("[tab") + 4, line.indexOf(']')));
            int number = Integer.parseInt(line.substring(line.lastIndexOf(' ') + 1));
            assertEquals("order for thread " + id, next[id], number);
            next[id]++;
        }
    }

    @Test
    public void closeWritesEverythingStillWaitingAndLaterEventsAreIgnored() throws IOException {
        Path file = folder.getRoot().toPath().resolve("log.txt");
        ActionLogger logger = new ActionLogger(file);
        for (int i = 0; i < 1000; i++) {
            logger.log("big.png", "event " + i);
        }
        logger.close();
        logger.close(); // closing twice is harmless
        logger.log("big.png", "too late");

        List<String> lines = readLines(file);
        assertEquals(1000, lines.size());
        assertTrue(lines.get(999).endsWith("event 999"));
        assertFalse(lines.stream().anyMatch(line -> line.contains("too late")));
    }

    @Test
    public void theLogFolderIsCreatedIfMissing() throws IOException {
        Path file = folder.getRoot().toPath().resolve("deep").resolve("er").resolve("log.txt");
        try (ActionLogger logger = new ActionLogger(file)) {
            logger.log("new tab", "hello");
        }
        assertEquals(1, readLines(file).size());
    }
}
