package com.rs_games.resonance.ai;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.*;

public class LlamaExtractor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path GAME_DIR = FMLPaths.GAMEDIR.get();
    private static final Path LLAMA_DIR = GAME_DIR.resolve("llama.cpp");
    private static final Path LLAMA_EXE = LLAMA_DIR.resolve("llama-server.exe");

    public static void extractIfNeeded() {
        if (Files.exists(LLAMA_EXE)) {
            LOGGER.info("llama.cpp already installed, skipping extraction.");
            return;
        }

        LOGGER.info("Copying llama files from JAR to game directory...");
        try {
            Files.createDirectories(LLAMA_DIR);
            copyAllFromJar("/llama/", LLAMA_DIR);
            LOGGER.info("All llama files extracted successfully to: {}", LLAMA_DIR.toAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to extract llama: {}", e.getMessage());
        }
    }

    private static void copyAllFromJar(String resourceDir, Path targetDir) throws Exception {
        java.net.URI uri = LlamaExtractor.class.getResource(resourceDir).toURI();

        Path resourcePath;
        if (uri.getScheme().equals("jar")) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, java.util.Collections.emptyMap())) {
                resourcePath = fs.getPath(resourceDir);
                copyDirectory(resourcePath, targetDir);
            }
        } else {
            resourcePath = Path.of(uri);
            copyDirectory(resourcePath, targetDir);
        }
    }

    private static void copyDirectory(Path source, Path target) throws Exception {
        try (var files = Files.walk(source)) {
            files.forEach(file -> {
                try {
                    Path relativePath = source.relativize(file);
                    Path targetFile = target.resolve(relativePath.toString());

                    if (Files.isDirectory(file)) {
                        Files.createDirectories(targetFile);
                    } else {
                        Files.createDirectories(targetFile.getParent());
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.debug("  Copied: {}", relativePath);
                    }
                } catch (Exception e) {
                    LOGGER.warn("  Failed to copy {}: {}", file, e.getMessage());
                }
            });
        }
    }

    public static boolean isInstalled() {
        return Files.exists(LLAMA_EXE);
    }
}