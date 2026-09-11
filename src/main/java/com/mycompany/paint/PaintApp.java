package com.mycompany.paint;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

// Sprint 1: open an image and show it, menu bar, Save, Save As, Close Image, Exit.
// No drawing tools yet, that's for a later sprint.
public class PaintApp extends Application {

    // The image that is open right now. null means nothing is open.
    private Image currentImage;

    // The file the current image came from (or was last saved to).
    // null means it has never been saved, so Save has to act like Save As.
    private File currentFile;

    // The control that actually shows the picture on the screen.
    private ImageView imageView = new ImageView();

    // The main window. Kept in a field so the file dialogs can attach to it.
    private Stage window;

    @Override
    public void start(Stage stage) {
        window = stage;

        // A ScrollPane so an image bigger than the window can be scrolled.
        ScrollPane scrollPane = new ScrollPane(imageView);

        // BorderPane has slots (top, bottom, left, right, center).
        // Menu bar goes in the top slot, the picture goes in the center.
        BorderPane layout = new BorderPane();
        layout.setTop(makeMenuBar());
        layout.setCenter(scrollPane);

        Scene scene = new Scene(layout, 900, 650);
        stage.setScene(scene);
        stage.setTitle("Paint");
        stage.show();
    }

    // Builds the File menu and puts it in a menu bar.
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
        exitItem.setOnAction(event -> window.close());

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().add(openItem);
        fileMenu.getItems().add(saveItem);
        fileMenu.getItems().add(saveAsItem);
        fileMenu.getItems().add(closeItem);
        fileMenu.getItems().add(exitItem);

        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    // Ask the user to pick a file, load it as an image, and show it.
    private void openImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Image");
        File file = chooser.showOpenDialog(window);

        if (file == null) {
            return; // the user pressed Cancel
        }

        Image image = new Image(file.toURI().toString());
        if (image.isError()) {
            showMessage("That file could not be opened as an image.");
            return;
        }

        currentImage = image;
        currentFile = file;
        imageView.setImage(currentImage);
        window.setTitle("Paint - " + file.getName());
    }

    // Save to the file we already have. If there isn't one yet, do Save As.
    private void save() {
        if (currentImage == null) {
            showMessage("There is no image to save.");
            return;
        }
        if (currentFile == null) {
            saveAs();
            return;
        }
        writeToFile(currentFile);
    }

    // Ask the user where to save, then save there and remember that file.
    private void saveAs() {
        if (currentImage == null) {
            showMessage("There is no image to save.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Image As");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG image", "*.png"));
        File file = chooser.showSaveDialog(window);

        if (file == null) {
            return;
        }

        writeToFile(file);
        currentFile = file;
        window.setTitle("Paint - " + file.getName());
    }

    // Write the current image to disk as a PNG.
    private void writeToFile(File file) {
        try {
            // JavaFX images can't be written to a file directly, so first
            // convert to a java.awt BufferedImage, which ImageIO can write.
            BufferedImage buffer = SwingFXUtils.fromFXImage(currentImage, null);
            ImageIO.write(buffer, "png", file);
        } catch (IOException e) {
            showMessage("The image could not be saved: " + e.getMessage());
        }
    }

    // Forget the current image and clear the screen.
    private void closeImage() {
        currentImage = null;
        currentFile = null;
        imageView.setImage(null);
        window.setTitle("Paint");
    }

    // A small pop-up box for error or info messages.
    private void showMessage(String text) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
