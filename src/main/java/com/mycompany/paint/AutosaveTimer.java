package com.mycompany.paint;

/**
 * The countdown behind autosave. It only does the counting: something else
 * (a JavaFX timeline in {@link PaintApp}) calls {@link #tick()} once a
 * second, and when the countdown hits zero the saving callback runs and the
 * countdown starts over.
 *
 * <p>Keeping the counting separate from the clock and from the saving means
 * the logic can be unit tested without waiting around or opening a window.</p>
 */
final class AutosaveTimer {

    /** Shortest interval allowed, in seconds. */
    static final int MIN_SECONDS = 5;

    private final Runnable onFire;
    private int intervalSeconds;
    private int remainingSeconds;
    private boolean running;

    /**
     * @param intervalSeconds seconds between autosaves (raised to {@link #MIN_SECONDS} if smaller)
     * @param onFire          what to do when the countdown reaches zero
     */
    AutosaveTimer(int intervalSeconds, Runnable onFire) {
        this.onFire = onFire;
        setIntervalSeconds(intervalSeconds);
    }

    /** Starts (or restarts) the countdown from the full interval. */
    void start() {
        running = true;
        remainingSeconds = intervalSeconds;
    }

    /** Stops counting. The remaining time is kept but nothing fires. */
    void stop() {
        running = false;
    }

    /** Puts the countdown back to the full interval, e.g. after the user saves by hand. */
    void reset() {
        remainingSeconds = intervalSeconds;
    }

    /**
     * Changes the interval. If the timer is running, the countdown starts
     * over from the new interval so the display never shows a stale number.
     *
     * @param seconds the new interval (raised to {@link #MIN_SECONDS} if smaller)
     */
    void setIntervalSeconds(int seconds) {
        intervalSeconds = Math.max(MIN_SECONDS, seconds);
        remainingSeconds = intervalSeconds;
    }

    /**
     * Counts down one second. Does nothing while stopped. When the count
     * reaches zero, runs the callback and restarts the countdown.
     *
     * @return true if the callback ran on this tick
     */
    boolean tick() {
        if (!running) {
            return false;
        }
        remainingSeconds--;
        if (remainingSeconds <= 0) {
            remainingSeconds = intervalSeconds;
            onFire.run();
            return true;
        }
        return false;
    }

    /** @return seconds left until the next autosave */
    int getRemainingSeconds() {
        return remainingSeconds;
    }

    /** @return the full interval in seconds */
    int getIntervalSeconds() {
        return intervalSeconds;
    }

    /** @return true while the countdown is active */
    boolean isRunning() {
        return running;
    }

    /**
     * Formats a number of seconds as m:ss for the on-screen countdown.
     *
     * @param seconds a non-negative number of seconds
     * @return e.g. "1:05" for 65
     */
    static String format(int seconds) {
        int safe = Math.max(0, seconds);
        return (safe / 60) + ":" + String.format("%02d", safe % 60);
    }
}
