package celeste.skyjourney;

import celeste.skyjourney.feature.FeatureManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import celeste.skyjourney.common.TerrainOptimizationManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class SkyJourneyMod implements ModInitializer {
    public static final String MOD_ID = "skyjourney";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        celeste.skyjourney.config.SkyJourneyConfig.load();
        LOGGER.info("[SkyJourney] Initializing modules...");
        FeatureManager.init();

        ServerTickEvents.START_WORLD_TICK.register(TerrainOptimizationManager::onWorldTick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD
                .register(TerrainOptimizationManager::onChunkLoad);

        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String json = celeste.skyjourney.config.SkyJourneyConfig.serialize();
            celeste.skyjourney.network.PacketHandler.sendConfigSync(handler.getPlayer(), json);
        });

        LOGGER.info("[SkyJourney] Initialization complete.");
    }
}