package com.rs_games.resonance.ai;

import com.mojang.logging.LogUtils;
import com.rs_games.resonance.config.Config;
import com.rs_games.resonance.config.Settings;
import com.rs_games.resonance.core.ResonanceCore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path gameDir = FMLPaths.GAMEDIR.get();
    private static Path llamaPath;
    private static Path modelPath;

    private static boolean initialized = false;
    private static Process serverProcess;
    private static final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    private static int activePort = -1;
    private static String SERVER_URL;
    private static String currentPersonality = "HELPER";
    private static final Map<UUID, String> playerContexts = new ConcurrentHashMap<>();

    public static void init() {
        if (initialized) return;
        LlamaExtractor.extractIfNeeded();

        Path llamaDir = gameDir.resolve("llama.cpp");
        llamaPath = llamaDir.resolve("llama-server.exe");

        String modelFileName = getActiveModelFileName();
        modelPath = gameDir.resolve("models").resolve(modelFileName);

        try {
            Files.createDirectories(llamaDir);
            Files.createDirectories(modelPath.getParent());
        } catch (Exception e) {
            LOGGER.error("Cannot create directories: {}", e.getMessage());
            return;
        }

        if (!Files.exists(llamaPath)) {
            LOGGER.info("Extracting llama-server.exe from JAR...");
            try (InputStream in = AIHandler.class.getResourceAsStream("/llama/llama-server.exe")) {
                if (in != null) {
                    Files.copy(in, llamaPath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("llama-server.exe extracted successfully.");
                } else {
                    LOGGER.error("llama-server.exe NOT FOUND in JAR resources!");
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to extract llama: {}", e.getMessage());
                return;
            }
        }

        if (!Files.exists(modelPath)) {
            LOGGER.error("Model not found: {}", modelPath.toAbsolutePath());
            LOGGER.info("Please download model from Hugging Face and place it in models/");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                killAllLlamaProcesses();
                Thread.sleep(500);

                int[] priorityPorts = {8080, 8888, 9000, 11544, 12345, 15000, 20000};
                boolean started = false;

                for (int port : priorityPorts) {
                    if (tryStartOnPort(port)) {
                        started = true;
                        break;
                    }
                }

                if (!started) {
                    for (int port = 10000; port < 10200; port++) {
                        if (tryStartOnPort(port)) {
                            started = true;
                            break;
                        }
                    }
                }

                if (!started) {
                    LOGGER.error("Could not find a free port on this device.");
                }

            } catch (Exception e) {
                LOGGER.error("Error: {}", e.getMessage());
            }
        });
    }

    private static boolean tryStartOnPort(int port) {
        if (!isPortAvailable(port)) return false;

        LOGGER.info("Trying port {}...", port);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    llamaPath.toAbsolutePath().toString(),
                    "-m", modelPath.toAbsolutePath().toString(),
                    "--port", String.valueOf(port),
                    "-c", "2048",
                    "-t", "3",
                    "--temp", "0.7",
                    "--repeat-penalty", "1.2",
                    "--log-disable",
                    "--host", "127.0.0.1"
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            serverProcess = pb.start();

            String testUrl = "http://127.0.0.1:" + port + "/v1/models";
            for (int i = 0; i < 15; i++) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(testUrl))
                            .timeout(java.time.Duration.ofSeconds(1))
                            .GET().build();
                    HttpResponse<String> resp = httpClient.send(req,
                            HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200) {
                        activePort = port;
                        SERVER_URL = "http://127.0.0.1:" + port + "/v1/chat/completions";
                        initialized = true;
                        LOGGER.info("Server Online on port {}!", port);
                        return true;
                    }
                } catch (Exception e) {
                }
                Thread.sleep(1000);
            }

            LOGGER.warn("Port {} did not respond", port);
            serverProcess.destroyForcibly();
            return false;

        } catch (Exception e) {
            LOGGER.warn("Port {} failed: {}", port, e.getMessage());
            return false;
        }
    }

    private static boolean isPortAvailable(int port) {
        try (java.net.ServerSocket ss = new java.net.ServerSocket(port)) {
            ss.setReuseAddress(true);
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    public static void setContext(UUID playerId, String context) {
        playerContexts.put(playerId, context);
    }

    public static String getContext(UUID playerId) {
        return playerContexts.getOrDefault(playerId, "");
    }

    public static void setModelAndRestart(String newModelFile) {
        LOGGER.info("Switching model to: {}", newModelFile);

        try {
            Path configFile = gameDir.resolve("config/resonance_config.json");
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile);
                json = json.replaceAll("\"lastModel\"\\s*:\\s*\"[^\"]*\"",
                        "\"lastModel\": \"" + newModelFile + "\"");
                Files.writeString(configFile, json);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not save model choice: {}", e.getMessage());
        }

        shutdown();
        initialized = false;
        init();
        LOGGER.info("Model switched successfully.");
    }

    public static void clearContext(UUID playerId) {
        playerContexts.remove(playerId);
    }

    public static void ask(ServerPlayer player, String question, String personality) {
        if (!initialized) {
            player.sendSystemMessage(Component.translatable("resonance.ai.loading"));
            return;
        }

        if (isProcessing.get()) {
            player.sendSystemMessage(Component.translatable("resonance.ai.error"));
            return;
        }

        isProcessing.set(true);
        currentPersonality = personality.toUpperCase();

        String personalityName = getPersonalityName(currentPersonality);
        player.sendSystemMessage(Component.translatable("resonance.ai.thinking"));

        String systemPrompt = ResonanceCore.getPersonalityPrompt(personality);

        String context = getContext(player.getUUID());
        if (!context.isEmpty()) {
            systemPrompt += "\n\nContext from previous conversation:\n" + context;
        }

        generateResponse(systemPrompt, question).thenAccept(result -> {
            player.sendSystemMessage(Component.literal(getPersonalityColor(currentPersonality) +
                    "[" + personalityName + "]: " + result));
            isProcessing.set(false);
        }).exceptionally(e -> {
            player.sendSystemMessage(Component.translatable("resonance.ai.connection_lost"));
            isProcessing.set(false);
            return null;
        });
    }

    public static int getActivePort() {
        return activePort;
    }

    public static void ask(ServerPlayer player, String question) {
        ask(player, question, "HELPER");
    }

    public static CompletableFuture<String> generateResponse(String systemPrompt, String userMessage) {
        String escapedSystem = systemPrompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        String escapedUser = userMessage
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        String json = "{\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escapedSystem + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapedUser + "\"}]," +
                "\"stream\":false}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(AIHandler::parseChatResponse);
    }

    private static String parseChatResponse(String body) {
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
            String content = json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            return cleanAIOutput(content);
        } catch (Exception e) {
            LOGGER.error("GSON parse error: {}", e.getMessage());
            return "System error. Check llama server logs.";
        }
    }

    private static String cleanAIOutput(String rawOutput) {
        if (rawOutput == null) return "...";
        rawOutput = rawOutput.replaceAll("\\n{3,}", "\n\n");
        rawOutput = rawOutput.trim();
        return rawOutput.isEmpty() ? "..." : rawOutput;
    }

    private static String getPersonalityName(String personality) {
        Settings settings = Config.load();
        for (Settings.Personality p : settings.personalities) {
            if (p.id.equalsIgnoreCase(personality)) {
                return p.name;
            }
        }
        return "Helper";
    }

    private static String getPersonalityColor(String personality) {
        Settings settings = Config.load();
        for (Settings.Personality p : settings.personalities) {
            if (p.id.equalsIgnoreCase(personality)) {
                return p.color;
            }
        }
        return "b";
    }

    private static void killAllLlamaProcesses() {
        try {
            Runtime.getRuntime().exec("taskkill /F /IM llama-server.exe /T");
            Runtime.getRuntime().exec("taskkill /F /IM llama-cli.exe /T");
        } catch (Exception ignored) {}
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void shutdown() {
        initialized = false;
        if (serverProcess != null) {
            serverProcess.destroyForcibly();
            try { serverProcess.waitFor(3, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        killAllLlamaProcesses();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
    }

    private static String getActiveModelFileName() {
        try {
            Path configFile = gameDir.resolve("config/resonance_config.json");
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile);
                int keyIndex = json.indexOf("\"lastModel\"");
                if (keyIndex >= 0) {
                    int colonIndex = json.indexOf(":", keyIndex);
                    int startQuote = json.indexOf("\"", colonIndex + 1);
                    int endQuote = json.indexOf("\"", startQuote + 1);
                    String model = json.substring(startQuote + 1, endQuote);
                    LOGGER.info("Loaded model from config: {}", model);
                    return model;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read config: {}", e.getMessage());
        }
        return "gemma-2-2b-it-Q4_K_M.gguf";
    }
}