package com.mycompany.paint;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Sprint 5: everything from Sprint 4 (undo/redo, clear canvas, polygons,
 * select/copy/paste/move, text) plus a star with a chosen number of points,
 * an autosave timer with an optional on-screen countdown, a small web server
 * that shows the tabs the user picks, and a warning when Save As would lose
 * data by switching file formats. See {@link ImageCanvasTab} for what
 * happens inside one tab, and {@link DrawTool} for what each tool does.
 *
 * <p>This class owns the toolbar controls and implements
 * {@link ToolSettings} so every open tab can read them without needing to
 * know they live here. It also owns the menus (the Edit menu simply
 * forwards to whichever tab is selected), the autosave countdown
 * ({@link AutosaveTimer}) and the web server ({@link ImageWebServer}).</p>
 */
public class PaintApp extends Application implements ToolSettings {

    private final TabPane tabPane = new TabPane();
    private final ComboBox<DrawTool> toolSelector =
            new ComboBox<>(FXCollections.observableArrayList(DrawTool.values()));
    private final ColorPicker colorPicker = new ColorPicker(Color.BLACK);
    private final Label colorInfoLabel = new Label();
    private final Slider widthSlider = new Slider(1, 20, 3);
    private final Label widthLabel = new Label();
    private final CheckBox dashedCheckBox = new CheckBox("Dashed");
    private final Spinner<Integer> sidesSpinner = makeIntSpinner(3, 100, 5);
    private final TextField textField = new TextField();
    private final Spinner<Integer> fontSizeSpinner = makeIntSpinner(6, 300, 32);
    private final Spinner<Integer> starPointsSpinner = makeIntSpinner(Geometry.MIN_STAR_POINTS, 100, 5);

