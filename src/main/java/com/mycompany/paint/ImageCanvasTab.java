package com.mycompany.paint;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * One open image: its canvas, the file it's tied to (if any), whether it
 * has unsaved changes, and the mouse-driven drawing logic for it.
 *
 * <p>{@link PaintApp} can have several of these open in the {@code TabPane}
 * at once - that's the whole point of tabs. Each one is completely
 * independent: drawing in one tab, or saving it, never touches another
 * tab's canvas or file. The one thing every tab shares is the toolbar
 * settings ({@link ToolSettings}) - the currently selected tool, color,
 * width, and dash state - which is why those live on {@code PaintApp}
 * instead of here.</p>
 */
public final class ImageCanvasTab {

    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Tab tab;
    private final ToolSettings settings;

    /** The file this canvas was last opened from or saved to. Null = never saved. */
    private File currentFile;

    /** True any time this tab's canvas has changes that have not been saved. */
    private boolean unsavedChanges;

    /** Where the current drag started, in canvas coordinates. Only meaningful mid-drag. */
    private double dragStartX;
    private double dragStartY;

    /**
     * A snapshot of the canvas taken the instant a shape-drawing drag
     * started. Every mouse-drag event during that drag restores this
     * snapshot first, then draws one fresh preview on top of it - which
     * is what makes the shape preview move smoothly instead of leaving a
     * trail of outlines. Null whenever no shape drag is in progress.
     */
    private WritableImage dragSnapshot;

