package com.mycompany.paint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Keeps a text log of what the user does, one event per line:
 *
 * <pre>10/26/2026 [drawing.png] 11:33:03 Line tool selected</pre>
 *
 * <p>Writing to a file can be slow, and the screen must never stutter
 * while someone is drawing, so the file work happens on its own background
 * thread. {@link #log} only stamps the time, drops the line in a queue and
 * returns; the logger thread takes lines off the queue in order and appends
 * them to the file. Call {@link #close()} when the program ends so the last
 * lines are written out.</p>
 */
final class ActionLogger implements AutoCloseable {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Put on the queue to tell the logger thread to finish. Compared by identity. */
    private static final String STOP = new String("<stop>");

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final Path file;
    private final Thread worker;
    private volatile boolean closed;

    /**
     * Opens (creating if needed) the log file for appending and starts the
     * logger thread.
     *
     * @param file the text file to append to; its folder is created if missing
     * @throws IOException if the folder or file cannot be created
     */
    ActionLogger(Path file) throws IOException {
        this.file = file;
        Path folder = file.toAbsolutePath().getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
        worker = new Thread(this::writeLoop, "paint-logger");
        worker.setDaemon(true); // never keeps the program alive on its own
        worker.start();
    }

    /**
     * Builds one log line.
     *
     * @param when     the time of the event
     * @param fileName the picture's file name, or "new tab" for one never saved
     * @param message  what happened
     * @return e.g. {@code 10/26/2026 [cat.png] 11:33:03 Line tool selected}
     */
    static String formatLine(LocalDateTime when, String fileName, String message) {
        return when.format(DATE) + " [" + fileName + "] " + when.format(TIME) + " " + message;
    }

    /**
     * Records an event. Returns right away; the line is written later by
     * the logger thread. Does nothing after {@link #close()}.
     *
     * @param fileName the picture's file name, or "new tab" for one never saved
     * @param message  what happened
     */
    void log(String fileName, String message) {
        if (closed) {
            return;
        }
        queue.add(formatLine(LocalDateTime.now(), fileName, message));
    }

    /** @return the log file being written */
    Path getFile() {
        return file;
    }

    /** @return the name of the thread that does the writing (never the caller's thread) */
    String getWorkerName() {
        return worker.getName();
    }

    /**
     * Writes out everything still waiting, then stops the logger thread.
     * Safe to call more than once. Waits at most a couple of seconds.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        queue.add(STOP);
        try {
            worker.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** The logger thread: take lines off the queue and append them to the file. */
    private void writeLoop() {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            while (true) {
                String line = queue.take();
                if (line == STOP) {
                    break;
                }
                out.write(line);
                out.newLine();
                if (queue.isEmpty()) {
                    out.flush(); // nothing else waiting, so make the lines visible now
                }
            }
        } catch (IOException e) {
            System.err.println("Logging stopped: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
