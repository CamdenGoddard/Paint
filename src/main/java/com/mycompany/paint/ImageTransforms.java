package com.mycompany.paint;

import java.awt.image.BufferedImage;

/**
 * Rotating and mirroring a picture, pixel by pixel. This works on plain
 * {@link BufferedImage}s rather than on the canvas so it can be unit tested
 * without opening a window.
 *
 * <p>All rotations are clockwise. Mirror means a flip like a mirror would
 * give: horizontal swaps left and right, vertical swaps top and bottom.</p>
 */
final class ImageTransforms {

    /** The ways a picture can be turned over or around. */
    enum Op {
        ROTATE_90("Rotate 90°"),
        ROTATE_180("Rotate 180°"),
        ROTATE_270("Rotate 270°"),
        FLIP_HORIZONTAL("Flip horizontal"),
        FLIP_VERTICAL("Flip vertical");

        private final String label;

        Op(String label) {
            this.label = label;
        }

        /** @return the name shown in menus and written to the log */
        String label() {
            return label;
        }

        /** @return true if the result has its width and height swapped */
        boolean swapsSides() {
            return this == ROTATE_90 || this == ROTATE_270;
        }
    }

    /** Utility class - nobody should construct one. */
    private ImageTransforms() {
    }

    /**
     * Returns a new picture that is the given one rotated or mirrored.
     * The original is not changed.
     *
     * @param source the picture to transform
     * @param op     what to do to it
     * @return the transformed picture (ARGB); for a 90 or 270 degree
     *         rotation its width is the source's height and vice versa
     */
    static BufferedImage apply(BufferedImage source, Op op) {
        int w = source.getWidth();
        int h = source.getHeight();
        int outW = op.swapsSides() ? h : w;
        int outH = op.swapsSides() ? w : h;
        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = source.getRGB(x, y);
                switch (op) {
                    case ROTATE_90:
                        out.setRGB(h - 1 - y, x, argb);
                        break;
                    case ROTATE_180:
                        out.setRGB(w - 1 - x, h - 1 - y, argb);
                        break;
                    case ROTATE_270:
                        out.setRGB(y, w - 1 - x, argb);
                        break;
                    case FLIP_HORIZONTAL:
                        out.setRGB(w - 1 - x, y, argb);
                        break;
                    case FLIP_VERTICAL:
                    default:
                        out.setRGB(x, h - 1 - y, argb);
                        break;
                }
            }
        }
        return out;
    }
}
