package com.mycompany.paint;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.scene.paint.Color;

/**
 * Turns a JavaFX {@link Color} into text a person can read: a hex code,
 * an rgb() triple, and - when it happens to be one of a curated list of
 * common colors - an English name.
 *
 * <p>The name lookup only recognizes an intentionally short list of
 * well-known colors (the ones {@link javafx.scene.control.ColorPicker}'s
 * own swatch grid offers, plus a few extras). It is not a "nearest color"
 * search - a color one shade off from Red is reported as "Custom", not
 * "Red". That keeps the label honest: it only ever claims a name when the
 * color is exactly that color.</p>
 */
public final class ColorNames {

    // Utility class - nobody should be able to construct one of these.
    private ColorNames() {
    }

    private static final Map<Color, String> NAMES = buildNames();

    private static Map<Color, String> buildNames() {
        // LinkedHashMap only because it makes debugging output predictable;
        // plain lookup order doesn't otherwise matter here.
        Map<Color, String> names = new LinkedHashMap<>();
        names.put(Color.BLACK, "Black");
        names.put(Color.WHITE, "White");
        names.put(Color.GRAY, "Gray");
        names.put(Color.SILVER, "Silver");
        names.put(Color.DARKGRAY, "Dark Gray");
        names.put(Color.LIGHTGRAY, "Light Gray");
        names.put(Color.RED, "Red");
        names.put(Color.DARKRED, "Dark Red");
        names.put(Color.MAROON, "Maroon");
        names.put(Color.PINK, "Pink");
        names.put(Color.HOTPINK, "Hot Pink");
        names.put(Color.ORANGE, "Orange");
        names.put(Color.DARKORANGE, "Dark Orange");
        names.put(Color.GOLD, "Gold");
        names.put(Color.YELLOW, "Yellow");
        names.put(Color.OLIVE, "Olive");
        names.put(Color.GREEN, "Green");
        names.put(Color.DARKGREEN, "Dark Green");
        names.put(Color.LIME, "Lime");
        names.put(Color.TEAL, "Teal");
        names.put(Color.CYAN, "Cyan");
        names.put(Color.LIGHTBLUE, "Light Blue");
        names.put(Color.SKYBLUE, "Sky Blue");
        names.put(Color.BLUE, "Blue");
        names.put(Color.NAVY, "Navy");
        names.put(Color.DARKBLUE, "Dark Blue");
        names.put(Color.PURPLE, "Purple");
        names.put(Color.INDIGO, "Indigo");
        names.put(Color.VIOLET, "Violet");
        names.put(Color.MAGENTA, "Magenta");
        names.put(Color.BROWN, "Brown");
        names.put(Color.SADDLEBROWN, "Saddle Brown");
        names.put(Color.TAN, "Tan");
        names.put(Color.BEIGE, "Beige");
        names.put(Color.IVORY, "Ivory");
        names.put(Color.TRANSPARENT, "Transparent");
        return names;
    }

    /**
     * Converts a color to its hex code.
     *
     * @param c any color
     * @return a 6-digit hex code like {@code #FF0000}, always upper-case
     *         with a leading {@code #} and no alpha channel
     */
    public static String hex(Color c) {
        int r = to255(c.getRed());
        int g = to255(c.getGreen());
        int b = to255(c.getBlue());
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Converts a color to an rgb() triple.
     *
     * @param c any color
     * @return an rgb triple like {@code rgb(255, 0, 0)}
     */
    public static String rgb(Color c) {
        int r = to255(c.getRed());
        int g = to255(c.getGreen());
        int b = to255(c.getBlue());
        return "rgb(" + r + ", " + g + ", " + b + ")";
    }

    /**
     * Looks up the English name for a color.
     *
     * @param c any color
     * @return the English name for this exact color if it's one of the
     *         common colors this class recognizes, otherwise {@code "Custom"}
     */
    public static String nameOf(Color c) {
        return NAMES.getOrDefault(c, "Custom");
    }

    /**
     * The full label shown next to the color picker: name, hex, and rgb
     * all together, e.g. {@code "Red · #FF0000 · rgb(255, 0, 0)"}.
     *
     * @param c the currently selected color
     * @return the combined description for that color
     */
    public static String describe(Color c) {
        return nameOf(c) + "  ·  " + hex(c) + "  ·  " + rgb(c);
    }

    // JavaFX Color channels are doubles from 0.0-1.0; ImageIO/most people
    // think in 0-255. Math.round (not a plain cast) avoids 254 vs 255
    // rounding surprises at the edges.
    private static int to255(double channel) {
        return (int) Math.round(channel * 255.0);
    }
}
