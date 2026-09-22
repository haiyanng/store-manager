package com.storemanager.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Optional;

public class ConfigService {

    private static final String CONFIG_FILE_NAME =
            "app-config.json";

    private static final Gson gson =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    public static boolean exists() {

        File file =
                new File(CONFIG_FILE_NAME);

        return file.exists()
                && file.isFile();
    }

    public static Optional<DatabaseSettings> loadDatabaseSettings() {

        try {

            File file =
                    new File(CONFIG_FILE_NAME);

            if (!file.exists()) {
                return Optional.empty();
            }

            try (
                    FileReader reader =
                            new FileReader(file)
            ) {

                DatabaseSettings settings =
                        gson.fromJson(
                                reader,
                                DatabaseSettings.class
                        );

                return Optional.ofNullable(settings);
            }

        } catch (Exception e) {

            e.printStackTrace();

            return Optional.empty();
        }
    }

    public static boolean saveDatabaseSettings(
            DatabaseSettings settings
    ) {

        try (
                FileWriter writer =
                        new FileWriter(CONFIG_FILE_NAME)
        ) {

            gson.toJson(
                    settings,
                    writer
            );

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
}