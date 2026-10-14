package com.mycompany.paint;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Everything about saving an image in a particular file format: reading a
 * file's extension, writing the image out, and describing what a format
 * change can cost (the warning shown by Save As).
 *
 * <p>PNG keeps every pixel exactly, including transparency. JPEG is lossy
 * and has no transparency. BMP is lossless but has no transparency either.</p>
 */
final class ImageFormats {

    /** The format used when a file name has no usable extension. */
    static final String DEFAULT_FORMAT = "png";

    /** Utility class - nobody should construct one. */
    private ImageFormats() {
    }

    /**
     * Lower-case file extension without the dot, or null if there isn't one.
     *
     * @param file the file to inspect
     * @return e.g. "png" for "drawing.PNG", or null for "drawing" or "drawing."
     */
    static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Turns an extension into one of the format names this program uses
     * ("png", "jpg" or "bmp"). "jpeg" counts as "jpg"; null or anything
     * unrecognized becomes the default, PNG.
     *
     * @param extension an extension without the dot, or null
     * @return "png", "jpg" or "bmp"
     */
    static String normalize(String extension) {
        if (extension == null) {
            return DEFAULT_FORMAT;
        }
        String lower = extension.toLowerCase(Locale.ROOT);
        if (lower.equals("jpeg") || lower.equals("jpg")) {
            return "jpg";
        }
        if (lower.equals("bmp")) {
            return "bmp";
        }
        return DEFAULT_FORMAT;
    }

    /**
     * @param format a name from {@link #normalize}
     * @return true if the format cannot store transparent pixels
     */
    static boolean lacksTransparency(String format) {
        return format.equals("jpg") || format.equals("bmp");
    }

    /**
     * Describes what could be lost by saving an image that started out in
     * one format into another.
     *
     * @param fromFormat      the format the image came from (any extension)
     * @param toFormat        the format it is being saved as (any extension)
     * @param hasTransparency whether the image currently has see-through pixels
     * @return a short explanation of the possible loss, or null if the
     *         change cannot lose anything (same format, or going to PNG)
     */
    static String warningFor(String fromFormat, String toFormat, boolean hasTransparency) {
        String from = normalize(fromFormat);
        String to = normalize(toFormat);
        if (from.equals(to) || !(lacksTransparency(to))) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        text.append("Saving as ").append(to.toUpperCase(Locale.ROOT))
                .append(" instead of ").append(from.toUpperCase(Locale.ROOT))
                .append(" can lose features or data:\n");
        if (to.equals("jpg")) {
            text.append("\n- JPEG is lossy: colors change slightly and edges blur, and the")
                    .append("\n  original pixels cannot be recovered.");
        }
        text.append("\n- ").append(to.toUpperCase(Locale.ROOT))
                .append(" cannot store transparency: see-through areas become white.");
        if (hasTransparency) {
            text.append("\n\nThis image has transparent areas, so they WILL turn white.");
        }
        text.append("\n\nThe picture on screen is not changed. Save anyway?");
        return text.toString();
    }

    /**
     * Writes an image to a file in the format matching the file's extension
     * (PNG when there is none). Formats without transparency get the image
     * flattened onto white first.
     *
     * @param picture the image to write
     * @param file    where to write it
     * @throws IOException if no writer could write the file
     */
    static void write(BufferedImage picture, File file) throws IOException {
        String format = normalize(extensionOf(file));
        BufferedImage toWrite = picture;

        if (lacksTransparency(format)) {
            toWrite = new BufferedImage(
                    picture.getWidth(), picture.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = toWrite.createGraphics();
            g.drawImage(picture, 0, 0, java.awt.Color.WHITE, null);
            g.dispose();
        }

        if (!ImageIO.write(toWrite, format, file)) {
            throw new IOException("No writer is available for the " + format + " format.");
        }
    }
}