    /**
     * Creates a new tab with a blank white canvas, the same size Sprint 1
     * and 2 used.
     *
     * @param settings the shared toolbar settings this tab's drawing should read from
     */
    public ImageCanvasTab(ToolSettings settings) {
        this.settings = settings;
        canvas = new Canvas(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        paintBlank();

        tab = new Tab();
        tab.setContent(new ScrollPane(canvas));
        tab.setUserData(this);

        setupMouseHandlers();
        updateTabTitle();
    }

    /**
     * Creates a new tab pre-loaded with an already-opened image, sized to
     * match it exactly (same behavior as Sprint 1/2's Open).
     *
     * @param settings the shared toolbar settings this tab's drawing should read from
     * @param file     the file the image was opened from
     * @param image    the already-loaded image to draw onto the new canvas
     */
    public ImageCanvasTab(ToolSettings settings, File file, Image image) {
        this.settings = settings;
        canvas = new Canvas(image.getWidth(), image.getHeight());
        gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
        currentFile = file;

        tab = new Tab();
        tab.setContent(new ScrollPane(canvas));
        tab.setUserData(this);

        setupMouseHandlers();
        updateTabTitle();
    }

    /**
     * Returns this tab's underlying JavaFX {@code Tab}.
     *
     * @return the JavaFX {@code Tab} this canvas lives inside, ready to add to a {@code TabPane}
     */
    public Tab getTab() {
        return tab;
    }

    /**
     * Returns the file this tab is tied to.
     *
     * @return the file this tab is tied to, or null if it has never been saved
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * Reports whether this tab has unsaved changes.
     *
     * @return true if this tab has changes that have not been saved to disk
     */
    public boolean isUnsaved() {
        return unsavedChanges;
    }

    /**
     * Saves whatever is currently on the canvas to the given file, in
     * whatever format matches its extension (defaulting to PNG). On
     * success, this tab remembers the file and clears its unsaved flag.
     *
     * @param file where to save
     * @throws IOException if the image could not be written - the caller
     *                      is expected to show the user an error message
     */
    public void writeToFile(File file) throws IOException {
        String extension = extensionOf(file);
        if (extension == null) {
            extension = "png";
        }

        WritableImage snapshot = canvas.snapshot(null, null);
        BufferedImage picture = SwingFXUtils.fromFXImage(snapshot, null);

        // JPG and BMP don't support transparency, so flatten onto white first.
        if (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("bmp")) {
            BufferedImage flattened = new BufferedImage(
                    picture.getWidth(), picture.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = flattened.createGraphics();
            g.drawImage(picture, 0, 0, java.awt.Color.WHITE, null);
            g.dispose();
            picture = flattened;
        }

        ImageIO.write(picture, extension, file);
        currentFile = file;
        markSaved();
    }

    /**
     * Wipes this tab's canvas back to a blank white sheet at the default
     * size and forgets its file, as if it were freshly opened. Used both
     * for "Close Image" and for the very last tab when the user closes it
     * (a paint window is never left with zero tabs open).
     */
    public void resetToBlank() {
        canvas.setWidth(DEFAULT_WIDTH);
        canvas.setHeight(DEFAULT_HEIGHT);
        paintBlank();
        currentFile = null;
        markSaved();
    }

    /** Fills the whole canvas white, like a fresh sheet of paper. */
    private void paintBlank() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /** Marks this tab as having unsaved changes and refreshes its tab title. */
    private void markUnsaved() {
        unsavedChanges = true;
        updateTabTitle();
    }

    /** Marks this tab as fully saved and refreshes its tab title. */
    private void markSaved() {
        unsavedChanges = false;
        updateTabTitle();
    }

    /**
     * Keeps the little text on the tab itself in sync: the file name (or
     * "Untitled"), with a trailing asterisk while there are unsaved changes.
     */
    private void updateTabTitle() {
        String name = (currentFile != null) ? currentFile.getName() : "Untitled";
        tab.setText(unsavedChanges ? name + " *" : name);
    }

    /** Hooks up mouse-press/drag/release so this canvas responds to drawing. */
    private void setupMouseHandlers() {
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
    }

    /**
     * Starts whatever the currently selected tool does: begins a freehand
     * stroke, takes the "before" snapshot for a shape preview, or samples
     * a color for the eyedropper. A right-click here always samples a
     * color, regardless of which tool is selected.
     *
     * @param e the mouse-press event
     */
    private void handleMousePressed(MouseEvent e) {
        if (e.isSecondaryButtonDown()) {
            sampleColorAt(e.getX(), e.getY());
            if (!e.isPrimaryButtonDown()) {
                return;
            }
        }
        if (!e.isPrimaryButtonDown()) {
            return;
        }

        DrawTool tool = settings.getTool();
        if (tool == DrawTool.EYEDROPPER) {
            sampleColorAt(e.getX(), e.getY());
            return;
        }

        dragStartX = e.getX();
        dragStartY = e.getY();

        if (tool == DrawTool.PENCIL || tool == DrawTool.ERASER) {
            applyStrokeSettings(tool);
            gc.beginPath();
            gc.moveTo(dragStartX, dragStartY);
            gc.stroke();
        } else {
            // Shape tools preview live rather than commit immediately -
            // remember the "before" picture so every drag event can
            // restore it before drawing a fresh outline.
            dragSnapshot = canvas.snapshot(null, null);
        }
    }

    /**
     * Continues whatever was started in {@link #handleMousePressed}: keeps
     * extending a freehand stroke, or redraws a shape's live preview at
     * the new mouse position. A right-click-drag samples a color on every
     * move, which is what lets the color grabber change the color of a
     * shape while it's still being dragged out.
     *
     * @param e the mouse-drag event
     */
    private void handleMouseDragged(MouseEvent e) {
        if (e.isSecondaryButtonDown()) {
            sampleColorAt(e.getX(), e.getY());
        }

        if (!e.isPrimaryButtonDown()) {
            return;
        }

        DrawTool tool = settings.getTool();
        if (tool == DrawTool.EYEDROPPER) {
            return;
        }

        if (tool == DrawTool.PENCIL || tool == DrawTool.ERASER) {
            applyStrokeSettings(tool);
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            markUnsaved();
        } else {
            redrawShapePreview(tool, e.getX(), e.getY());
        }
    }

    /**
     * Finishes a shape drag by committing one final preview at the
     * release point. Freehand tools and the eyedropper have nothing left
     * to do here - they already finished their work during the drag (or
     * the initial press).
     *
     * @param e the mouse-release event
     */
    private void handleMouseReleased(MouseEvent e) {
        DrawTool tool = settings.getTool();
        if (tool == DrawTool.PENCIL || tool == DrawTool.ERASER || tool == DrawTool.EYEDROPPER) {
            return;
        }
        if (dragSnapshot != null) {
            redrawShapePreview(tool, e.getX(), e.getY());
            dragSnapshot = null;
            markUnsaved();
        }
    }

    /**
     * Restores the pre-drag snapshot, then draws one fresh outline from
     * the drag's start point to the given end point. Called on every drag
     * event (for the live preview) and once more on release (to commit
     * the final shape) - the two are the same operation, just at
     * different mouse positions.
     *
     * @param tool which shape to draw
     * @param endX current mouse x, in canvas coordinates
     * @param endY current mouse y, in canvas coordinates
     */
    private void redrawShapePreview(DrawTool tool, double endX, double endY) {
        if (dragSnapshot != null) {
            gc.drawImage(dragSnapshot, 0, 0);
        }
        applyStrokeSettings(tool);
        drawShape(tool, dragStartX, dragStartY, endX, endY);
    }

    /**
     * Sets the graphics context's stroke color, width, and dash pattern
     * from the shared toolbar settings, reading them fresh every time
     * rather than caching them - so a mid-drag change (the color grabber,
     * or nudging the width slider) shows up immediately.
     *
     * <p>Dashed strokes use a {@code BUTT} line cap rather than the
     * default. With a rounded or square cap, each dash is visually
     * extended by half the line width at both ends; once the line gets
     * thick relative to the 8px gap in the dash pattern, those extensions
     * from neighboring dashes overlap and the gaps disappear entirely,
     * making a "dashed" line look solid. A butt cap ends each dash exactly
     * where it's drawn, so the gaps stay visible no matter how wide the
     * line is.
     *
     * @param tool the tool about to draw, so the eraser can override the color
     */
    private void applyStrokeSettings(DrawTool tool) {
        gc.setStroke(tool == DrawTool.ERASER ? Color.WHITE : settings.getColor());
        gc.setLineWidth(settings.getLineWidth());
        if (settings.isDashed()) {
            gc.setLineDashes(12, 8);
            gc.setLineCap(StrokeLineCap.BUTT);
        } else {
            gc.setLineDashes(); // empty pattern = solid line
            gc.setLineCap(StrokeLineCap.SQUARE);
        }
    }

    /**
     * Draws one shape's outline between two points. Square and Circle
     * behave like Rectangle and Ellipse but force their two dimensions
     * equal, using whichever of the drag's width/height is larger, and
     * keep whichever corner the drag actually started from.
     *
     * @param tool the shape to draw
     * @param x1   drag start x
     * @param y1   drag start y
     * @param x2   drag end (current) x
     * @param y2   drag end (current) y
     */
    private void drawShape(DrawTool tool, double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2);
        double minY = Math.min(y1, y2);
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);

        switch (tool) {
            case LINE:
                gc.strokeLine(x1, y1, x2, y2);
                break;

            case RECTANGLE:
                gc.strokeRect(minX, minY, width, height);
                break;

            case SQUARE: {
                double side = Math.max(width, height);
                double squareX = (x2 >= x1) ? x1 : x1 - side;
                double squareY = (y2 >= y1) ? y1 : y1 - side;
                gc.strokeRect(squareX, squareY, side, side);
                break;
            }

            case ELLIPSE:
                gc.strokeOval(minX, minY, width, height);
                break;

            case CIRCLE: {
                double diameter = Math.max(width, height);
                double circleX = (x2 >= x1) ? x1 : x1 - diameter;
                double circleY = (y2 >= y1) ? y1 : y1 - diameter;
                gc.strokeOval(circleX, circleY, diameter, diameter);
                break;
            }

            case TRIANGLE: {
                // Apex centered on top, base spanning the full width - a
                // plain isosceles triangle inscribed in the bounding box.
                double[] xs = { minX + width / 2.0, minX, minX + width };
                double[] ys = { minY, minY + height, minY + height };
                gc.strokePolygon(xs, ys, 3);
                break;
            }

            default:
                // PENCIL, ERASER, and EYEDROPPER never reach drawShape.
                break;
        }
    }

    /**
     * Reads the pixel color at a canvas position and feeds it back into
     * the shared toolbar settings as the new active color.
     *
     * @param x canvas x coordinate to sample
     * @param y canvas y coordinate to sample
     */
    private void sampleColorAt(double x, double y) {
        WritableImage snapshot = canvas.snapshot(null, null);
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        if (ix < 0 || iy < 0 || ix >= snapshot.getWidth() || iy >= snapshot.getHeight()) {
            return;
        }
        Color sampled = snapshot.getPixelReader().getColor(ix, iy);
        settings.setColor(sampled);
    }

    /**
     * Lower-case file extension without the dot, or null if there isn't one.
     *
     * @param file the file to inspect
     * @return e.g. "png" for "drawing.PNG", or null for "drawing" or "drawing."
     */
    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
