package it.minebuilder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MineBuilder implements ModInitializer {
    public static final String MOD_ID = "minebuilder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Mine Builder initialized. GPT connection not yet configured.");
    }
}
