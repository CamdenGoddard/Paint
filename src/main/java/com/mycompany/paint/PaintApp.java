package com.mycompany.paint;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

// Sprint 2: draw on the canvas with the mouse, pick a color and line width,
// open/save PNG/JPG/BMP, a Help menu, and a warning before losing unsaved work.
public class PaintApp extends Application {

    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;

    // The drawing surface. Both the opened picture and the mouse drawing
    // end up on this same canvas, so saving it saves both together.
    private Canvas canvas = new Canvas(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    private GraphicsContext gc = canvas.getGraphicsContext2D();

    // The file the canvas was last opened from or saved to. null = never saved.
    private File currentFile;

    // True any time the canvas has changes that have not been saved yet.
    private boolean unsavedChanges = false;

    private final ColorPicker colorPicker = new ColorPicker(Color.BLACK);
    private final Slider widthSlider = new Slider(1, 20, 3);
    private final Label widthLabel = new Label("Width: 3");

    private Stage window;

    @Override
    public void start(Stage stage) {
        window = stage;

        // start with a blank white canvas, like a fresh sheet of paper
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        setupDrawing();

        ScrollPane scrollPane = new ScrollPane(canvas);

        HBox toolBar = new HBox(10, colorPicker, widthLabel, widthSlider);
        toolBar.setPadding(new Insets(5));

        VBox top = new VBox(makeMenuBar(), toolBar);

        BorderPane layout = new BorderPane();
        layout.setTop(top);
        layout.setCenter(scrollPane);

        Scene scene = new Scene(layout, 900, 650);
        stage.setScene(scene);
        stage.setTitle("Paint");

        // catches the window's X button too, not just the Exit menu item
        stage.setOnCloseRequest(event -> {
            if (unsavedChanges && !confirmDiscard("closing")) {
                event.consume();
            }
        });

        stage.show();
    }

    // Lets the width slider show its current number, and hooks up drawing
    // with the mouse: press to start a line, drag to keep drawing it.
    private void setupDrawing() {
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                widthLabel.setText("Width: " + newVal.intValue()));

        canvas.setOnMousePressed(e -> {
            gc.setStroke(colorPicker.getValue());
            gc.setLineWidth(widthSlider.getValue());
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });

        canvas.setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            unsavedChanges = true;
        });
    }

    // Builds the File and Help menus and puts them in a menu bar.
    private MenuBar makeMenuBar() {
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(event -> openImage());

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(event -> save());

        MenuItem saveAsItem = new MenuItem("Save As");
        saveAsItem.setOnAction(event -> saveAs());

        MenuItem closeItem = new MenuItem("Close Image");
        closeItem.setOnAction(event -> closeImage());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(event -> {
            if (!unsavedChanges || confirmDiscard("exiting")) {
                window.close();
            }
        });

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().add(openItem);
        fileMenu.getItems().add(saveItem);
        fileMenu.getItems().add(saveAsItem);
        fileMenu.getItems().add(closeItem);
        fileMenu.getItems().add(exitItem);

        MenuItem helpItem = new MenuItem("Help");
        helpItem.setOnAction(event -> showHelp());

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(event -> showAbout());

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(helpItem);
        helpMenu.getItems().add(aboutItem);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(fileMenu);
        menuBar.getMenus().add(helpMenu);
        return menuBar;
    }

    // Ask the user to pick a file, load it as an image, and draw it on the canvas.
    private void openImage() {
        if (unsavedChanges && !confirmDiscard("opening a different picture")) {
            return;
        }

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

        canvas.setWidth(image.getWidth());
        canvas.setHeight(image.getHeight());
        gc.drawImage(image, 0, 0);

        currentFile = file;
        unsavedChanges = false;
        window.setTitle("Paint - " + file.getName());
    }

    // Save to the file we already have. If there isn't one yet, do Save As.
    private void save() {
        if (currentFile == null) {
            saveAs();
            return;
        }
        writeToFile(currentFile);
    }

    // Ask the user where to save, then save there and remember that file.
    private void saveAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Image As");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG image", "*.png"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JPEG image", "*.jpg", "*.jpeg"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("BMP image", "*.bmp"));
        File file = chooser.showSaveDialog(window);

        if (file == null) {
            return;
        }

        writeToFile(file);
        currentFile = file;
        window.setTitle("Paint - " + file.getName());
    }

    // Write whatever is currently on the canvas to disk, in the format
    // that matches the file's extension (defaults to PNG if there isn't one).
    private void writeToFile(File file) {
        String extension = extensionOf(file);
        if (extension == null) {
            extension = "png";
        }

        WritableImage snapshot = canvas.snapshot(null, null);
        BufferedImage picture = SwingFXUtils.fromFXImage(snapshot, null);

        // JPG and BMP don't support transparency, so flatten onto white first.
        if (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("bmp")) {
            BufferedImage flattened = new BufferedImage(
                    picture.getWidth(), picture.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = flattened.createGraphics();
            g.drawImage(picture, 0, 0, java.awt.Color.WHITE, null);
            g.dispose();
            picture = flattened;
        }

        try {
            ImageIO.write(picture, extension, file);
            unsavedChanges = false;
        } catch (IOException e) {
            showMessage("The image could not be saved: " + e.getMessage());
        }
    }

    // Lower-case file extension without the dot, or null if there isn't one.
    private static String extensionOf(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // Clear the canvas back to blank, after checking for unsaved changes.
    private void closeImage() {
        if (unsavedChanges && !confirmDiscard("closing this image")) {
            return;
        }

        canvas.setWidth(DEFAULT_WIDTH);
        canvas.setHeight(DEFAULT_HEIGHT);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        currentFile = null;
        unsavedChanges = false;
        window.setTitle("Paint");
    }

    // Asks "are you sure", since there are unsaved changes. true = go ahead anyway.
    private boolean confirmDiscard(String action) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText("You have unsaved changes. Continue " + action + " without saving?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showHelp() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText(null);
        alert.setContentText(
                "File > Open to load a picture, or just start drawing on the blank canvas.\n"
                + "Click and drag on the canvas to draw.\n"
                + "Pick a color and line width above the canvas.\n"
                + "File > Save / Save As to save as PNG, JPG, or BMP.");
        alert.showAndWait();
    }

    // About opens in its own small window, separate from the Help alert.
    private void showAbout() {
        Stage aboutStage = new Stage();
        aboutStage.initOwner(window);
        aboutStage.setTitle("About");

        Label text = new Label("Camden's Pain(t)\nCS 250 term project\nby Camden Goddard");
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> aboutStage.close());

        VBox box = new VBox(10, text, closeButton);
        box.setPadding(new Insets(15));

        aboutStage.setScene(new Scene(box));
        aboutStage.show();
    }

    private void showMessage(String text) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
