package com.mycompany.paint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for saving in a different file format. These are the proof that a
 * format change really can lose data (see TEST_PLAN.md): they save the same
 * picture as PNG, JPEG and BMP, read each file back, and compare pixels.
 */
public class ImageFormatsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** A picture with an opaque pure-red pixel, a half-see-through blue pixel and a fully clear pixel. */
    private static BufferedImage samplePicture() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 1, 0xFFFF0000);
        image.setRGB(2, 2, 0x800000FF);
        image.setRGB(3, 3, 0x00000000);
        return image;
    }

    private BufferedImage saveAndReload(String name) throws IOException {
        File file = folder.newFile(name);
        file.delete();
        ImageFormats.write(samplePicture(), file);
        BufferedImage back = ImageIO.read(file);
        assertNotNull(back);
        return back;
    }

    @Test
    public void pngKeepsEveryPixelExactlyIncludingTransparency() throws IOException {
        BufferedImage original = samplePicture();
        BufferedImage back = saveAndReload("same.png");
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                assertEquals("pixel " + x + "," + y, original.getRGB(x, y), back.getRGB(x, y));
            }
        }
    }

    @Test
    public void bmpLosesTransparencyClearPixelsTurnWhite() throws IOException {
        BufferedImage back = saveAndReload("lost.bmp");
        assertEquals(0xFFFFFFFF, back.getRGB(3, 3));        // was fully clear
        assertEquals(0xFFFF0000, back.getRGB(1, 1));        // opaque colors survive BMP exactly
        assertNotEquals(0x800000FF, back.getRGB(2, 2));     // the half-clear pixel is no longer half-clear
        assertEquals(0xFF, back.getRGB(2, 2) >>> 24);
    }

    @Test
    public void jpegLosesTransparencyAndExactColors() throws IOException {
        BufferedImage back = saveAndReload("lost.jpg");
        assertEquals(0xFF, back.getRGB(3, 3) >>> 24);       // no alpha channel any more
        // A single saturated red pixel in a mostly clear picture cannot survive lossy compression.
        assertNotEquals(0xFFFF0000, back.getRGB(1, 1));
    }

    @Test
    public void warningIsGivenOnlyWhenAFormatChangeCanLoseSomething() {
        assertNull(ImageFormats.warningFor("png", "png", true));
        assertNull(ImageFormats.warningFor("jpg", "jpeg", false));   // same format, spelled differently
        assertNull(ImageFormats.warningFor("jpg", "png", false));    // PNG keeps everything
        assertNull(ImageFormats.warningFor("bmp", "png", true));

        String toJpg = ImageFormats.warningFor("png", "jpg", false);
        assertNotNull(toJpg);
        assertTrue(toJpg.contains("lossy"));
        assertTrue(toJpg.contains("transparency"));
        assertFalse(toJpg.contains("WILL turn white"));

        String toBmp = ImageFormats.warningFor("png", "bmp", true);
        assertNotNull(toBmp);
        assertTrue(toBmp.contains("WILL turn white"));
        assertFalse(toBmp.contains("lossy"));
    }

    @Test
    public void extensionsAreReadAndNormalized() {
        assertEquals("png", ImageFormats.extensionOf(new File("Drawing.PNG")));
        assertNull(ImageFormats.extensionOf(new File("drawing")));
        assertNull(ImageFormats.extensionOf(new File("drawing.")));
        assertEquals("jpg", ImageFormats.normalize("JPEG"));
        assertEquals("bmp", ImageFormats.normalize("bmp"));
        assertEquals("png", ImageFormats.normalize("gif"));
        assertEquals("png", ImageFormats.normalize(null));
    }
}
