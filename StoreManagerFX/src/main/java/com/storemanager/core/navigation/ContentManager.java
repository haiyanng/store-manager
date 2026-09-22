package com.storemanager.core.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class ContentManager {

    private static StackPane contentArea;

    public static void initialize(
            StackPane centerPane
    ) {

        contentArea = centerPane;
    }

    public static void loadContent(
            String fxmlPath
    ) {

        if (contentArea == null) {
            throw new RuntimeException(
                    "ContentManager is not initialized"
            );
        }

        try {

            var resource =
                    ContentManager.class.getResource(fxmlPath);

            if (resource == null) {
                throw new RuntimeException(
                        "FXML content not found: " + fxmlPath
                );
            }

            Parent content =
                    FXMLLoader.load(resource);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot load content: " + fxmlPath,
                    e
            );
        }
    }
}