    private final CheckBox autosaveCheckBox = new CheckBox("Autosave every");
    private final Spinner<Integer> autosaveSecondsSpinner = makeIntSpinner(AutosaveTimer.MIN_SECONDS, 3600, 60);
    private final CheckBox showCountdownCheckBox = new CheckBox("Show countdown");
    private final Label countdownLabel = new Label();
    private final Label autosaveStatusLabel = new Label();
    private final AutosaveTimer autosaveTimer =
            new AutosaveTimer(60, this::autosaveAllTabs);
    private final Timeline autosaveClock = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> tickAutosave()));

    private final CheckBox webServerCheckBox = new CheckBox("Web server on port");
    private final Spinner<Integer> webPortSpinner = makeIntSpinner(1024, 65535, 8080);
    private final CheckBox shareTabCheckBox = new CheckBox("Share this tab");
    private final Label webLinkLabel = new Label();
    private final ImageWebServer webServer = new ImageWebServer(new ImageWebServer.ImageSource() {
        @Override
        public List<String> names() {
            return onFxThread(() -> {
                List<String> names = new ArrayList<>();
                for (ImageCanvasTab shared : sharedTabs()) {
                    names.add(shared.getDisplayName());
                }
                return names;
            }, new ArrayList<>());
        }

        @Override
        public byte[] png(int index) {
            return onFxThread(() -> {
                List<ImageCanvasTab> shared = sharedTabs();
                return (index >= 0 && index < shared.size()) ? shared.get(index).toPngBytes() : null;
            }, null);
        }
    });

    private Stage window;

    /** Required by JavaFX - the real setup happens in {@link #start}, once the toolkit is ready. */
    public PaintApp() {
    }

    @Override
    public void start(Stage stage) {
        window = stage;

        toolSelector.getSelectionModel().selectFirst(); // DrawTool.PENCIL

        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateWidthLabel());
        colorPicker.valueProperty().addListener((obs, oldVal, newVal) -> updateColorLabel());
        updateWidthLabel();
        updateColorLabel();

        addNewTab(); // start with one blank canvas, same as Sprint 1/2 did

        HBox toolRow1 = new HBox(10, new Label("Tool:"), toolSelector, colorPicker, colorInfoLabel);
        toolRow1.setPadding(new Insets(5, 5, 2, 5));

        Button clearButton = new Button("Clear Canvas");
        clearButton.setOnAction(event -> clearCanvas());

        HBox toolRow2 = new HBox(10, widthLabel, widthSlider, dashedCheckBox, clearButton);
        toolRow2.setPadding(new Insets(2, 5, 2, 5));

        textField.setPromptText("Text for the Text tool");
        textField.setPrefColumnCount(14);
        sidesSpinner.setPrefWidth(75);
        starPointsSpinner.setPrefWidth(75);
        fontSizeSpinner.setPrefWidth(80);
        HBox toolRow3 = new HBox(10,
                new Label("Polygon sides:"), sidesSpinner,
                new Label("Star points:"), starPointsSpinner,
                new Label("Text:"), textField,
                new Label("Text size:"), fontSizeSpinner);
        toolRow3.setPadding(new Insets(2, 5, 2, 5));

        setUpAutosave();
        autosaveSecondsSpinner.setPrefWidth(85);
        HBox toolRow4 = new HBox(10, autosaveCheckBox, autosaveSecondsSpinner, new Label("seconds"),
                showCountdownCheckBox, countdownLabel, autosaveStatusLabel);
        toolRow4.setPadding(new Insets(2, 5, 2, 5));

        setUpWebServer();
        webPortSpinner.setPrefWidth(90);
        HBox toolRow5 = new HBox(10, webServerCheckBox, webPortSpinner, shareTabCheckBox, webLinkLabel);
        toolRow5.setPadding(new Insets(2, 5, 5, 5));

        VBox top = new VBox(makeMenuBar(), toolRow1, toolRow2, toolRow3, toolRow4, toolRow5);

        BorderPane layout = new BorderPane();
        layout.setTop(top);
        layout.setCenter(tabPane);

        Scene scene = new Scene(layout, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Paint");

        // Catches the window's X button too, not just File > Exit.
        stage.setOnCloseRequest(event -> {
            if (anyUnsavedChanges() && !confirmDiscard("exiting")) {
                event.consume();
            }
        });

        stage.show();
    }

    /** Called by JavaFX when the program is closing: shut down the clock and the web server. */
    @Override
    public void stop() {
        autosaveClock.stop();
        webServer.stop();
    }

    // ------------------------------------------------------------------
    // ToolSettings - lets every ImageCanvasTab read the shared toolbar
    // without needing to know it's PaintApp underneath.
    // ------------------------------------------------------------------

    @Override
    public DrawTool getTool() {
        return toolSelector.getValue();
    }

    @Override
    public void setTool(DrawTool tool) {
        toolSelector.setValue(tool);
    }

    @Override
    public Color getColor() {
        return colorPicker.getValue();
    }

    @Override
    public void setColor(Color color) {
        // Just changing the value is enough - the listener registered in
        // start() refreshes colorInfoLabel automatically, so the eyedropper
        // and a manual pick both keep the label in sync the same way.
        colorPicker.setValue(color);
    }

    @Override
    public double getLineWidth() {
        return widthSlider.getValue();
    }

    @Override
    public boolean isDashed() {
        return dashedCheckBox.isSelected();
    }

    @Override
    public int getPolygonSides() {
        return sidesSpinner.getValue();
    }

    @Override
    public int getStarPoints() {
        return starPointsSpinner.getValue();
    }

    @Override
    public String getText() {
        return textField.getText();
    }

    @Override
    public double getFontSize() {
        return fontSizeSpinner.getValue();
    }

    /**
     * Builds a whole-number spinner that can be typed into. A typed value
     * only counts once the spinner loses focus or Enter is pressed, so this
     * also commits it on focus loss - otherwise clicking straight onto the
     * canvas after typing a number would leave the old number in use. Text
     * that is not a number reverts to the last good value.
     */
    private static Spinner<Integer> makeIntSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);
        spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                try {
                    int typed = Integer.parseInt(spinner.getEditor().getText().trim());
                    spinner.getValueFactory().setValue(Math.max(min, Math.min(max, typed)));
                } catch (NumberFormatException ex) {
                    // fall through: put the last good value back in the box
                }
                spinner.getEditor().setText(String.valueOf(spinner.getValue()));
            }
        });
        return spinner;
    }

    /** Refreshes the "Width: N px" label from the slider's current value. */
    private void updateWidthLabel() {
        widthLabel.setText("Width: " + (int) widthSlider.getValue() + " px");
    }

    /** Refreshes the name/hex/rgb label from the color picker's current value. */
    private void updateColorLabel() {
        colorInfoLabel.setText(ColorNames.describe(colorPicker.getValue()));
    }

    // ------------------------------------------------------------------
    // Menu bar and keyboard shortcuts
    // ------------------------------------------------------------------

    /** Builds the File, Edit and Help menus, with keyboard shortcuts on their items. */
    private MenuBar makeMenuBar() {
        MenuItem newTabItem = new MenuItem("New Tab");
        newTabItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newTabItem.setOnAction(event -> addNewTab());

        MenuItem openItem = new MenuItem("Open");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(event -> openImage());

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(event -> save());

        MenuItem saveAsItem = new MenuItem("Save As");
        saveAsItem.setAccelerator(new KeyCodeCombination(
                KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(event -> saveAs());

        MenuItem closeItem = new MenuItem("Close Image");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
        closeItem.setOnAction(event -> closeCurrentImage());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(event -> {
            if (!anyUnsavedChanges() || confirmDiscard("exiting")) {
                window.close();
            }
        });

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(newTabItem, openItem, saveItem, saveAsItem, closeItem, exitItem);

        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        undoItem.setOnAction(event -> activeTab().undo());

        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        redoItem.setOnAction(event -> activeTab().redo());

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        cutItem.setOnAction(event -> activeTab().cutSelection());

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(event -> activeTab().copySelection());

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        pasteItem.setOnAction(event -> activeTab().pasteFromClipboard());

        MenuItem clearItem = new MenuItem("Clear Canvas");
        clearItem.setAccelerator(new KeyCodeCombination(
                KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        clearItem.setOnAction(event -> clearCanvas());

        Menu editMenu = new Menu("Edit");
        editMenu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem(),
                cutItem, copyItem, pasteItem, new SeparatorMenuItem(), clearItem);

        MenuItem helpItem = new MenuItem("Help");
        helpItem.setOnAction(event -> showHelp());

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(event -> showAbout());

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().addAll(helpItem, aboutItem);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, editMenu, helpMenu);
        return menuBar;
    }

    // ------------------------------------------------------------------
    // Tabs
    // ------------------------------------------------------------------

    /**
     * Creates a new blank tab, wires up its close behavior, and switches
     * to it. Used for the initial window and for File > New Tab.
     *
     * @return the newly created tab, in case a caller needs it
     */
    private ImageCanvasTab addNewTab() {
        ImageCanvasTab imageTab = new ImageCanvasTab(this);
        registerTab(imageTab);
        return imageTab;
    }

    /** Wires up close behavior, adds the tab to the TabPane, and selects it. */
    private void registerTab(ImageCanvasTab imageTab) {
        wireTabCloseRequest(imageTab);
        tabPane.getTabs().add(imageTab.getTab());
        tabPane.getSelectionModel().select(imageTab.getTab());
    }

    /**
     * Makes the tab's own close button ("x") go through the same
     * unsaved-changes check as File > Close Image, instead of just
     * closing immediately. This is the one place that logic lives -
     * {@link #closeCurrentImage()} below reuses it.
     */
    private void wireTabCloseRequest(ImageCanvasTab imageTab) {
        imageTab.getTab().setOnCloseRequest(event -> {
            event.consume(); // we decide what actually happens, below
            closeImageTab(imageTab);
        });
    }

    /**
     * @return the {@link ImageCanvasTab} behind whichever tab is currently selected
     */
    private ImageCanvasTab activeTab() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        return (ImageCanvasTab) selected.getUserData();
    }

    /**
     * @return true if any open tab - not just the current one - has unsaved changes
     */
    private boolean anyUnsavedChanges() {
        for (Tab tab : tabPane.getTabs()) {
            if (((ImageCanvasTab) tab.getUserData()).isUnsaved()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // File menu actions
    // ------------------------------------------------------------------

    /**
     * Lets the user pick an image file and opens it into a brand new tab,
     * leaving every already-open tab untouched - so unlike Sprint 2,
     * there's nothing to lose here and nothing to confirm first.
     */
    private void openImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image files", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File file = chooser.showOpenDialog(window);

        if (file == null) {
            return; // the user pressed Cancel
        }

        Image image = new Image(file.toURI().toString());
        if (image.isError()) {
            showMessage("That file could not be opened as an image.");
            return;
        }

        ImageCanvasTab newTab = new ImageCanvasTab(this, file, image);
        registerTab(newTab);
    }

    /** Saves the active tab to the file it already has, or Save As if it doesn't have one yet. */
    private void save() {
        ImageCanvasTab current = activeTab();
        if (current.getCurrentFile() == null) {
            saveAs();
            return;
        }
        writeAndReport(current, current.getCurrentFile());
    }

    /**
     * Asks the user where to save, then saves there. If the chosen format
     * is different from the picture's original format and could lose
     * something (JPEG, BMP), a warning explains what and asks first.
     */
    private void saveAs() {
        ImageCanvasTab current = activeTab();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Image As");
        FileChooser.ExtensionFilter png = new FileChooser.ExtensionFilter("PNG image", "*.png");
        FileChooser.ExtensionFilter jpg = new FileChooser.ExtensionFilter("JPEG image", "*.jpg", "*.jpeg");
        FileChooser.ExtensionFilter bmp = new FileChooser.ExtensionFilter("BMP image", "*.bmp");
        chooser.getExtensionFilters().addAll(png, jpg, bmp);
        switch (current.getOriginalFormat()) {
            case "jpg":
                chooser.setSelectedExtensionFilter(jpg);
                break;
            case "bmp":
                chooser.setSelectedExtensionFilter(bmp);
                break;
            default:
                chooser.setSelectedExtensionFilter(png);
                break;
        }
        File file = chooser.showSaveDialog(window);

        if (file == null) {
            return;
        }

        // A name with no extension (or one we cannot write) gets the extension
        // of the filter that was selected, so the file name matches its contents.
        String typed = ImageFormats.extensionOf(file);
        if (typed == null || !(typed.equals("png") || typed.equals("jpg")
                || typed.equals("jpeg") || typed.equals("bmp"))) {
            FileChooser.ExtensionFilter chosen = chooser.getSelectedExtensionFilter();
            String pattern = (chosen == null) ? "*.png" : chosen.getExtensions().get(0);
            file = new File(file.getPath() + pattern.substring(1));
        }

        String warning = ImageFormats.warningFor(current.getOriginalFormat(),
                ImageFormats.extensionOf(file), current.hasTransparency());
        if (warning != null && !confirmFormatLoss(warning)) {
            return;
        }
        writeAndReport(current, file);
    }

    /**
     * Shows the data-loss warning for a format change.
     *
     * @param warning the explanation from {@link ImageFormats#warningFor}
     * @return true if the user chose to save anyway
     */
    private boolean confirmFormatLoss(String warning) {
        ButtonType saveAnyway = new ButtonType("Save anyway", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(AlertType.WARNING, warning, saveAnyway, ButtonType.CANCEL);
        alert.setTitle("Image data may be lost");
        alert.setHeaderText("Changing the file format");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == saveAnyway;
    }

    /**
     * Runs {@link ImageCanvasTab#writeToFile}, shows an error message if it
     * fails, and restarts the autosave countdown when it works (the picture
     * was just saved, so there is nothing for autosave to do yet).
     */
    private void writeAndReport(ImageCanvasTab tab, File file) {
        try {
            tab.writeToFile(file);
            autosaveTimer.reset();
            updateCountdownLabel();
        } catch (IOException e) {
            showMessage("The image could not be saved: " + e.getMessage());
        }
    }

    /**
     * Closes the currently active tab (or, if it's the only one open,
     * resets it to blank instead - a Paint window always keeps at least
     * one tab). Checks for unsaved changes first, same as clicking the
     * tab's own close button.
     */
    private void closeCurrentImage() {
        closeImageTab(activeTab());
    }

    /**
     * Asks "are you sure", then wipes the active tab's canvas to blank.
     * The wipe is an ordinary edit, so Undo can still bring the picture back.
     */
    private void clearCanvas() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Clear Canvas");
        alert.setHeaderText(null);
        alert.setContentText("Clear the whole canvas? (You can still undo this with Edit > Undo.)");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            activeTab().clearCanvas();
        }
    }

    /** The shared logic behind both File > Close Image and a tab's own close button. */
    private void closeImageTab(ImageCanvasTab imageTab) {
        if (imageTab.isUnsaved() && !confirmDiscard("closing this image")) {
            return;
        }
        if (tabPane.getTabs().size() <= 1) {
            imageTab.resetToBlank();
        } else {
            tabPane.getTabs().remove(imageTab.getTab());
        }
    }

    // ------------------------------------------------------------------
    // Autosave
    // ------------------------------------------------------------------

    /** Connects the autosave controls to the timer and starts the one-second clock. */
    private void setUpAutosave() {
        autosaveCheckBox.selectedProperty().addListener((obs, was, isOn) -> {
            if (isOn) {
                autosaveTimer.setIntervalSeconds(autosaveSecondsSpinner.getValue());
                autosaveTimer.start();
            } else {
                autosaveTimer.stop();
            }
            updateCountdownLabel();
        });
        autosaveSecondsSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            autosaveTimer.setIntervalSeconds(newValue);
            updateCountdownLabel();
        });
        showCountdownCheckBox.selectedProperty().addListener((obs, was, isOn) -> updateCountdownLabel());

        autosaveClock.setCycleCount(Timeline.INDEFINITE);
        autosaveClock.play();
        updateCountdownLabel();
    }

    /** Runs once a second: counts the timer down and refreshes the label. */
    private void tickAutosave() {
        autosaveTimer.tick();
        updateCountdownLabel();
    }

    /** Shows or hides the countdown, depending on the "Show countdown" box and whether autosave is on. */
    private void updateCountdownLabel() {
        boolean visible = showCountdownCheckBox.isSelected() && autosaveTimer.isRunning();
        countdownLabel.setVisible(visible);
        countdownLabel.setManaged(visible);
        countdownLabel.setText("Next autosave in " + AutosaveTimer.format(autosaveTimer.getRemainingSeconds()));
    }

    /**
     * Saves every tab that has unsaved changes. A tab that already has a
     * file is saved to that file; a tab that has never been saved gets a
     * backup copy in a PaintAutosave folder in the user's home folder (the
     * tab itself stays "unsaved"). A tab in the middle of a mouse drag is
     * skipped and picked up on the next round.
     */
    private void autosaveAllTabs() {
        int saved = 0;
        int number = 0;
        String failure = null;

        for (Tab tab : tabPane.getTabs()) {
            number++;
            ImageCanvasTab imageTab = (ImageCanvasTab) tab.getUserData();
            if (!imageTab.isUnsaved() || imageTab.isBusy()) {
                continue;
            }
            try {
                if (imageTab.getCurrentFile() != null) {
                    imageTab.writeToFile(imageTab.getCurrentFile());
                } else {
                    File folder = new File(System.getProperty("user.home"), "PaintAutosave");
                    folder.mkdirs();
                    imageTab.writeCopy(new File(folder, "untitled-" + number + ".png"));
                }
                saved++;
            } catch (IOException e) {
                failure = e.getMessage();
            }
        }

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm:ss a"));
        autosaveStatusLabel.setText(failure != null
                ? "Autosave failed at " + time + ": " + failure
                : "Last autosave: " + time + " (" + saved + " saved)");
    }

    // ------------------------------------------------------------------
    // Web server
    // ------------------------------------------------------------------

    /** Connects the web server controls: starting/stopping it and choosing which tabs to share. */
    private void setUpWebServer() {
        webServerCheckBox.setOnAction(event -> {
            if (webServerCheckBox.isSelected()) {
                try {
                    webServer.start(webPortSpinner.getValue());
                    webPortSpinner.setDisable(true);
                    webLinkLabel.setText("Open http://localhost:" + webServer.getPort() + "/");
                } catch (IOException e) {
                    webServerCheckBox.setSelected(false);
                    showMessage("The web server could not start on port " + webPortSpinner.getValue()
                            + " (is something else using it?): " + e.getMessage());
                }
            } else {
                webServer.stop();
                webPortSpinner.setDisable(false);
                webLinkLabel.setText("");
            }
        });

        shareTabCheckBox.setOnAction(event -> activeTab().setShared(shareTabCheckBox.isSelected()));
        // Each tab remembers whether it is shared, so the box follows the selected tab.
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) ->
                shareTabCheckBox.setSelected(newTab != null && ((ImageCanvasTab) newTab.getUserData()).isShared()));
    }

    /** @return the tabs the user chose to share, in tab order. Call on the JavaFX thread. */
    private List<ImageCanvasTab> sharedTabs() {
        List<ImageCanvasTab> shared = new ArrayList<>();
        for (Tab tab : tabPane.getTabs()) {
            ImageCanvasTab imageTab = (ImageCanvasTab) tab.getUserData();
            if (imageTab.isShared()) {
                shared.add(imageTab);
            }
        }
        return shared;
    }

    /**
     * Runs a job on the JavaFX thread and waits for the answer. The web
     * server answers requests on its own threads, but tabs and canvases may
     * only be touched from the JavaFX thread.
     *
     * @param job      what to run
     * @param fallback the answer to give if the job fails or takes too long
     * @return the job's result, or the fallback
     */
    private static <T> T onFxThread(Callable<T> job, T fallback) {
        try {
            if (Platform.isFxApplicationThread()) {
                return job.call();
            }
            FutureTask<T> task = new FutureTask<>(job);
            Platform.runLater(task);
            return task.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------

    /**
     * Asks "are you sure", since there are unsaved changes.
     *
     * @param action describes what's about to happen, e.g. "exiting"
     * @return true if the user chose to go ahead anyway
     */
    private boolean confirmDiscard(String action) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText("You have unsaved changes. Continue " + action + " without saving?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /** Shows the Help menu's usage instructions. */
    private void showHelp() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText(null);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); // keep long help text from being cut off
        alert.setContentText(
                "File > Open loads a picture into a new tab; File > New Tab starts a blank one.\n"
                + "Pick a tool from the Tool dropdown. Pencil and Eraser draw as you drag; every\n"
                + "shape (Line, Square, Rectangle, Circle, Ellipse, both Triangles, Regular Polygon,\n"
                + "Star) previews as you drag and locks in when you release the mouse.\n"
                + "Right Triangle: the right angle is where you press the mouse.\n"
                + "Regular Polygon: press at the center and drag outward; set the number of sides\n"
                + "with the \"Polygon sides\" box (3 sides gives an equilateral triangle).\n"
                + "Star: drag out a box; set the number of points (4 or more) with \"Star points\".\n"
                + "Four points makes a diamond.\n"
                + "Text: type into the Text box, pick the Text tool, then drag - the text follows\n"
                + "the mouse and is placed where you let go.\n"
                + "Select: drag a rectangle, then drag inside it to move that piece. Edit > Copy,\n"
                + "Cut and Paste work on the selection; a pasted piece can be dragged into place\n"
                + "right away. Click outside the piece to put it down.\n"
                + "Edit > Undo / Redo step through your edits (up to 40 per tab).\n"
                + "Clear Canvas (button or Edit menu) asks first, and can be undone.\n"
                + "Autosave: tick \"Autosave every\", set the seconds, and tick \"Show countdown\" to\n"
                + "see the timer. Saving by hand restarts the countdown.\n"
                + "Web server: tick \"Web server on port\", tick \"Share this tab\" on the tabs to show,\n"
                + "then open the link in a browser. Nothing shared shows a message instead.\n"
                + "Save As to a different format (JPEG, BMP) warns first, because transparency and\n"
                + "exact colors can be lost.\n"
                + "Pick a color and line width - the label by the color swatch shows its name,\n"
                + "hex code, and RGB value. \"Dashed\" draws dashed lines and outlines.\n"
                + "Color Grabber sets the color from a pixel you click. Right-clicking does the\n"
                + "same at any time, even mid-drag on a shape you're drawing.\n"
                + "Shortcuts: Ctrl+N New Tab, Ctrl+O Open, Ctrl+S Save, Ctrl+Shift+S Save As,\n"
                + "Ctrl+W Close Image, Ctrl+Z Undo, Ctrl+Y Redo, Ctrl+X/C/V Cut/Copy/Paste,\n"
                + "Ctrl+Shift+C Clear Canvas.");
        alert.showAndWait();
    }

    /** About opens in its own small window, separate from the Help alert. */
    private void showAbout() {
        Stage aboutStage = new Stage();
        aboutStage.initOwner(window);
        aboutStage.setTitle("About");

        Label text = new Label(
                "Camden's Pain(t)\nCS 250 term project\nby Camden Goddard\n\n"
                + "Written in Java, so it's fully brewed and object-oriented.");
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> aboutStage.close());

        VBox box = new VBox(10, text, closeButton);
        box.setPadding(new Insets(15));

        aboutStage.setScene(new Scene(box));
        aboutStage.show();
    }

    /** Shows a plain informational popup with the given message. */
    private void showMessage(String text) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
