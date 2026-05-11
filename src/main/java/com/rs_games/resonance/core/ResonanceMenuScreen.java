package com.rs_games.resonance.core;

import com.mojang.logging.LogUtils;
import com.rs_games.resonance.ai.AIHandler;
import com.rs_games.resonance.ai.DownloadConfirmScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ResonanceMenuScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private List<String> installedModels = new ArrayList<>();
    private List<String> availableModels = new ArrayList<>();

    private static final String[][] DOWNLOADABLE_MODELS = {
            {"Gemma 2B (Q4_K_M) ~1.7 GB", "https://huggingface.co/RS-Games-Studio/gemma-2-2b-it-Q4_K_M.gguf/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"},
            {"Llama 3.2 3B (Q4_K_M) ~2.0 GB", "https://huggingface.co/RS-Games-Studio/gemma-2-2b-it-Q4_K_M.gguf/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"},
            {"Phi-3.5-mini 3.8B (Q4_K_M) ~2.3 GB", "https://huggingface.co/RS-Games-Studio/gemma-2-2b-it-Q4_K_M.gguf/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf"}
    };

    private boolean showDownloads = false;
    private boolean showInstalled = false;

    public ResonanceMenuScreen() {
        super(Component.literal("Resonance AI"));
    }

    @Override
    protected void init() {
        super.init();
        scanInstalledModels();
        availableModels.clear();

        int centerX = this.width / 2;
        int y = 65;
        if (showDownloads) {
            for (String[] model : DOWNLOADABLE_MODELS) {
                this.addRenderableWidget(
                        Button.builder(Component.literal("§b" + model[0]), b -> {
                                    Minecraft.getInstance().setScreen(new DownloadConfirmScreen(model[0], model[1]));
                                })
                                .pos(centerX - 100, y)
                                .size(200, 20)
                                .build()
                );
                y += 22;
            }

            this.addRenderableWidget(
                    Button.builder(Component.literal("Back"), b -> {
                                showDownloads = false;
                                rebuildWidgets();
                            })
                            .pos(centerX - 100, this.height - 40)
                            .size(200, 20)
                            .build()
            );
            return;
        }

        if (showInstalled) {
            if (installedModels.isEmpty()) {
            } else {
                for (String modelName : installedModels) {
                    boolean isActive = modelName.equals(getCurrentModelName());
                    String label = isActive ? "§a" + modelName + " (active)" : "§7" + modelName;

                    this.addRenderableWidget(
                            Button.builder(Component.literal(label), b -> selectModel(modelName))
                                    .pos(centerX - 100, y)
                                    .size(200, 20)
                                    .build()
                    );
                    y += 22;
                }
            }

            this.addRenderableWidget(
                    Button.builder(Component.literal("Back"), b -> {
                                showInstalled = false;
                                rebuildWidgets();
                            })
                            .pos(centerX - 100, this.height - 100)
                            .size(200, 20)
                            .build()
            );
            return;
        }
        this.addRenderableWidget(
                Button.builder(Component.literal("Download Models"), b -> {
                            showDownloads = true;
                            rebuildWidgets();
                        })
                        .pos(centerX - 100, y)
                        .size(200, 20)
                        .build()
        );
        y += 25;
        this.addRenderableWidget(
                Button.builder(Component.literal("Installed Models"), b -> {
                            showInstalled = true;
                            rebuildWidgets();
                        })
                        .pos(centerX - 100, y)
                        .size(200, 20)
                        .build()
        );
        y += 30;
        this.addRenderableWidget(
                Button.builder(Component.literal("Open Web UI"), b -> openWebUI())
                        .pos(centerX - 100, y)
                        .size(200, 20)
                        .build()
        );
        y += 30;

        this.addRenderableWidget(
                Button.builder(Component.literal("GitHub"), b -> openLink("https://github.com/t21664815-eng/Resonance"))
                        .pos(centerX - 100, y)
                        .size(95, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Hugging Face"), b -> openLink("https://huggingface.co/RS-Games-Studio"))
                        .pos(centerX + 5, y)
                        .size(95, 20)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(Component.literal("Back"), b -> onClose())
                        .pos(centerX - 100, this.height - 40)
                        .size(200, 20)
                        .build()
        );
    }

    private void scanInstalledModels() {
        installedModels.clear();
        try {
            Path modelsDir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("models");
            Files.createDirectories(modelsDir);
            Files.list(modelsDir)
                    .filter(f -> f.toString().endsWith(".gguf"))
                    .forEach(f -> installedModels.add(f.getFileName().toString()));
        } catch (IOException e) {
            LOGGER.error("Cannot scan models: {}", e.getMessage());
        }
    }

    private String getCurrentModelName() {
        try {
            Path configFile = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get()
                    .resolve("config/resonance_config.json");
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile);
                int keyIndex = json.indexOf("\"lastModel\"");
                if (keyIndex >= 0) {
                    int colonIndex = json.indexOf(":", keyIndex);
                    int startQuote = json.indexOf("\"", colonIndex + 1);
                    int endQuote = json.indexOf("\"", startQuote + 1);
                    return json.substring(startQuote + 1, endQuote);
                }
            }
        } catch (Exception e) {}
        return "gemma-2-2b-it-Q4_K_M.gguf";
    }

    private void selectModel(String modelFile) {
        Path modelPath = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get()
                .resolve("models").resolve(modelFile);

        if (!Files.exists(modelPath)) return;

        AIHandler.setModelAndRestart(modelFile);

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§aModel changed to: " + modelFile));
        }

        Minecraft.getInstance().setScreen(null);
    }

    private void openWebUI() {
        int port = AIHandler.getActivePort();
        if (port > 0) {
            openLink("http://127.0.0.1:" + port);
        }
    }

    private void openLink(String url) {
        try {
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
        } catch (Exception e) {
            LOGGER.error("Cannot open link: {}", e.getMessage());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, Component.literal("Resonance AI"), this.width / 2, 20, 0xFFFFFF);

        if (showDownloads) {
            graphics.drawCenteredString(this.font, Component.literal("Click to download:"), this.width / 2, 45, 0xAAAAAA);
        } else if (showInstalled) {
            graphics.drawCenteredString(this.font, Component.literal("Installed models (click to switch):"), this.width / 2, 45, 0xAAAAAA);
        } else {
            graphics.drawCenteredString(this.font, Component.literal(""), this.width / 2, 45, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}