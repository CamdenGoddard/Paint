# Test plan: saving in a different file format

**Claim to prove:** saving a picture in a different format can lose features
or data (JPEG: exact colors and transparency; BMP: transparency), the program
warns about it before saving, and a format that cannot lose anything (PNG)
does not trigger the warning.

## 1. Automated proof (unit tests)

`ImageFormatsTest` builds one small picture with three kinds of pixel:

| pixel | what it is |
|-------|------------|
| (1,1) | opaque pure red `FFFF0000` |
| (2,2) | half see-through blue `800000FF` |
| (3,3) | fully see-through |

It saves the picture through the same code the program uses
(`ImageFormats.write`), reads the file back, and compares the pixels:

| test | what it shows |
|------|---------------|
| `pngKeepsEveryPixelExactlyIncludingTransparency` | every pixel, including the see-through ones, comes back identical |
| `bmpLosesTransparencyClearPixelsTurnWhite` | the fully clear pixel comes back white and the half-clear pixel comes back opaque; opaque red survives exactly |
| `jpegLosesTransparencyAndExactColors` | no alpha channel is left, and the pure red pixel no longer comes back as pure red |
| `warningIsGivenOnlyWhenAFormatChangeCanLoseSomething` | PNG to PNG, JPG to JPEG, and anything to PNG give no warning; PNG to JPG and PNG to BMP do, and say "WILL turn white" when the picture has transparency |

Run with `mvn test` (or Run Tests in IntelliJ). All should pass.

## 2. Manual demonstration in the program

1. Start the program. Draw a few shapes with the pencil and shape tools.
2. File > Save As, choose **PNG**, name it `demo.png`. No warning appears
   (the picture started as PNG).
3. File > Save As again, choose **JPEG image**, name it `demo.jpg`.
   - **Expected:** a warning titled "Image data may be lost" explaining that
     JPEG is lossy and cannot store transparency, with **Save anyway** and
     **Cancel**. Press Cancel: no file is written.
   - Repeat and press **Save anyway**: `demo.jpg` is written.
4. Open `demo.png` and `demo.jpg` in two tabs (File > Open) and use the Color
   Grabber on the same edge pixel in each. The color label shows slightly
   different RGB values in the JPEG tab. That is the data loss.
5. To see transparency loss: open a PNG that has see-through areas (for
   example a logo with no background), then Save As **BMP**. The warning now
   adds "This image has transparent areas, so they WILL turn white." After
   saving, open the BMP: the see-through areas are white.
6. Save As **PNG** from the JPEG tab: no warning (nothing more can be lost),
   but the PNG keeps the JPEG's already-lost detail, which shows the data
   is not recoverable.

## 3. Other features in this sprint

| feature | how to check |
|---------|--------------|
| Star points | pick Star, set "Star points" to 4 (diamond), 5, 8, 20; drag a box |
| Autosave | tick Autosave, set 5 seconds, draw on a tab that has a file, wait: the tab's `*` disappears and "Last autosave" updates |
| Countdown | tick Show countdown; it counts down; untick and it disappears; Save restarts it |
| Web server | tick Web server, open the link: message says nothing is shared; tick Share this tab; reload: the picture appears; untick it: the message returns |
