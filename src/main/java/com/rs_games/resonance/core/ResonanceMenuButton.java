package com.rs_games.resonance.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ResonanceMenuButton {

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        int x = event.getScreen().width / 2 + 104;
        int y = event.getScreen().height / 4 + 48;
        event.addListener(
                Button.builder(
                                Component.literal("AI"),
                                (button) -> Minecraft.getInstance().setScreen(new ResonanceMenuScreen())
                        )
                        .pos(x, y)
                        .size(20, 20)
                        .build()
        );
    }
}