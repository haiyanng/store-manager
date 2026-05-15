package com.storemanager.app;

import javafx.application.Application;
import javafx.stage.Stage;

public class AppLauncher
        extends Application {

    @Override
    public void start(
            Stage stage
    ) {

        System.out.println(
                "StoreManagerFX Started"
        );

        stage.setTitle(
                "StoreManagerFX"
        );

        stage.show();
    }

    public static void main(
            String[] args
    ) {

        launch(args);
    }
}