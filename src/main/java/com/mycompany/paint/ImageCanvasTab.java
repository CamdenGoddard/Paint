package com.mycompany.paint;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;

/**
 * One open image: its canvas, the file it's tied to (if any), whether it
 * has unsaved changes, its undo/redo history, and the mouse-driven drawing
 * and editing logic for it.
 *
 * <p>{@link PaintApp} can have several of these open in the {@code TabPane}
 * at once - that's the whole point of tabs. Each one is completely
 * independent: drawing in one tab, undoing in it, or saving it never touches
 * another tab's canvas, history, or file. The one thing every tab shares is
 * the toolbar settings ({@link ToolSettings}) - the currently selected
 * tool, color, width and so on - which is why those live on
 * {@code PaintApp} instead of here.</p>
 *
 * <h2>How edits are kept undoable</h2>
 * <p>Every edit follows the same pattern: take a snapshot of the canvas
 * <em>before</em> the edit changes it, draw the live preview on top of
 * that snapshot while the mouse is still down, and when the edit is
 * final, hand the "before" snapshot to the {@link UndoHistory}. The live
 * preview is therefore always drawn on a canvas that can be put back
 * exactly as it was.</p>
 *
 * <h2>Selections</h2>
 * <p>The selection rectangle is drawn on a second, transparent canvas
 * stacked over the picture, so the dashed outline is never part of the
 * image that gets saved or snapshotted. A piece of the image being moved
 * (or just pasted) is "floating": its pixels are held aside in
 * {@code floating}, and every mouse movement redraws the canvas as
 * (image with a hole) + (floating piece at its new position). It becomes
 * a permanent, undoable part of the picture the moment anything else
 * happens - another click, another tool, a save, an undo.</p>
 */
public final class ImageCanvasTab {

    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;

    /**
     * The most recently copied or cut piece of image, shared by every tab
     * so a copy in one tab can be pasted into another. The system clipboard
     * gets a copy too, so it also works with other programs.
     */
    private static Image appClipboard;

    private final Canvas canvas;
    private final GraphicsContext gc;
    /** Transparent layer over the canvas that holds only the selection outline. */
    private final Canvas overlay;
    private final GraphicsContext overlayGc;
    private final Tab tab;
    private final ToolSettings settings;
    private final UndoHistory history = new UndoHistory();

    /** The file this canvas was last opened from or saved to. Null = never saved. */
    private File currentFile;

    /** True any time this tab's canvas has changes that have not been saved. */
    private boolean unsavedChanges;

    /** Where the current drag started, in canvas coordinates. Only meaningful mid-drag. */
    private double dragStartX;
    private double dragStartY;

    /**
     * A snapshot of the canvas taken the instant a drag started. Every
     * mouse-drag event during that drag restores this snapshot first, then
     * draws one fresh preview on top of it - which is what makes the
     * preview move smoothly instead of leaving a trail. When the drag ends
     * it is also what goes onto the undo stack. Null whenever no
     * drag-to-draw is in progress.
     */
    private WritableImage dragSnapshot;

    /** True once a freehand drag has actually drawn something (a plain click does not count). */
    private boolean strokeMoved;

    // ---- selection state -------------------------------------------------

    private enum SelectDrag { NONE, SELECTING, MOVING }

    private SelectDrag selectDrag = SelectDrag.NONE;
    private boolean hasSelection;
    private int selX;
    private int selY;
    private int selW;
    private int selH;
    private double anchorX;
    private double anchorY;
    private double grabDx;
    private double grabDy;

    /** The lifted-out pixels being moved or just pasted. Null when nothing is floating. */
    private Image floating;
    /** The canvas as it looks with the floating piece removed (a white hole, if it was lifted). */
    private WritableImage floatingBase;
    /** The canvas as it looked before the piece was lifted or pasted - what Undo restores. */
    private WritableImage floatingBefore;

