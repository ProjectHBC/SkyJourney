package celeste.skyjourney.feature;

import celeste.skyjourney.SkyJourneyMod;
import celeste.skyjourney.mixin.plugin.SkyJourneyPluginState;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * MOD内の各機能のライフサイクルと設定を管理
 */
public class FeatureManager {

    private static Config config = new Config();
    private static File configFile;

    public static void init() {
        loadConfig();
        SkyJourneyPluginState.checkPlugin(config);

        if (config.enableSneakFix) {
            SkyJourneyMod.LOGGER.info("Feature: SneakGroundFix [ENABLED]");
        } else {
            SkyJourneyMod.LOGGER.info("Feature: SneakGroundFix [DISABLED]");
        }

        if (config.enableVillagerFix) {
            SkyJourneyMod.LOGGER.info("Feature: VillagerShipFix [ENABLED]");
        } else {
            SkyJourneyMod.LOGGER.info("Feature: VillagerShipFix [DISABLED]");
        }

        if (config.enableDrawerFix) {
            SkyJourneyMod.LOGGER.info("Feature: DrawerInteractionFix [ENABLED]");
        } else {
            SkyJourneyMod.LOGGER.info("Feature: DrawerInteractionFix [DISABLED]");
        }
    }

    private static void loadConfig() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("skyjourney.json").toFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, Config.class);
            } catch (IOException e) {
                SkyJourneyMod.LOGGER.error("Failed to load config", e);
            }
        } else {
            saveConfig();
        }
    }

    private static void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            SkyJourneyMod.LOGGER.error("Failed to save config", e);
        }
    }

    public static boolean isSneakFixEnabled() {
        return config.enableSneakFix;
    }

    public static boolean isVillagerFixEnabled() {
        return config.enableVillagerFix;
    }

    public static boolean isDrawerFixEnabled() {
        return config.enableDrawerFix;
    }

    public static class Config {
        public boolean enableSneakFix = true;
        public boolean enableVillagerFix = true;
        public boolean enableDrawerFix = false;
    }
}