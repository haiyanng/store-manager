package com.storemanager.core.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage primaryStage;

    public static void initialize(
            Stage stage
    ) {

        primaryStage = stage;
    }

    public static void switchScene(
            String fxmlPath
    ) {

        try {

            Parent root =
                    FXMLLoader.load(
                            SceneManager.class.getResource(fxmlPath)
                    );

            Scene scene =
                    new Scene(root);

            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Cannot load scene: " + fxmlPath,
                    e
            );
        }
    }
}