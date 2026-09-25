package com.mycompany.paint;

/**
 * Every tool the user can pick from the toolbar's tool selector.
 *
 * <p>Each tool controls two things: what a mouse-press-and-drag on the
 * canvas produces, and (for {@link #EYEDROPPER}) whether it draws at all.
 * See {@link ImageCanvasTab} for the actual mouse-handling logic that
 * reads this enum and decides what to draw.</p>
 *
 * <p>{@link #PENCIL} and {@link #ERASER} paint immediately as the mouse
 * moves (freehand). Every other tool except {@link #EYEDROPPER} previews
 * live while dragging and only commits the final shape when the mouse is
 * released - see {@link ImageCanvasTab#redrawShapePreview}.</p>
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
    TRIANGLE("Triangle"),

    /**
     * Samples the color of whatever pixel is clicked and makes it the
     * active color, instead of drawing anything.
     */
    EYEDROPPER("Color Grabber"),

    /**
     * Freehand drawing that always paints white, regardless of the color
     * picker - a simple way to "erase" back to the blank canvas color.
     */
    ERASER("Eraser");

    private final String displayName;

    DrawTool(String displayName) {
        this.displayName = displayName;
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
