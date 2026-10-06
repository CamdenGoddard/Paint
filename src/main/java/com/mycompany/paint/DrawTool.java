package com.mycompany.paint;

/**
 * Every tool the user can pick from the toolbar's tool selector.
 *
 * <p>Each tool controls what a mouse-press-and-drag on the canvas
 * produces. They fall into a few families, and {@link ImageCanvasTab}
 * reads this enum to decide which behavior to use:</p>
 * <ul>
 *   <li><b>Freehand</b> ({@link #PENCIL}, {@link #ERASER}) paint
 *       immediately as the mouse moves.</li>
 *   <li><b>Shapes</b> (see {@link #isShape()}) preview live while dragging
 *       and lock in when the mouse is released.</li>
 *   <li>{@link #TEXT} previews the typed text live under the mouse and
 *       locks it in on release.</li>
 *   <li>{@link #SELECT} draws a selection rectangle, and dragging inside an
 *       existing selection moves that piece of the image live.</li>
 *   <li>{@link #EYEDROPPER} does not draw at all - it samples a color.</li>
 * </ul>
 */
public enum DrawTool {

    /** Freehand drawing - paints along wherever the mouse is dragged. */
    PENCIL("Pencil (freehand)"),

    /** A single straight line from where the drag started to where it ends. */
    LINE("Line"),

    /** An outlined square. Width and height are forced equal, unlike Rectangle. */
    SQUARE("Square"),

    /** An outlined rectangle sized to the drag's bounding box. */
    RECTANGLE("Rectangle"),

    /** An outlined circle. Width and height are forced equal, unlike Ellipse. */
    CIRCLE("Circle"),

    /** An outlined ellipse sized to the drag's bounding box. */
    ELLIPSE("Ellipse"),

    /** An outlined isosceles triangle inscribed in the drag's bounding box. */
    TRIANGLE("Triangle (isosceles)"),

    /**
     * An outlined right triangle. The right angle is at the point where the
     * drag started, with legs running horizontally and vertically.
     */
    RIGHT_TRIANGLE("Right Triangle"),

    /**
     * An outlined regular polygon with any number of sides (set with the
     * "Sides" box in the toolbar), dragged out from its center.
     */
    POLYGON("Regular Polygon (N sides)"),

    /** An outlined five-pointed star sized to the drag's bounding box. */
    STAR("Star"),

    /**
     * Samples the color of whatever pixel is clicked and makes it the
     * active color, instead of drawing anything.
     */
    EYEDROPPER("Color Grabber"),

    /**
     * Freehand drawing that always paints white, regardless of the color
     * picker - a simple way to "erase" back to the blank canvas color.
     */
    ERASER("Eraser"),

    /**
     * Stamps the text typed into the toolbar's text box onto the image,
     * previewing it live under the mouse and placing it on release.
     */
    TEXT("Text"),

    /**
     * Selects a rectangular piece of the image. The selection can then be
     * copied, cut, pasted, or dragged to a new spot.
     */
    SELECT("Select (rectangle)");

    private final String displayName;

    DrawTool(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Reports whether this tool paints freehand as the mouse moves.
     *
     * @return true for the pencil and the eraser
     */
    public boolean isFreehand() {
        return this == PENCIL || this == ERASER;
    }

    /**
     * Reports whether this tool draws an outlined shape between the drag's
     * start and end points.
     *
     * @return true for line, square, rectangle, circle, ellipse, the two
     *         triangles, the regular polygon, and the star
     */
    public boolean isShape() {
        switch (this) {
            case LINE:
            case SQUARE:
            case RECTANGLE:
            case CIRCLE:
            case ELLIPSE:
            case TRIANGLE:
            case RIGHT_TRIANGLE:
            case POLYGON:
            case STAR:
                return true;
            default:
                return false;
        }
    }

    /**
     * The label shown for this tool in the toolbar's combo box.
     * @return a short, human-readable name for this tool
     */
    @Override
    public String toString() {
        return displayName;
    }
}
