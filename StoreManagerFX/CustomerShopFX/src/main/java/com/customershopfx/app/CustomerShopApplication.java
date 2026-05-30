package com.customershopfx.app;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.stage.Stage;

public class CustomerShopApplication extends Application {
    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        SceneManager.init(stage);
        SceneManager.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
