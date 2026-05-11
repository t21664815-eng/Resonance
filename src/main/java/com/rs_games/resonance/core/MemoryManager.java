package com.rs_games.resonance.core;

import com.mojang.logging.LogUtils;
import com.rs_games.resonance.ai.AIHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MemoryManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path MEMORY_DIR = FMLPaths.GAMEDIR.get().resolve("ai_memory");
    private static final int MAX_MEMORIES = 31;

    public static void saveMemory(ServerPlayer player, List<String> conversation) {
        if (conversation == null || conversation.isEmpty()) return;

        try {
            Files.createDirectories(MEMORY_DIR);
            String playerName = player.getName().getString();
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            List<String> lastLines = conversation.size() > 15
                    ? conversation.subList(conversation.size() - 15, conversation.size())
                    : conversation;

            String shortMemory = "Last conversation:\n" + String.join("\n", lastLines);

            Path memoryFile = MEMORY_DIR.resolve(playerName + "_" + timestamp + ".txt");
            Files.writeString(memoryFile, shortMemory);

            LOGGER.info("Raw memory saved ({} lines).", lastLines.size());
            cleanupOldMemories(playerName);

        } catch (Exception e) {
            LOGGER.error("Error saving memory: {}", e.getMessage());
        }
    }
    public static String loadLatestMemory(ServerPlayer player) {
        String playerName = player.getName().getString();
        String summary = loadLatestMemory(playerName);

        if (summary != null) {
            player.sendSystemMessage(Component.translatable("resonance.memory.loaded"));
            return summary;
        }
        return null;
    }
    public static String loadLatestMemory(String playerName) {
        try {
            if (!Files.exists(MEMORY_DIR)) return null;

            try (var files = Files.list(MEMORY_DIR)) {
                return files
                        .filter(f -> f.getFileName().toString().startsWith(playerName + "_"))
                        .sorted((a, b) -> -a.compareTo(b))
                        .findFirst()
                        .map(f -> {
                            try {
                                return Files.readString(f);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> listMemories(String playerName) {
        try {
            if (!Files.exists(MEMORY_DIR)) return List.of();

            try (var files = Files.list(MEMORY_DIR)) {
                return files
                        .filter(f -> f.getFileName().toString().startsWith(playerName + "_"))
                        .sorted((a, b) -> -a.compareTo(b))
                        .map(f -> f.getFileName().toString())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    public static String loadMemoryByIndex(String playerName, int index) {
        List<String> memories = listMemories(playerName);
        if (index >= 0 && index < memories.size()) {
            try {
                Path file = MEMORY_DIR.resolve(memories.get(index));
                return Files.readString(file);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static void cleanupOldMemories(String playerName) {
        try {
            try (var files = Files.list(MEMORY_DIR)) {
                var memories = files
                        .filter(f -> f.getFileName().toString().startsWith(playerName + "_"))
                        .sorted((a, b) -> -a.compareTo(b))
                        .collect(Collectors.toList());

                if (memories.size() > MAX_MEMORIES) {
                    for (int i = MAX_MEMORIES; i < memories.size(); i++) {
                        Files.delete(memories.get(i));
                        LOGGER.info("Delete {}", memories.get(i).getFileName());
                    }
                }
            }
        } catch (Exception e) {
        }
    }
}