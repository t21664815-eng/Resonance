package com.rs_games.resonance.core;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class SecurityManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void checkIntegrity() {
        LOGGER.info("Security check skipped (not configured for Resonance).");
    }
}