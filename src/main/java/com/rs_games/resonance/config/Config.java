package com.rs_games.resonance.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get();
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("resonance_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Settings load() {
        try {
            Files.createDirectories(CONFIG_DIR);

            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                Settings settings = GSON.fromJson(json, Settings.class);
                if (settings != null) {
                    LOGGER.info("Config loaded: {}", CONFIG_FILE);
                    return settings;
                }
            }

            Settings defaults = Settings.createDefault();
            save(defaults);
            LOGGER.info("Created new config with default settings.");
            return defaults;

        } catch (Exception e) {
            LOGGER.error("Error loading config: {}", e.getMessage());
            return Settings.createDefault();
        }
    }

    public static void save(Settings settings) {
        try {
            Files.createDirectories(CONFIG_DIR);
            String json = GSON.toJson(settings);
            Files.writeString(CONFIG_FILE, json);
            LOGGER.info("Config saved.");
        } catch (Exception e) {
            LOGGER.error("Error saving config: {}", e.getMessage());
        }
    }
}