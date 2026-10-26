package com.mycompany.paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.awt.image.BufferedImage;
import org.junit.Test;

/**
 * Tests for rotating and mirroring. The test picture is 3 wide and 2 tall,
 * with every pixel a different color so a wrong move is easy to spot:
 *
 * <pre>
 *   A B C
 *   D E F
 * </pre>
 */
public class ImageTransformsTest {

    private static final int A = 0xFF010000, B = 0xFF020000, C = 0xFF030000;
    private static final int D = 0xFF040000, E = 0xFF050000, F = 0xFF060000;

    private static BufferedImage sample() {
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        int[][] rows = { { A, B, C }, { D, E, F } };
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                image.setRGB(x, y, rows[y][x]);
            }
        }
        return image;
    }

    /** Checks a picture against rows of expected colors. */
    private static void assertPicture(int[][] rows, BufferedImage actual) {
        assertEquals("height", rows.length, actual.getHeight());
        assertEquals("width", rows[0].length, actual.getWidth());
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[0].length; x++) {
                assertEquals("pixel " + x + "," + y, rows[y][x], actual.getRGB(x, y));
            }
        }
    }

    @Test
    public void rotate90TurnsClockwiseAndSwapsWidthAndHeight() {
        // The left column (A over D) becomes the top row, read right to left.
        assertPicture(new int[][] { { D, A }, { E, B }, { F, C } },
                ImageTransforms.apply(sample(), ImageTransforms.Op.ROTATE_90));
    }

    @Test
    public void rotate180TurnsUpsideDown() {
        assertPicture(new int[][] { { F, E, D }, { C, B, A } },
                ImageTransforms.apply(sample(), ImageTransforms.Op.ROTATE_180));
    }

    @Test
    public void rotate270TurnsCounterClockwise() {
        assertPicture(new int[][] { { C, F }, { B, E }, { A, D } },
                ImageTransforms.apply(sample(), ImageTransforms.Op.ROTATE_270));
    }

    @Test
    public void flipHorizontalSwapsLeftAndRight() {
        assertPicture(new int[][] { { C, B, A }, { F, E, D } },
                ImageTransforms.apply(sample(), ImageTransforms.Op.FLIP_HORIZONTAL));
    }

    @Test
    public void flipVerticalSwapsTopAndBottom() {
        assertPicture(new int[][] { { D, E, F }, { A, B, C } },
                ImageTransforms.apply(sample(), ImageTransforms.Op.FLIP_VERTICAL));
    }

    @Test
    public void fourQuarterTurnsAreTheOriginalAgain() {
        BufferedImage image = sample();
        for (int i = 0; i < 4; i++) {
            image = ImageTransforms.apply(image, ImageTransforms.Op.ROTATE_90);
        }
        assertPicture(new int[][] { { A, B, C }, { D, E, F } }, image);
    }

    @Test
    public void turnsThatCancelOutCancelOut() {
        BufferedImage there = ImageTransforms.apply(sample(), ImageTransforms.Op.ROTATE_90);
        assertPicture(new int[][] { { A, B, C }, { D, E, F } },
                ImageTransforms.apply(there, ImageTransforms.Op.ROTATE_270));

        BufferedImage flipped = ImageTransforms.apply(sample(), ImageTransforms.Op.FLIP_HORIZONTAL);
        assertPicture(new int[][] { { A, B, C }, { D, E, F } },
                ImageTransforms.apply(flipped, ImageTransforms.Op.FLIP_HORIZONTAL));

        BufferedImage twice = ImageTransforms.apply(
                ImageTransforms.apply(sample(), ImageTransforms.Op.ROTATE_90), ImageTransforms.Op.ROTATE_90);
        assertPicture(new int[][] { { F, E, D }, { C, B, A } }, twice);
    }

    @Test
    public void transparencyIsKeptAndTheOriginalIsNotChanged() {
        BufferedImage image = sample();
        image.setRGB(0, 0, 0x00000000); // fully clear
        BufferedImage rotated = ImageTransforms.apply(image, ImageTransforms.Op.ROTATE_90);
        assertEquals(0x00000000, rotated.getRGB(1, 0));
        assertNotSame(image, rotated);
        assertEquals(0x00000000, image.getRGB(0, 0));
        assertEquals(3, image.getWidth());
    }
}
