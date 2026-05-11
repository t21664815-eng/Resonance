package com.rs_games.resonance.core;

import com.mojang.logging.LogUtils;
import com.rs_games.resonance.ai.AIHandler;
import com.rs_games.resonance.config.Config;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LastResonance.MODID)
public class LastResonance {
    public static final String MODID = "resonance";
    public static final Logger LOGGER = LogUtils.getLogger();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(AIHandler::shutdown));
    }

    public LastResonance() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(new WorldEvents());
        MinecraftForge.EVENT_BUS.register(new ResonanceMenuButton());
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AI Loading...");
        Config.load();

        AIHandler.init();
    }
}