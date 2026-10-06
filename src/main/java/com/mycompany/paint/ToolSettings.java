package com.mycompany.paint;

import javafx.scene.paint.Color;

/**
 * The drawing settings shared by every tab: which tool is active, what
 * color and line width new strokes use, whether outlines are dashed, and
 * the options for the polygon and text tools.
 *
 * <p>{@link PaintApp} owns the actual toolbar controls (the color picker,
 * the width slider, and so on) and implements this interface by reading
 * them. {@link ImageCanvasTab} only ever sees this interface, not
 * {@code PaintApp} itself - each tab does not need to know how the
 * settings are stored, only how to read (and, for the color grabber and
 * paste, write) them.</p>
 */
public interface ToolSettings {

    /**
     * Returns the active drawing tool.
     *
     * @return the tool currently selected in the toolbar
     */
    DrawTool getTool();

    /**
     * Changes the active tool. Pasting uses this to switch to the Select
     * tool, so the pasted piece can be dragged into place right away.
     *
     * @param tool the tool to select in the toolbar
     */
    void setTool(DrawTool tool);

    /**
     * Returns the active drawing color.
     *
     * @return the color new strokes should use (ignored by {@link DrawTool#ERASER},
     *         which always paints white)
     */
    Color getColor();

    /**
     * Changes the active color. Used by the color grabber tool to feed a
     * sampled pixel color back into the toolbar's color picker - which,
     * because the preview for an in-progress shape always redraws using
     * the current color, immediately changes the color of whatever shape
     * is being dragged out.
     *
     * @param color the newly sampled color
     */
    void setColor(Color color);

    /**
     * Returns the active line width.
     *
     * @return the current line width, in pixels, for both freehand strokes
     *         and shape outlines
     */
    double getLineWidth();

    /**
     * Reports whether new strokes should be dashed.
     *
     * @return true if new strokes and outlines should be drawn dashed
     *         rather than solid
     */
    boolean isDashed();

    /**
     * Returns how many sides the Regular Polygon tool should draw.
     *
     * @return the number of sides, always at least 3
     */
    int getPolygonSides();

    /**
     * Returns the text the Text tool should place on the image.
     *
     * @return the text typed into the toolbar's text box; may be empty,
     *         in which case the Text tool does nothing
     */
    String getText();

    /**
     * Returns the size of the text the Text tool places.
     *
     * @return the font size, in pixels
     */
    double getFontSize();
}
