package com.mycompany.paint;

import javafx.application.Application;

/**
 * Entry point. All this does is hand off to JavaFX, which constructs
 * {@link PaintApp} and calls its {@code start} method once the toolkit
 * is ready.
 */
public class Main {

    /** Not instantiated - everything here happens through {@link #main}. */
    private Main() {
    }

    /**
     * Launches the application.
     *
     * @param args command-line arguments, passed straight through to JavaFX
     */
    public static void main(String[] args) {
        Application.launch(PaintApp.class, args);
    }
}
