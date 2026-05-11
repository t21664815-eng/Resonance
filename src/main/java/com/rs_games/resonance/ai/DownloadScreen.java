package com.rs_games.resonance.ai;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DownloadScreen extends Screen {
    private boolean finished = false;
    private boolean success = false;
    private String statusText = "Checking files...";
    private int progress = 0;

    public DownloadScreen() {
        super(Component.literal("Resonance AI Core"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int bottomY = this.height - 30;
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Back"),
                                (button) -> Minecraft.getInstance().setScreen(null)
                        )
                        .pos(10, bottomY)
                        .size(60, 20)
                        .build()
        );
        if (finished) {
            this.addRenderableWidget(
                    Button.builder(
                                    Component.literal(success ? "Done" : "Retry"),
                                    (button) -> {
                                        if (success) {
                                            Minecraft.getInstance().setScreen(null);
                                        } else {
                                            Minecraft.getInstance().setScreen(new DownloadScreen());
                                        }
                                    }
                            )
                            .pos(this.width - 70, bottomY)
                            .size(60, 20)
                            .build()
            );
        }
        checkFiles();
    }

    private void checkFiles() {
        new Thread(() -> {
            try {
                statusText = "Checking files...";
                progress = 10;
                Thread.sleep(500);

                if (ResonanceDownloadManager.isEverythingReady()) {
                    success = true;
                    statusText = "All files are ready!";
                    progress = 100;
                } else {
                    statusText = "Model not found. Please download manually.";
                    success = false;
                    progress = 0;
                }
            } catch (Exception e) {
                statusText = "Error: " + e.getMessage();
                success = false;
            }
            finished = true;
            Minecraft.getInstance().execute(this::rebuildWidgets);
        }).start();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        graphics.drawCenteredString(this.font,
                Component.literal("Resonance AI Core"),
                centerX, centerY - 60, 0xFFFFFF);

        graphics.drawCenteredString(this.font,
                Component.literal("First local AI in Minecraft"),
                centerX, centerY - 45, 0xAAAAAA);

        String color = finished ? (success ? "§a" : "§c") : "§e";
        graphics.drawCenteredString(this.font,
                Component.literal(color + statusText),
                centerX, centerY - 15, 0xFFFFFF);

        int barWidth = 200;
        int barHeight = 20;
        int barX = centerX - barWidth / 2;
        int barY = centerY;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        graphics.fill(barX + 2, barY + 2,
                barX + (int)((barWidth - 4) * progress / 100.0),
                barY + barHeight - 2,
                finished ? (success ? 0xFF238636 : 0xFFE94560) : 0xFF58A6FF);

        graphics.drawCenteredString(this.font,
                Component.literal(progress + "%"),
                centerX, barY - 15, 0xCCCCCC);

        graphics.drawCenteredString(this.font,
                Component.literal("Model: Gemma 2B (Q4_K_M) | ~1.7 GB"),
                centerX, this.height - 50, 0x666666);

        graphics.drawCenteredString(this.font,
                Component.literal("RS Games Studio"),
                centerX, this.height - 35, 0x555555);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}