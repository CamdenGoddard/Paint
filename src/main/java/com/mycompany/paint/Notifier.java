package com.mycompany.paint;

import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

/**
 * Shows small pop-up notifications from the operating system (the corner of
 * the screen on Windows), used by autosave.
 *
 * <p>It uses the system tray, so a small icon sits in the tray while the
 * program runs. If the computer or desktop does not support that, every
 * call quietly does nothing and {@link #isAvailable()} says so, so the
 * program still works.</p>
 */
final class Notifier {

    private TrayIcon trayIcon;
    private boolean triedToStart;

    /**
     * Shows a notification. Safe to call from any thread.
     *
     * @param title   the bold first line
     * @param message the text under it
     * @return true if a notification was handed to the system
     */
    synchronized boolean show(String title, String message) {
        if (!start()) {
            return false;
        }
        trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        return true;
    }

    /** @return true if notifications can be shown on this computer */
    synchronized boolean isAvailable() {
        return start();
    }

    /** Removes the tray icon. Call when the program closes. */
    synchronized void dispose() {
        if (trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (RuntimeException e) {
                // already gone - nothing to do
            }
            trayIcon = null;
        }
    }

    /** Creates the tray icon the first time it is needed. */
    private boolean start() {
        if (trayIcon != null) {
            return true;
        }
        if (triedToStart) {
            return false;
        }
        triedToStart = true;
        try {
            if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
                return false;
            }
            TrayIcon icon = new TrayIcon(makeIcon(), "Camden's Pain(t)");
            icon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
            return true;
        } catch (Exception | Error e) {
            return false; // no tray on this system: carry on without notifications
        }
    }

    /** A small painted square, used as the tray icon. */
    private static BufferedImage makeIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, 16, 16);
        g.setColor(new java.awt.Color(0xD9, 0x3A, 0x3A));
        g.fillRect(1, 1, 7, 7);
        g.setColor(new java.awt.Color(0x2F, 0x6F, 0xD6));
        g.fillRect(8, 8, 7, 7);
        g.setColor(java.awt.Color.DARK_GRAY);
        g.drawRect(0, 0, 15, 15);
        g.dispose();
        return image;
    }
}
