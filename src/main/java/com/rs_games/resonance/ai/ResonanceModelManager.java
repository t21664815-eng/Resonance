package com.rs_games.resonance.ai;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import com.rs_games.resonance.config.Config;
import com.rs_games.resonance.config.Settings;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ResonanceModelManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path GAME_DIR = FMLPaths.GAMEDIR.get();
    private static final Path MODELS_DIR = GAME_DIR.resolve("models");
    private static final Gson GSON = new Gson();
    private static final String MODELS_LIST_URL =
            "https://raw.githubusercontent.com/t21664815-eng/Resonance/main/models.json";

    private static List<ModelInfo> remoteModels = new ArrayList<>();
    private static List<ModelInfo> localModels = new ArrayList<>();

    public static class ModelInfo {
        public String name;
        public String file;
        public String url;
        public long size;
        public String sha256;
        public String info;
    }

    public static CompletableFuture<List<ModelInfo>> fetchRemoteModels() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(MODELS_LIST_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                    remoteModels = GSON.fromJson(reader,
                            new TypeToken<List<ModelInfo>>(){}.getType());
                    LOGGER.info("Loaded {} models from GitHub", remoteModels.size());
                    return remoteModels;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load models: {}", e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public static List<ModelInfo> scanLocalModels() {
        localModels.clear();
        try {
            Files.createDirectories(MODELS_DIR);
            Files.list(MODELS_DIR)
                    .filter(f -> f.toString().endsWith(".gguf"))
                    .forEach(f -> {
                        ModelInfo info = new ModelInfo();
                        info.file = f.getFileName().toString();
                        info.name = info.file.replace(".gguf", "");
                        try {
                            info.size = Files.size(f);
                        } catch (IOException ignored) {}
                        localModels.add(info);
                    });
            LOGGER.info("Found {} local models", localModels.size());
        } catch (Exception e) {
            LOGGER.error("Error scanning models: {}", e.getMessage());
        }
        return localModels;
    }

    public static List<ModelInfo> getAvailableDownloads() {
        List<ModelInfo> downloadable = new ArrayList<>();
        scanLocalModels();

        for (ModelInfo remote : remoteModels) {
            boolean found = false;
            for (ModelInfo local : localModels) {
                if (local.file.equals(remote.file)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                downloadable.add(remote);
            }
        }
        return downloadable;
    }

    public static CompletableFuture<Path> downloadModel(ModelInfo model) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(MODELS_DIR);
                Path target = MODELS_DIR.resolve(model.file);

                LOGGER.info("Downloading {} ({} MB)...", model.name, model.size / 1024 / 1024);

                URL url = new URL(model.url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);

                long totalSize = conn.getContentLengthLong();
                long downloaded = 0;

                try (InputStream in = conn.getInputStream();
                     OutputStream out = Files.newOutputStream(target)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;

                        if (downloaded % (10 * 1024 * 1024) == 0) {
                            int percent = (int) (downloaded * 100 / totalSize);
                            LOGGER.info("Progress: {}% ({}/{} MB)",
                                    percent, downloaded / 1024 / 1024, totalSize / 1024 / 1024);
                        }
                    }
                }

                if (model.sha256 != null && !model.sha256.isEmpty()) {
                    String localHash = sha256(target);
                    if (!localHash.equals(model.sha256)) {
                        LOGGER.error("Hash mismatch! File corrupted.");
                        Files.delete(target);
                        return null;
                    }
                    LOGGER.info("Hash verified successfully.");
                }

                LOGGER.info("Model downloaded: {}", target.toAbsolutePath());
                return target;

            } catch (Exception e) {
                LOGGER.error("Download error: {}", e.getMessage());
                return null;
            }
        });
    }

    public static void setActiveModel(String modelFile) {
        Settings settings = Config.load();
        settings.lastModel = modelFile;
        Config.save(settings);
        LOGGER.info("Active model changed to: {}", modelFile);
    }

    public static Path getActiveModelPath() {
        Settings settings = Config.load();
        return MODELS_DIR.resolve(settings.lastModel);
    }

    public static boolean hasAnyModel() {
        scanLocalModels();
        return !localModels.isEmpty();
    }

    private static String sha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(Files.readAllBytes(file));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isEverythingReady() {
        Path llamaExe = GAME_DIR.resolve("llama.cpp/llama-server.exe");
        Path modelFile = MODELS_DIR.resolve("gemma-2-2b-it-Q4_K_M.gguf");
        return Files.exists(llamaExe) && Files.exists(modelFile);
    }
}