package com.mycompany.paint;

/**
 * The corner-point math behind the polygon and star tools, kept free of
 * any drawing code so it can be unit tested on its own.
 */
final class Geometry {

    /** The fewest points a star can have. With 4 the star is a diamond. */
    static final int MIN_STAR_POINTS = 4;

    /** Inner-to-outer radius ratio of a classic five-pointed star. */
    private static final double STAR_INNER_RATIO = 0.382;

    /** Utility class - nobody should construct one. */
    private Geometry() {
    }

    /**
     * Works out the corners of a regular polygon dragged out from its center.
     * The distance from the center to the mouse is the radius, and the first
     * corner points at the mouse.
     *
     * @param cx     center x
     * @param cy     center y
     * @param mouseX current mouse x
     * @param mouseY current mouse y
     * @param sides  number of sides; anything under 3 is treated as 3
     * @return {xs, ys} arrays of the corners, or null while the mouse is
     *         still on top of the center (nothing to draw yet)
     */
    static double[][] regularPolygon(double cx, double cy,
                                     double mouseX, double mouseY, int sides) {
        int n = Math.max(3, sides);
        double radius = Math.hypot(mouseX - cx, mouseY - cy);
        if (radius < 1) {
            return null;
        }
        double firstAngle = Math.atan2(mouseY - cy, mouseX - cx);

        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double angle = firstAngle + 2 * Math.PI * i / n;
            xs[i] = cx + radius * Math.cos(angle);
            ys[i] = cy + radius * Math.sin(angle);
        }
        return new double[][] { xs, ys };
    }

    /**
     * How far in the star's inner corners sit, as a fraction of the outer
     * radius. Four points is special-cased so the "star" is a plain diamond
     * (the inner corners land exactly on the straight edges between the outer
     * ones); five and up use the classic star proportion.
     *
     * @param points number of star points
     * @return the inner radius divided by the outer radius
     */
    static double starInnerRatio(int points) {
        if (points <= MIN_STAR_POINTS) {
            return Math.cos(Math.PI / MIN_STAR_POINTS);
        }
        return STAR_INNER_RATIO;
    }

    /**
     * Works out the corners of a star with the given number of points,
     * stretched to an ellipse with radii rx and ry. The first point is
     * straight up, and corners alternate outer, inner, outer, inner...
     *
     * @param cx     center x
     * @param cy     center y
     * @param rx     horizontal outer radius
     * @param ry     vertical outer radius
     * @param points number of points; anything under 4 is treated as 4
     * @return {xs, ys} arrays with 2 * points corners
     */
    static double[][] star(double cx, double cy, double rx, double ry, int points) {
        int n = Math.max(MIN_STAR_POINTS, points);
        double inner = starInnerRatio(n);

        double[] xs = new double[2 * n];
        double[] ys = new double[2 * n];
        for (int i = 0; i < 2 * n; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / n;
            double scale = (i % 2 == 0) ? 1.0 : inner;
            xs[i] = cx + rx * scale * Math.cos(angle);
            ys[i] = cy + ry * scale * Math.sin(angle);
        }
        return new double[][] { xs, ys };
    }
}
