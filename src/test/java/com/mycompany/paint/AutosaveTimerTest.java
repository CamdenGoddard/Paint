package com.mycompany.paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests for the autosave countdown logic (no clock, no window needed). */
public class AutosaveTimerTest {

    private int fired;

    private AutosaveTimer timer(int seconds) {
        fired = 0;
        return new AutosaveTimer(seconds, () -> fired++);
    }

    @Test
    public void doesNothingUntilStarted() {
        AutosaveTimer timer = timer(10);
        for (int i = 0; i < 30; i++) {
            assertFalse(timer.tick());
        }
        assertEquals(0, fired);
        assertEquals(10, timer.getRemainingSeconds());
    }

    @Test
    public void firesWhenTheCountdownReachesZeroThenRestarts() {
        AutosaveTimer timer = timer(5);
        timer.start();
        for (int i = 0; i < 4; i++) {
            assertFalse(timer.tick());
        }
        assertEquals(1, timer.getRemainingSeconds());
        assertTrue(timer.tick());
        assertEquals(1, fired);
        assertEquals(5, timer.getRemainingSeconds());
        for (int i = 0; i < 5; i++) {
            timer.tick();
        }
        assertEquals(2, fired);
    }

    @Test
    public void resetPutsTheCountdownBackAndDelaysTheSave() {
        AutosaveTimer timer = timer(10);
        timer.start();
        for (int i = 0; i < 8; i++) {
            timer.tick();
        }
        assertEquals(2, timer.getRemainingSeconds());
        timer.reset(); // the user saved by hand
        assertEquals(10, timer.getRemainingSeconds());
        timer.tick();
        timer.tick();
        assertEquals(0, fired);
    }

    @Test
    public void stoppingFreezesTheCountdown() {
        AutosaveTimer timer = timer(10);
        timer.start();
        timer.tick();
        timer.stop();
        timer.tick();
        timer.tick();
        assertEquals(9, timer.getRemainingSeconds());
        assertFalse(timer.isRunning());
    }

    @Test
    public void intervalHasAMinimumAndChangingItRestartsTheCountdown() {
        AutosaveTimer timer = timer(1);
        assertEquals(AutosaveTimer.MIN_SECONDS, timer.getIntervalSeconds());
        timer.start();
        timer.tick();
        timer.setIntervalSeconds(30);
        assertEquals(30, timer.getRemainingSeconds());
    }

    @Test
    public void formatsMinutesAndSeconds() {
        assertEquals("0:00", AutosaveTimer.format(0));
        assertEquals("0:09", AutosaveTimer.format(9));
        assertEquals("1:05", AutosaveTimer.format(65));
        assertEquals("10:00", AutosaveTimer.format(600));
        assertEquals("0:00", AutosaveTimer.format(-3));
    }
}
