package com.mycompany.paint;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Small outline icons for the toolbar buttons, drawn from vector paths so
 * the program needs no image files. Every icon is designed on a 24 by 24
 * grid and drawn in a dark gray line.
 */
final class ToolIcons {

    private static final Color LINE = Color.web("#2b2b2b");

    /** Utility class - nobody should construct one. */
    private ToolIcons() {
    }

    /**
     * Returns the icon for a drawing tool.
     *
     * @param tool which tool
     * @return a 24 by 24 icon node
     */
    static Node forTool(DrawTool tool) {
        switch (tool) {
            case PENCIL:
                return icon("M4 20l1.2-5L16 4.2l3.8 3.8L9 18.8z M14 6.2l3.8 3.8");
            case LINE:
                return icon("M4 20L20 4");
            case SQUARE:
                return icon("M5 5h14v14H5z");
            case RECTANGLE:
                return icon("M2.5 7h19v10h-19z");
            case CIRCLE:
                return icon("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0");
            case ELLIPSE:
                return icon("M2 12a10 6 0 1 0 20 0a10 6 0 1 0 -20 0");
            case TRIANGLE:
                return icon("M12 4L21 20H3z");
            case RIGHT_TRIANGLE:
                return icon("M5 4v16h15z");
            case POLYGON:
                return icon(pathOf(Geometry.regularPolygon(12, 12, 22, 12, 6)));
            case STAR:
                return icon(pathOf(Geometry.star(12, 12, 10, 10, 5)));
            case EYEDROPPER:
                return icon("M19.5 4.5l-3 3 M16.5 7.5L8 16l-1 4 4-1 8.5-8.5z M14.5 5.5l4 4");
            case ERASER:
                return icon("M3 15l9-9 9 9-5 5H8z M7.5 10.5l9 9");
            case TEXT:
                return icon("M5 5h14 M12 5v15 M8.5 20h7");
            case SELECT:
                SVGPath dashed = path("M4 4h16v16H4z");
                dashed.getStrokeDashArray().addAll(3.0, 3.0);
                return new Group(dashed);
            default:
                return icon("M4 4h16v16H4z");
        }
    }

    /** Icon for the New Tab button. */
    static Node newTab() {
        return icon("M5 2.5h10l5 5v14H5z M15 2.5v5h5");
    }

    /** Icon for the Open button (a folder). */
    static Node open() {
        return icon("M2.5 6h7l2 2h10v12h-19z");
    }

    /** Icon for the Save button (a floppy disk). */
    static Node save() {
        return icon("M3 3h14l4 4v14H3z M7 3v6h9V3 M7 21v-7h10v7");
    }

    /** Icon for Undo (an arrow curving back). */
    static Node undo() {
        return icon("M9 6L4 11l5 5 M4 11h10a5 5 0 0 1 0 10h-4");
    }

    /** Icon for Redo (an arrow curving forward). */
    static Node redo() {
        return icon("M15 6l5 5-5 5 M20 11H10a5 5 0 0 0 0 10h4");
    }

    /** Icon for the rotate buttons: a circular arrow, with the angle written beside it by the button's text. */
    static Node rotate() {
        return icon("M19.5 12a7.5 7.5 0 1 1-2.6-5.7 M19.5 3.5v5h-5");
    }

    /** Icon for Flip Horizontal (left and right triangles mirrored across a line). */
    static Node flipHorizontal() {
        return icon("M12 3v18 M9 7L3 12l6 5z M15 7l6 5-6 5z");
    }

    /** Icon for Flip Vertical (top and bottom triangles mirrored across a line). */
    static Node flipVertical() {
        return icon("M3 12h18 M7 9l5-6 5 6z M7 15l5 6 5-6z");
    }

    /** Icon for Clear Canvas (a trash can). */
    static Node clear() {
        return icon("M4 7h16 M9 7V4h6v3 M6 7l1 14h10l1-14 M10 11v6 M14 11v6");
    }

    /** Icon for the web server toggle (a globe). */
    static Node web() {
        return icon("M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0 M3 12h18 M12 3c-4 4-4 14 0 18 M12 3c4 4 4 14 0 18");
    }

    /** Builds one outline icon from path data. */
    private static Node icon(String pathData) {
        return new Group(path(pathData));
    }

    private static SVGPath path(String pathData) {
        SVGPath shape = new SVGPath();
        shape.setContent(pathData);
        shape.setFill(null);
        shape.setStroke(LINE);
        shape.setStrokeWidth(1.8);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return shape;
    }

    /** Turns a list of corners into a closed path string. */
    private static String pathOf(double[][] corners) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < corners[0].length; i++) {
            text.append(i == 0 ? "M" : "L")
                    .append(String.format(java.util.Locale.ROOT, "%.2f %.2f", corners[0][i], corners[1][i]));
        }
        return text.append("z").toString();
    }
}
