package com.mycompany.paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Tests for the polygon and star corner math. */
public class GeometryTest {

    private static final double EPSILON = 1e-9;

    @Test
    public void starHasTwoCornersPerPoint() {
        for (int points = 4; points <= 12; points++) {
            double[][] corners = Geometry.star(0, 0, 100, 100, points);
            assertEquals(2 * points, corners[0].length);
            assertEquals(2 * points, corners[1].length);
        }
    }

    @Test
    public void starPointsNeverGoBelowFour() {
        assertEquals(8, Geometry.star(0, 0, 50, 50, 2)[0].length);
    }

    @Test
    public void firstStarPointIsStraightUp() {
        double[][] corners = Geometry.star(200, 300, 80, 60, 5);
        assertEquals(200, corners[0][0], EPSILON);
        assertEquals(300 - 60, corners[1][0], EPSILON);
    }

    @Test
    public void fourPointStarIsADiamond() {
        // The inner corners must sit exactly on the straight line between the
        // neighbouring outer corners, so the outline is a plain diamond.
        double[][] c = Geometry.star(0, 0, 100, 100, 4);
        for (int inner = 1; inner < 8; inner += 2) {
            int before = inner - 1;
            int after = (inner + 1) % 8;
            double midX = (c[0][before] + c[0][after]) / 2;
            double midY = (c[1][before] + c[1][after]) / 2;
            assertEquals(midX, c[0][inner], 1e-6);
            assertEquals(midY, c[1][inner], 1e-6);
        }
    }

    @Test
    public void fivePointStarKeepsClassicProportions() {
        assertEquals(0.382, Geometry.starInnerRatio(5), EPSILON);
        assertEquals(0.382, Geometry.starInnerRatio(9), EPSILON);
    }

    @Test
    public void regularPolygonCornersAreEquallyFarFromCenter() {
        double[][] corners = Geometry.regularPolygon(100, 100, 160, 180, 7);
        assertNotNull(corners);
        assertEquals(7, corners[0].length);
        double radius = Math.hypot(60, 80);
        for (int i = 0; i < 7; i++) {
            assertEquals(radius, Math.hypot(corners[0][i] - 100, corners[1][i] - 100), 1e-6);
        }
    }

    @Test
    public void regularPolygonFirstCornerFollowsTheMouse() {
        double[][] corners = Geometry.regularPolygon(0, 0, 30, 40, 5);
        assertEquals(30, corners[0][0], 1e-6);
        assertEquals(40, corners[1][0], 1e-6);
    }

    @Test
    public void regularPolygonNeedsAtLeastThreeSidesAndSomeDistance() {
        assertEquals(3, Geometry.regularPolygon(0, 0, 10, 0, 1)[0].length);
        assertNull(Geometry.regularPolygon(5, 5, 5.2, 5.2, 6));
    }
}
