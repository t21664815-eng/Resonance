package com.rs_games.resonance.core;

import com.mojang.logging.LogUtils;
import com.rs_games.resonance.ai.AIHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.*;

public class WorldEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, List<String>> conversations = new HashMap<>();

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String memory = MemoryManager.loadLatestMemory(player);
            if (memory != null) {
                AIHandler.setContext(player.getUUID(),
                        "Context from previous session:\n" + memory);
            }
            conversations.put(player.getUUID(), new ArrayList<>());
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID id = player.getUUID();
            List<String> conversation = conversations.remove(id);
            if (conversation != null && !conversation.isEmpty()) {
                MemoryManager.saveMemory(player, conversation);
            }
        }
    }

    @SubscribeEvent
    public void onChatMessage(ServerChatEvent event) {
        String message = event.getMessage().getString();
        ServerPlayer player = event.getPlayer();
        UUID playerId = player.getUUID();

        // Команды (начинаются с /resonance)
        if (message.startsWith("/resonance")) {
            handleCommand(player, message);
            event.setCanceled(true);
            return;
        }

        // Обращение к AI-боту
        String[] triggers = {"помощник", "помоги", "help", "helper", "вики", "wiki", "хелпер", "ассистент", "бот"};
        for (String trigger : triggers) {
            if (message.toLowerCase().startsWith(trigger)) {
                String question = message.substring(trigger.length()).trim();
                if (question.isEmpty()) question = "Привет!";

                conversations.computeIfAbsent(playerId, k -> new ArrayList<>())
                        .add("Игрок: " + question);

                AIHandler.ask(player, question, "HELPER");
                event.setCanceled(true);
                return;
            }
        }
    }

    private void handleCommand(ServerPlayer player, String message) {
        String[] parts = message.split(" ");
        String cmd = parts.length > 1 ? parts[1].toLowerCase() : "";

        switch (cmd) {
            case "status":
                if (AIHandler.isInitialized()) {
                    player.sendSystemMessage(Component.literal("§aAI: Online"));
                    player.sendSystemMessage(Component.literal("§7Port: " + AIHandler.getActivePort()));
                    player.sendSystemMessage(Component.literal("§7Web UI: http://127.0.0.1:" + AIHandler.getActivePort()));
                } else {
                    player.sendSystemMessage(Component.literal("§cAI: Offline"));
                }
                break;

            case "help":
                player.sendSystemMessage(Component.literal("§bCommands:"));
                player.sendSystemMessage(Component.literal("§7/resonance status §8- Check AI status"));
                player.sendSystemMessage(Component.literal("§7/resonance help §8- Show commands"));
                player.sendSystemMessage(Component.literal("§7/resonance download §8- Open model downloader"));
                break;

            case "download":
                player.sendSystemMessage(Component.literal("§bDownload models from:"));
                player.sendSystemMessage(Component.literal("§7https://huggingface.co/RS-Games-Studio"));
                break;

            default:
                player.sendSystemMessage(Component.literal("§cUnknown command. Try /resonance help"));
                break;
        }
    }
}