package com.rs_games.resonance.ai;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class ResonanceDownloadManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path GAME_DIR = FMLPaths.GAMEDIR.get();
    private static final Path MODELS_DIR = GAME_DIR.resolve("models");
    static final Path LLAMA_DIR = GAME_DIR.resolve("llama.cpp");

    private static final String LLAMA_URL =
            "https://github.com/t21664815-eng/The-Last-Resonance-Assets/releases/download/v0.0.1-alpha_ai_core/llama-b9062-bin-win-cpu-x64.zip";
    private static final String MODEL_URL =
            "https://huggingface.co/RS-Games-Studio/gemma-2-2b-it-Q4_K_M.gguf/resolve/main/gemma-2-2b-it-Q4_K_M.gguf";

    public static CompletableFuture<Boolean> downloadAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(MODELS_DIR);
                Files.createDirectories(LLAMA_DIR);

                Path llamaExe = LLAMA_DIR.resolve("llama-server.exe");
                if (!Files.exists(llamaExe)) {
                    LOGGER.info("Downloading llama.cpp...");
                    boolean ok = downloadFile(LLAMA_URL, GAME_DIR.resolve("llama.zip"));
                    if (ok) {
                        unzip(GAME_DIR.resolve("llama.zip"), LLAMA_DIR);
                        Files.delete(GAME_DIR.resolve("llama.zip"));
                        LOGGER.info("llama.cpp installed.");
                    } else {
                        LOGGER.error("Failed to download llama.cpp");
                        return false;
                    }
                }

                Path modelFile = MODELS_DIR.resolve("gemma-2-2b-it-Q4_K_M.gguf");
                if (!Files.exists(modelFile)) {
                    LOGGER.info("Downloading model...");
                    boolean ok = downloadFile(MODEL_URL, modelFile);
                    if (ok) {
                        LOGGER.info("Model installed.");
                    } else {
                        LOGGER.error("Failed to download model");
                        return false;
                    }
                }

                return true;
            } catch (Exception e) {
                LOGGER.error("Error: {}", e.getMessage());
                return false;
            }
        });
    }

    private static boolean downloadFile(String urlStr, Path target) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(300000);
            conn.setRequestProperty("User-Agent", "Resonance/1.0");

            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return Files.exists(target) && Files.size(target) > 0;
        } catch (Exception e) {
            LOGGER.error("Download error: {}", e.getMessage());
            return false;
        }
    }

    private static void unzip(Path zipFile, Path targetDir) {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            LOGGER.error("Unzip error: {}", e.getMessage());
        }
    }

    public static boolean isEverythingReady() {
        Path llamaExe = LLAMA_DIR.resolve("llama-server.exe");
        Path modelFile = MODELS_DIR.resolve("gemma-2-2b-it-Q4_K_M.gguf");
        return Files.exists(llamaExe) && Files.exists(modelFile);
    }
}