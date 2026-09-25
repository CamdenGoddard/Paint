package com.mycompany.paint;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Sprint 3: multiple images open at once via tabs, nine drawing tools
 * (freehand, straight line, five shapes, an eyedropper, and an eraser),
 * dashed outlines, keyboard shortcuts, and a color readout in hex/RGB/name
 * form. See {@link ImageCanvasTab} for what happens inside one tab, and
 * {@link DrawTool} for what each tool does.
 *
 * This class owns the toolbar controls - the tool selector, color
 * picker, width slider, and dashed checkbox - and implements
 * {@link ToolSettings} so every open tab can read them without needing to
 * know they live here.
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

        HBox toolRow2 = new HBox(10, widthLabel, widthSlider, dashedCheckBox);
        toolRow2.setPadding(new Insets(2, 5, 5, 5));

        VBox top = new VBox(makeMenuBar(), toolRow1, toolRow2);

        BorderPane layout = new BorderPane();
        layout.setTop(top);
        layout.setCenter(tabPane);

        Scene scene = new Scene(layout, 900, 700);
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

    // ------------------------------------------------------------------
    // ToolSettings - lets every ImageCanvasTab read the shared toolbar
    // without needing to know it's PaintApp underneath.
    // ------------------------------------------------------------------

    @Override
    public DrawTool getTool() {
        return toolSelector.getValue();
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

    /** Builds the File and Help menus, with keyboard shortcuts on the File items. */
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

        MenuItem helpItem = new MenuItem("Help");
        helpItem.setOnAction(event -> showHelp());

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(event -> showAbout());

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().addAll(helpItem, aboutItem);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, helpMenu);
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

    /** Asks the active tab's user where to save, then saves there. */
    private void saveAs() {
        ImageCanvasTab current = activeTab();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Image As");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPEG image", "*.jpg", "*.jpeg"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BMP image", "*.bmp"));
        File file = chooser.showSaveDialog(window);

        if (file == null) {
            return;
        }
        writeAndReport(current, file);
    }

    /** Runs {@link ImageCanvasTab#writeToFile} and shows an error message if it fails. */
    private void writeAndReport(ImageCanvasTab tab, File file) {
        try {
            tab.writeToFile(file);
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
        alert.setContentText(
                "File > Open loads a picture into a new tab; File > New Tab starts a blank one.\n"
                + "Pick a tool from the Tool dropdown. Pencil and Eraser draw as you drag;\n"
                + "Line, Square, Rectangle, Circle, Ellipse, and Triangle preview as you drag\n"
                + "and lock in when you release the mouse.\n"
                + "Pick a color and line width above the canvas - the label by the color\n"
                + "swatch shows its name, hex code, and RGB value.\n"
                + "Check \"Dashed\" to draw dashed lines and outlines instead of solid ones.\n"
                + "Color Grabber sets the active color from a pixel you click. Right-clicking\n"
                + "does the same thing at any time, even mid-drag on a shape you're drawing.\n"
                + "Shortcuts: Ctrl+N New Tab, Ctrl+O Open, Ctrl+S Save,\n"
                + "Ctrl+Shift+S Save As, Ctrl+W Close Image.");
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
