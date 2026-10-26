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

## 4. Sprint 6 features

**Rotate and mirror**

| step | expected |
|------|----------|
| Draw a red line near the top-left. Press Ctrl+R | the canvas becomes taller than wide and the line is now a vertical line near the right edge |
| Press Ctrl+Z | the picture and its original size come back |
| Ctrl+Shift+R, Ctrl+L, Ctrl+H, Ctrl+Shift+H (undoing between) | 180 degrees (line bottom-right), 270 degrees (line down the left side), mirrored left-right, mirrored top-bottom |
| Pick Select, drag a box around part of the picture, press the 90 button | only that piece turns; the rest of the picture does not change; Ctrl+Z puts it back |
| Image > Selection > Rotate 90 with nothing selected | a message says to select something first |

`ImageTransformsTest` checks each of the five operations on a 3x2 picture whose
pixels are all different colors, that four quarter turns give back the original,
and that opposite turns cancel.

**Logging**

1. Run the program, draw a shape, choose a tool, save, then close it.
2. Open `PaintLog/paint-log.txt` in your home folder (Help > Show Log File Location).
3. Expected: one line per action in the form `date [file name] time message`,
   in the order you did things, ending with `Program closed`.

`ActionLoggerTest` checks the line format, appending across runs, that writing
happens on the separate `paint-logger` thread, that six threads logging at once
lose no lines and keep each thread's order, and that closing writes everything
still waiting.

**Icons, tooltips, notifications**

| feature | how to check |
|---------|--------------|
| Icons | every tool has an icon button; clicking one changes the Tool drop-down, and the reverse |
| Tooltips | rest the mouse on any button, box or spinner: a tip appears after a moment |
| Notifications | tick Autosave: an operating-system notification appears (Windows: bottom-right corner); untick Notifications and nothing appears |
