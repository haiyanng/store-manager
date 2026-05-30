package com.customershopfx.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class SceneManager {
    private static Stage stage;
    private static Scene authScene;
    private static AuthLayoutController authLayoutController;
    private static com.customershopfx.auth.view.LoginController loginController;
    private static com.customershopfx.auth.view.RegisterController registerController;
    private static Scene shopScene;

    private SceneManager() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Customer Shop");
        stage.setMinWidth(1050);
        stage.setMinHeight(700);
        stage.setWidth(1680);
        stage.setHeight(980);
    }

    public static void showLogin() {
        ensureAuthScene();
        authLayoutController.showLogin();
        showAuthScene();
    }

    public static void showRegister() {
        ensureAuthScene();
        authLayoutController.showRegister();
        showAuthScene();
    }

    public static void showShop() {
        try {
            if (shopScene == null) {
                Loaded<Parent, com.customershopfx.shop.view.ShopController> shop = load("/com/customershopfx/shop/ShopView.fxml");
                shopScene = new Scene(shop.root());
                addStylesheets(shopScene, "/com/customershopfx/css/app.css", "/com/customershopfx/css/shop.css");
            }
            stage.setScene(shopScene);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load shop shell", e);
        }
    }

    private static void ensureAuthScene() {
        if (authScene != null) {
            return;
        }
        try {
            Loaded<Parent, AuthLayoutController> layout = load("/com/customershopfx/app/AuthLayout.fxml");
            Loaded<Parent, com.customershopfx.auth.view.LoginController> login = load("/com/customershopfx/auth/LoginView.fxml");
            Loaded<Parent, com.customershopfx.auth.view.RegisterController> register = load("/com/customershopfx/auth/RegisterView.fxml");
            authLayoutController = layout.controller();
            loginController = login.controller();
            registerController = register.controller();
            authLayoutController.setViews(login.root(), register.root(), loginController, registerController);
            authScene = new Scene(layout.root());
            addStylesheets(authScene, "/com/customershopfx/css/app.css", "/com/customershopfx/css/auth.css");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load auth layout", e);
        }
    }

    private static void showAuthScene() {
        if (stage.getScene() != authScene) {
            stage.setScene(authScene);
        }
        stage.show();
    }

    private static void addStylesheets(Scene scene, String... stylesheets) {
        for (String stylesheet : stylesheets) {
            scene.getStylesheets().add(SceneManager.class.getResource(stylesheet).toExternalForm());
        }
    }

    private static <T> Loaded<Parent, T> load(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));
        Parent root = loader.load();
        return new Loaded<>(root, loader.getController());
    }

    private record Loaded<R, C>(R root, C controller) {
    }
}
