package celeste.skyjourney;

import celeste.skyjourney.feature.FeatureManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyJourneyMod implements ModInitializer {
    public static final String MOD_ID = "skyjourney";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[SkyJourney] Initializing modules...");
        FeatureManager.init();

        LOGGER.info("[SkyJourney] Initialization complete.");
    }
}