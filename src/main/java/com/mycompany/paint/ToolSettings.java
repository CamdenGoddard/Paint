package com.mycompany.paint;

import javafx.scene.paint.Color;

/**
 * The drawing settings shared by every tab: which tool is active, what
 * color and line width new strokes use, and whether outlines are dashed.
 *
 * <p>{@link PaintApp} owns the actual toolbar controls (the color picker,
 * the width slider, and so on) and implements this interface by reading
 * them. {@link ImageCanvasTab} only ever sees this interface, not
 * {@code PaintApp} itself - each tab does not need to know how the
 * settings are stored, only how to read (and, for the color grabber,
 * write) them.</p>
 */
public interface ToolSettings {

    /**
     * Returns the active drawing tool.
     *
     * @return the tool currently selected in the toolbar
     */
    DrawTool getTool();

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
}
