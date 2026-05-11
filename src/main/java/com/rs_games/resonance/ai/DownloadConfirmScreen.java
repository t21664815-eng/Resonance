package com.rs_games.resonance.ai;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DownloadConfirmScreen extends Screen {
    private final String modelName;
    private final String modelUrl;
    private final String fileName;

    private boolean downloading = false;
    private boolean cancelled = false;
    private boolean finished = false;
    private boolean success = false;
    private int progress = 0;
    private String statusText = "";
    private String speedText = "";
    private Thread downloadThread;
    private long startTime;

    public DownloadConfirmScreen(String modelName, String modelUrl) {
        super(Component.literal("Download Model"));
        this.modelName = modelName;
        this.modelUrl = modelUrl;
        String name = modelUrl.substring(modelUrl.lastIndexOf('/') + 1);
        this.fileName = name.contains("?") ? name.substring(0, name.indexOf('?')) : name;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int bottomY = this.height - 40;

        if (!downloading && !finished) {
            this.addRenderableWidget(
                    Button.builder(Component.translatable("resonance.button.start_download"), b -> startDownload())
                            .pos(centerX - 150, bottomY - 30)
                            .size(95, 20)
                            .build()
            );
            this.addRenderableWidget(
                    Button.builder(Component.translatable("resonance.button.play_anyway"), b -> onClose())
                            .pos(centerX - 47, bottomY - 30)
                            .size(95, 20)
                            .build()
            );
            this.addRenderableWidget(
                    Button.builder(Component.translatable("resonance.button.cancel"), b -> onClose())
                            .pos(centerX + 55, bottomY - 30)
                            .size(95, 20)
                            .build()
            );
        }

        if (downloading) {
            this.addRenderableWidget(
                    Button.builder(Component.translatable("resonance.button.play_anyway"), b -> onClose())
                            .pos(centerX - 100, bottomY - 30)
                            .size(95, 20)
                            .build()
            );
            this.addRenderableWidget(
                    Button.builder(Component.translatable("resonance.button.cancel_download"), b -> cancelDownload())
                            .pos(centerX + 5, bottomY - 30)
                            .size(95, 20)
                            .build()
            );
        }

        if (finished) {
            String label = success ? "resonance.button.done" : "resonance.button.retry";
            this.addRenderableWidget(
                    Button.builder(Component.translatable(label), b -> {
                                if (success) onClose();
                                else { finished = false; cancelled = false; progress = 0; rebuildWidgets(); }
                            })
                            .pos(centerX - 50, bottomY - 30)
                            .size(100, 20)
                            .build()
            );
        }
    }

    private void startDownload() {
        downloading = true;
        cancelled = false;
        startTime = System.currentTimeMillis();
        rebuildWidgets();

        downloadThread = new Thread(() -> {
            Path target = null;
            try {
                statusText = "Connecting...";
                URL url = new URL(modelUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(300000);
                conn.setRequestProperty("User-Agent", "Resonance/1.0");

                long totalBytes = conn.getContentLengthLong();
                long downloadedBytes = 0;
                long lastUpdateTime = System.currentTimeMillis();

                Path modelsDir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("models");
                Files.createDirectories(modelsDir);
                target = modelsDir.resolve(fileName);

                try (InputStream in = conn.getInputStream();
                     OutputStream out = Files.newOutputStream(target)) {

                    byte[] buffer = new byte[65536];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1 && !cancelled) {
                        out.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;

                        long now = System.currentTimeMillis();
                        if (now - lastUpdateTime > 200) {
                            progress = totalBytes > 0 ? (int) (downloadedBytes * 100 / totalBytes) : 0;

                            long duration = now - startTime;
                            if (duration > 0) {
                                double speed = downloadedBytes / (duration / 1000.0);
                                speedText = formatSpeed(speed);
                            }

                            String downloaded = formatSize(downloadedBytes);
                            String total = totalBytes > 0 ? formatSize(totalBytes) : "?";
                            statusText = downloaded + " / " + total;

                            lastUpdateTime = now;
                        }
                    }
                }

                if (cancelled) {
                    Files.deleteIfExists(target);
                    success = false;
                    statusText = "Download cancelled";
                    speedText = "";
                } else {
                    success = Files.exists(target) && Files.size(target) > 0;
                    if (success) {
                        progress = 100;
                    }
                    statusText = success ? "Download complete!" : "Download failed!";
                    speedText = "";
                }

            } catch (Exception e) {
                success = false;
                statusText = "Error: " + e.getMessage();
                speedText = "";
            }
            downloading = false;
            finished = true;
            Minecraft.getInstance().execute(this::rebuildWidgets);
        });
        downloadThread.start();
    }

    private void cancelDownload() {
        cancelled = true;
        if (downloadThread != null) {
            downloadThread.interrupt();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond < 1024) return String.format("%.0f B/s", bytesPerSecond);
        if (bytesPerSecond < 1024 * 1024) return String.format("%.1f KB/s", bytesPerSecond / 1024);
        return String.format("%.1f MB/s", bytesPerSecond / (1024 * 1024));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, Component.literal("§b§lDownload Model"), centerX, 25, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("§7" + modelName), centerX, 40, 0xAAAAAA);
        graphics.drawCenteredString(this.font, Component.literal("§8File: " + fileName), centerX, 55, 0x888888);

        if (!downloading && !finished) {
            graphics.drawCenteredString(this.font, Component.literal("§eReady to download"), centerX, 100, 0xFFFF55);
            graphics.drawCenteredString(this.font, Component.literal("§7" + fileName + " will be saved to:"), centerX, 115, 0xAAAAAA);
            graphics.drawCenteredString(this.font, Component.literal("§7.minecraft/models/"), centerX, 128, 0xAAAAAA);
        }
        if (downloading || finished) {
            int barWidth = 250;
            int barHeight = 24;
            int barX = centerX - barWidth / 2;
            int barY = 90;
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF222222);
            graphics.fill(barX + 1, barY + 1, barX + barWidth - 1, barY + barHeight - 1, 0xFF333333);

            int fillWidth = (int) ((barWidth - 4) * progress / 100.0);
            int color = finished
                    ? (success ? 0xFF2EA043 : 0xFFDA3633)
                    : 0xFF58A6FF;
            graphics.fill(barX + 2, barY + 2, barX + fillWidth, barY + barHeight - 2, color);

            String percentText = progress + "%";
            graphics.drawCenteredString(this.font, Component.literal(percentText),
                    centerX, barY + (barHeight - 8) / 2, 0xFFFFFF);

            String color2 = finished ? (success ? "§a" : "§c") : "§e";
            graphics.drawCenteredString(this.font, Component.literal(color2 + statusText),
                    centerX, barY + barHeight + 12, 0xFFFFFF);

            if (!speedText.isEmpty()) {
                graphics.drawCenteredString(this.font, Component.literal("§7" + speedText),
                        centerX, barY + barHeight + 25, 0xAAAAAA);
            }

            if (downloading && progress > 0 && progress < 100) {
                long elapsed = System.currentTimeMillis() - startTime;
                long eta = (elapsed / progress) * (100 - progress);
                String etaText = "≈ " + formatETA(eta);
                graphics.drawCenteredString(this.font, Component.literal("§7" + etaText),
                        centerX, barY + barHeight + 38, 0x888888);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String formatETA(long millis) {
        if (millis < 1000) return "1s";
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        seconds %= 60;
        return minutes + "m " + seconds + "s";
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !downloading;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}