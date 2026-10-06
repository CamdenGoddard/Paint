package com.mycompany.paint;

import javafx.scene.canvas.GraphicsContext;

/**
 * Draws the outline of each shape tool onto a {@link GraphicsContext},
 * given the two points of a mouse drag.
 *
 * <p>This only knows geometry. The caller ({@link ImageCanvasTab}) has
 * already set the stroke color, width and dash pattern on the graphics
 * context, and decides when a drag is a live preview and when it is final.</p>
 */
final class ShapeDrawer {

    /** Utility class - nobody should construct one. */
    private ShapeDrawer() {
    }

    /**
     * Draws one shape's outline for a drag from (x1, y1) to (x2, y2).
     *
     * <p>How the drag is interpreted depends on the shape:</p>
     * <ul>
     *   <li>Line, Rectangle, Ellipse, Triangle and Star fit the drag's
     *       bounding box.</li>
     *   <li>Square and Circle force both sides equal, using the larger of
     *       the drag's width and height, and keep the corner the drag
     *       started from.</li>
     *   <li>Right Triangle puts the right angle exactly where the drag
     *       started; the two legs run horizontally and vertically to the
     *       mouse's x and y.</li>
     *   <li>Regular Polygon is dragged out from its center; the distance to
     *       the mouse is the radius and the first corner points at the
     *       mouse, so the polygon also rotates as you drag.</li>
     * </ul>
     *
     * @param gc    where to draw, with stroke settings already applied
     * @param tool  which shape to draw; non-shape tools are ignored
     * @param x1    drag start x
     * @param y1    drag start y
     * @param x2    drag end (current) x
     * @param y2    drag end (current) y
     * @param sides how many sides the Regular Polygon tool should have
     */
    static void draw(GraphicsContext gc, DrawTool tool,
                     double x1, double y1, double x2, double y2, int sides) {
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
                // Apex centered on top, base spanning the full width - an
                // isosceles triangle inscribed in the bounding box. It only
                // has a right angle in the one case where the box is exactly
                // twice as wide as it is tall; use Right Triangle for those.
                double[] xs = { minX + width / 2.0, minX, minX + width };
                double[] ys = { minY, minY + height, minY + height };
                gc.strokePolygon(xs, ys, 3);
                break;
            }

            case RIGHT_TRIANGLE: {
                // Right angle at the drag's start point; one leg runs
                // horizontally to x2, the other vertically to y2.
                double[] xs = { x1, x2, x1 };
                double[] ys = { y1, y1, y2 };
                gc.strokePolygon(xs, ys, 3);
                break;
            }

            case POLYGON:
                drawRegularPolygon(gc, x1, y1, x2, y2, sides);
                break;

            case STAR:
                drawStar(gc, minX, minY, width, height);
                break;

            default:
                // PENCIL, ERASER, EYEDROPPER, TEXT and SELECT never reach here.
                break;
        }
    }

    /**
     * Draws a regular polygon (all sides and angles equal) with the given
     * number of sides. With 3 sides this is an equilateral triangle, which
     * never has a right angle.
     */
    private static void drawRegularPolygon(GraphicsContext gc, double cx, double cy,
                                           double mouseX, double mouseY, int sides) {
        int n = Math.max(3, sides);
        double radius = Math.hypot(mouseX - cx, mouseY - cy);
        if (radius < 1) {
            return; // nothing to draw until the mouse moves away from the center
        }
        double firstAngle = Math.atan2(mouseY - cy, mouseX - cx);

        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double angle = firstAngle + 2 * Math.PI * i / n;
            xs[i] = cx + radius * Math.cos(angle);
            ys[i] = cy + radius * Math.sin(angle);
        }
        gc.strokePolygon(xs, ys, n);
    }

    /**
     * Draws a five-pointed star whose outer points reach the edges of the
     * given box (stretched to fit it, so a wide drag makes a wide star).
     * The ten corners alternate between the outer radius and an inner
     * radius of about 38% of it, which is what makes a classic star shape.
     */
    private static void drawStar(GraphicsContext gc, double x, double y, double w, double h) {
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        double rx = w / 2.0;
        double ry = h / 2.0;
        double innerRatio = 0.382;

        double[] xs = new double[10];
        double[] ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5; // first point straight up
            double scale = (i % 2 == 0) ? 1.0 : innerRatio;
            xs[i] = cx + rx * scale * Math.cos(angle);
            ys[i] = cy + ry * scale * Math.sin(angle);
        }
        gc.strokePolygon(xs, ys, 10);
    }
}
