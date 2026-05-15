package com.storemanager.app;

import javafx.application.Application;
import javafx.stage.Stage;

public class AppLauncher
        extends Application {

    @Override
    public void start(
            Stage stage
    ) {

        stage.setTitle("StoreManagerFX");

        AppBootstrap.start(stage);
    }

    public static void main(
            String[] args
    ) {

        launch(args);
    }
}