    /**
     * Creates a new tab with a blank white canvas of the default size.
     *
     * @param settings the shared toolbar settings this tab's drawing should read from
     */
    public ImageCanvasTab(ToolSettings settings) {
        this(settings, null, null, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Creates a new tab pre-loaded with an already-opened image, sized to
     * match it exactly.
     *
     * @param settings the shared toolbar settings this tab's drawing should read from
     * @param file     the file the image was opened from
     * @param image    the already-loaded image to draw onto the new canvas
     */
    public ImageCanvasTab(ToolSettings settings, File file, Image image) {
        this(settings, file, image, image.getWidth(), image.getHeight());
    }

    private ImageCanvasTab(ToolSettings settings, File file, Image image, double width, double height) {
        this.settings = settings;
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        overlay = new Canvas(width, height);
        overlay.setMouseTransparent(true); // clicks fall through to the real canvas
        // A canvas must be focus-traversable to take focus when clicked. We want that:
        // clicking the picture should pull focus out of the toolbar's text and number
        // boxes, so what was typed there counts and Ctrl+Z means "undo", not "undo typing".
        canvas.setFocusTraversable(true);
        overlayGc = overlay.getGraphicsContext2D();

        if (image == null) {
            paintBlank();
        } else {
            gc.drawImage(image, 0, 0);
        }
        currentFile = file;

        tab = new Tab();
        tab.setContent(new ScrollPane(new Pane(canvas, overlay)));
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
        commitFloating(); // a piece still being moved should be saved where it sits

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
     * size and forgets its file and its undo history, as if it were freshly
     * opened. Used both for "Close Image" and for the very last tab when
     * the user closes it (a paint window is never left with zero tabs open).
     */
    public void resetToBlank() {
        floating = null;
        floatingBase = null;
        floatingBefore = null;
        hasSelection = false;
        selectDrag = SelectDrag.NONE;

        canvas.setWidth(DEFAULT_WIDTH);
        canvas.setHeight(DEFAULT_HEIGHT);
        overlay.setWidth(DEFAULT_WIDTH);
        overlay.setHeight(DEFAULT_HEIGHT);
        paintBlank();
        redrawSelectionOverlay();

        history.clear();
        currentFile = null;
        markSaved();
    }

    // ------------------------------------------------------------------
    // Edit menu actions: undo, redo, clear, copy, cut, paste
    // ------------------------------------------------------------------

    /**
     * Undoes the most recent edit, if there is one. Whatever the canvas
     * looks like right now goes onto the redo stack first.
     */
    public void undo() {
        commitFloating();
        WritableImage previous = history.undo(snapshot());
        if (previous != null) {
            restore(previous);
        }
    }

    /** Redoes the edit that was most recently undone, if there is one. */
    public void redo() {
        commitFloating();
        WritableImage next = history.redo(snapshot());
        if (next != null) {
            restore(next);
        }
    }

    /**
     * Fills the whole canvas white again. The caller is expected to have
     * already asked the user "are you sure". This is an ordinary edit, so
     * it can still be undone.
     */
    public void clearCanvas() {
        commitFloating();
        history.record(snapshot());
        paintBlank();
        hasSelection = false;
        redrawSelectionOverlay();
        markUnsaved();
    }

    /**
     * Copies the selected piece of the image to the clipboard. Does
     * nothing if nothing is selected.
     */
    public void copySelection() {
        if (!hasSelection) {
            return;
        }
        Image piece = (floating != null) ? floating : snapshotRegion(selX, selY, selW, selH);
        appClipboard = piece;

        ClipboardContent content = new ClipboardContent();
        content.putImage(piece);
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Copies the selected piece to the clipboard, then erases it from the
     * image (leaving white). Does nothing if nothing is selected.
     */
    public void cutSelection() {
        if (!hasSelection) {
            return;
        }
        copySelection();

        if (floating != null) {
            // The piece was already lifted, so the canvas "with a hole" is
            // exactly what the image looks like without it.
            clearAndDraw(floatingBase);
            history.record(floatingBefore);
            floating = null;
            floatingBase = null;
            floatingBefore = null;
        } else {
            history.record(snapshot());
            gc.setFill(Color.WHITE);
            gc.fillRect(selX, selY, selW, selH);
        }
        hasSelection = false;
        redrawSelectionOverlay();
        markUnsaved();
    }

    /**
     * Pastes the clipboard's image into the top-left corner of the canvas
     * as a floating, selected piece, and switches to the Select tool so it
     * can be dragged into place right away. Does nothing if the clipboard
     * has no image.
     */
    public void pasteFromClipboard() {
        Image pasted = null;
        Clipboard systemClipboard = Clipboard.getSystemClipboard();
        if (systemClipboard.hasImage()) {
            pasted = systemClipboard.getImage();
        }
        if (pasted == null) {
            pasted = appClipboard;
        }
        if (pasted == null) {
            return;
        }

        commitFloating();
        floatingBefore = snapshot();
        floatingBase = floatingBefore; // nothing is removed, so the base is the "before" picture
        floating = pasted;

        selX = 0;
        selY = 0;
        selW = (int) pasted.getWidth();
        selH = (int) pasted.getHeight();
        hasSelection = true;

        settings.setTool(DrawTool.SELECT);
        renderFloating();
        redrawSelectionOverlay();
        markUnsaved();
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    /** Fills the whole canvas white, like a fresh sheet of paper. */
    private void paintBlank() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /** Takes a picture of the whole canvas as it looks right now. */
    private WritableImage snapshot() {
        return canvas.snapshot(null, null);
    }

    /** Takes a picture of just one rectangle of the canvas. The rectangle must lie inside it. */
    private WritableImage snapshotRegion(int x, int y, int w, int h) {
        SnapshotParameters params = new SnapshotParameters();
        params.setViewport(new Rectangle2D(x, y, w, h));
        return canvas.snapshot(params, null);
    }

    /**
     * Replaces the canvas's contents with a picture. The canvas is cleared
     * first so any transparent parts of the picture do not show whatever
     * was drawn underneath them.
     */
    private void clearAndDraw(Image picture) {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.drawImage(picture, 0, 0);
    }

    /** Puts an undo/redo snapshot back on the canvas and drops any selection. */
    private void restore(WritableImage picture) {
        clearAndDraw(picture);
        hasSelection = false;
        redrawSelectionOverlay();
        markUnsaved();
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

    // ------------------------------------------------------------------
    // Mouse handling
    // ------------------------------------------------------------------

    /** Hooks up mouse-press/drag/release so this canvas responds to drawing. */
    private void setupMouseHandlers() {
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
    }

    /**
     * Starts whatever the currently selected tool does: begins a freehand
     * stroke, takes the "before" snapshot for a shape or text preview,
     * starts a selection, or samples a color for the eyedropper. A
     * right-click here always samples a color, regardless of which tool is
     * selected.
     *
     * @param e the mouse-press event
     */
    private void handleMousePressed(MouseEvent e) {
        canvas.requestFocus();
        if (e.getButton() == MouseButton.SECONDARY) {
            sampleColorAt(e.getX(), e.getY());
            return;
        }
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }

        DrawTool tool = settings.getTool();
        if (tool != DrawTool.SELECT) {
            dropSelection(); // using any other tool puts a floating piece down and deselects
        }

        if (tool == DrawTool.EYEDROPPER) {
            sampleColorAt(e.getX(), e.getY());
            return;
        }

        dragStartX = e.getX();
        dragStartY = e.getY();

        if (tool.isFreehand()) {
            dragSnapshot = snapshot();
            strokeMoved = false;
            applyStrokeSettings(tool);
            gc.beginPath();
            gc.moveTo(dragStartX, dragStartY);
            gc.stroke();
        } else if (tool.isShape()) {
            // Shapes preview live rather than commit immediately - remember
            // the "before" picture so every drag event can restore it before
            // drawing a fresh outline.
            dragSnapshot = snapshot();
        } else if (tool == DrawTool.TEXT) {
            if (settings.getText().isEmpty()) {
                return; // nothing typed yet, so there is nothing to place
            }
            dragSnapshot = snapshot();
            redrawPreview(tool, dragStartX, dragStartY);
        } else if (tool == DrawTool.SELECT) {
            pressSelect(e.getX(), e.getY());
        }
    }

    /**
     * Continues whatever was started in {@link #handleMousePressed}: keeps
     * extending a freehand stroke, redraws a shape's or text's live preview
     * at the new mouse position, or resizes/moves the selection. A
     * right-click-drag samples a color on every move, which is what lets
     * the color grabber change the color of a shape while it's still being
     * dragged out.
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
        if (tool.isFreehand()) {
            if (dragSnapshot == null) {
                return;
            }
            applyStrokeSettings(tool);
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            strokeMoved = true;
            markUnsaved();
        } else if (tool.isShape() || tool == DrawTool.TEXT) {
            if (dragSnapshot != null) {
                redrawPreview(tool, e.getX(), e.getY());
            }
        } else if (tool == DrawTool.SELECT) {
            dragSelect(e.getX(), e.getY());
        }
    }

    /**
     * Finishes a drag. A shape or text drag commits one final preview at
     * the release point and files its "before" snapshot on the undo stack.
     * A freehand stroke only needs its snapshot filed. A selection drag
     * just ends (a floating piece stays floating until something else
     * happens).
     *
     * @param e the mouse-release event
     */
    private void handleMouseReleased(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return; // letting go of the right button mid-drag must not end the left-button drag
        }

        DrawTool tool = settings.getTool();
        if (tool.isFreehand()) {
            if (dragSnapshot != null && strokeMoved) {
                history.record(dragSnapshot);
                markUnsaved();
            }
            dragSnapshot = null;
        } else if (tool.isShape() || tool == DrawTool.TEXT) {
            if (dragSnapshot != null) {
                redrawPreview(tool, e.getX(), e.getY());
                history.record(dragSnapshot);
                dragSnapshot = null;
                markUnsaved();
            }
        } else if (tool == DrawTool.SELECT) {
            releaseSelect();
        }
    }

    /**
     * Restores the pre-drag snapshot, then draws one fresh shape outline
     * (or the typed text) for the drag from its start point to the given
     * point. Called on every drag event (for the live preview) and once
     * more on release (to commit the final result) - the two are the same
     * operation, just at different mouse positions.
     *
     * @param tool which shape (or the Text tool) to draw
     * @param x    current mouse x, in canvas coordinates
     * @param y    current mouse y, in canvas coordinates
     */
    private void redrawPreview(DrawTool tool, double x, double y) {
        clearAndDraw(dragSnapshot);
        if (tool == DrawTool.TEXT) {
            gc.setFill(settings.getColor());
            gc.setFont(Font.font(settings.getFontSize()));
            gc.setTextBaseline(VPos.TOP);
            gc.fillText(settings.getText(), x, y);
        } else {
            applyStrokeSettings(tool);
            ShapeDrawer.draw(gc, tool, dragStartX, dragStartY, x, y, settings.getPolygonSides());
        }
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
     * Reads the pixel color at a canvas position and feeds it back into
     * the shared toolbar settings as the new active color.
     *
     * @param x canvas x coordinate to sample
     * @param y canvas y coordinate to sample
     */
    private void sampleColorAt(double x, double y) {
        WritableImage snapshot = snapshot();
        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        if (ix < 0 || iy < 0 || ix >= snapshot.getWidth() || iy >= snapshot.getHeight()) {
            return;
        }
        Color sampled = snapshot.getPixelReader().getColor(ix, iy);
        settings.setColor(sampled);
    }

    // ------------------------------------------------------------------
    // Selection: drawing the rectangle, moving a piece, floating pieces
    // ------------------------------------------------------------------

    /**
     * Mouse pressed with the Select tool. Pressing inside the current
     * selection grabs it to be moved; pressing anywhere else puts down any
     * floating piece and starts a brand-new selection rectangle.
     */
    private void pressSelect(double x, double y) {
        boolean insideSelection = hasSelection
                && x >= selX && x < selX + selW && y >= selY && y < selY + selH;

        if (insideSelection) {
            selectDrag = SelectDrag.MOVING;
            grabDx = x - selX;
            grabDy = y - selY;
        } else {
            dropSelection();
            selectDrag = SelectDrag.SELECTING;
            anchorX = clamp(x, canvas.getWidth());
            anchorY = clamp(y, canvas.getHeight());
        }
    }

    /**
     * Mouse dragged with the Select tool: stretches the new selection
     * rectangle live, or carries the selected piece along live. The piece is
     * not lifted out of the picture until the first actual movement, so a
     * plain click inside a selection changes nothing.
     */
    private void dragSelect(double x, double y) {
        if (selectDrag == SelectDrag.SELECTING) {
            double cx = clamp(x, canvas.getWidth());
            double cy = clamp(y, canvas.getHeight());
            selX = (int) Math.round(Math.min(anchorX, cx));
            selY = (int) Math.round(Math.min(anchorY, cy));
            selW = (int) Math.round(Math.abs(cx - anchorX));
            selH = (int) Math.round(Math.abs(cy - anchorY));
            hasSelection = selW > 0 && selH > 0;
            redrawSelectionOverlay();
        } else if (selectDrag == SelectDrag.MOVING) {
            if (floating == null) {
                liftSelection();
            }
            selX = (int) Math.round(x - grabDx);
            selY = (int) Math.round(y - grabDy);
            renderFloating();
            redrawSelectionOverlay();
        }
    }

    /** Mouse released with the Select tool: throws away a selection too small to be on purpose. */
    private void releaseSelect() {
        if (selectDrag == SelectDrag.SELECTING && (selW < 2 || selH < 2)) {
            hasSelection = false;
            redrawSelectionOverlay();
        }
        selectDrag = SelectDrag.NONE;
    }

    /**
     * Lifts the selected pixels off the picture so they can be moved:
     * remembers the picture as it was (for Undo), keeps a copy of the
     * selected piece, and leaves a white hole where it came from.
     */
    private void liftSelection() {
        floatingBefore = snapshot();
        floating = snapshotRegion(selX, selY, selW, selH);
        gc.setFill(Color.WHITE);
        gc.fillRect(selX, selY, selW, selH);
        floatingBase = snapshot();
    }

    /** Redraws the canvas as (picture without the floating piece) + (piece at its current position). */
    private void renderFloating() {
        clearAndDraw(floatingBase);
        gc.drawImage(floating, selX, selY);
    }

    /**
     * Makes a floating piece a permanent part of the picture: files the
     * pre-lift snapshot on the undo stack so the whole move (or paste) is
     * one undoable step. The pixels are already on the canvas, so nothing
     * is redrawn. If the piece was dragged partly off the edge, the
     * selection shrinks to the part still visible. Does nothing when
     * nothing is floating.
     */
    private void commitFloating() {
        if (floating == null) {
            return;
        }
        history.record(floatingBefore);
        floating = null;
        floatingBase = null;
        floatingBefore = null;
        markUnsaved();

        int left = Math.max(0, selX);
        int top = Math.max(0, selY);
        int right = Math.min((int) canvas.getWidth(), selX + selW);
        int bottom = Math.min((int) canvas.getHeight(), selY + selH);
        if (right > left && bottom > top) {
            selX = left;
            selY = top;
            selW = right - left;
            selH = bottom - top;
        } else {
            hasSelection = false; // dragged completely off the canvas
        }
        redrawSelectionOverlay();
    }

    /** Puts down any floating piece, then clears the selection entirely. */
    private void dropSelection() {
        commitFloating();
        hasSelection = false;
        selectDrag = SelectDrag.NONE;
        redrawSelectionOverlay();
    }

    /**
     * Redraws the selection outline on the overlay layer: a white line with
     * a black dashed line on top, so it stays visible over any color.
     */
    private void redrawSelectionOverlay() {
        overlayGc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
        if (!hasSelection) {
            return;
        }
        overlayGc.setLineWidth(1);
        overlayGc.setLineCap(StrokeLineCap.BUTT);

        overlayGc.setLineDashes();
        overlayGc.setStroke(Color.WHITE);
        overlayGc.strokeRect(selX + 0.5, selY + 0.5, selW - 1, selH - 1);

        overlayGc.setLineDashes(5, 5);
        overlayGc.setStroke(Color.BLACK);
        overlayGc.strokeRect(selX + 0.5, selY + 0.5, selW - 1, selH - 1);
    }

    /** Keeps a coordinate inside 0..max. */
    private static double clamp(double value, double max) {
        return Math.max(0, Math.min(max, value));
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
