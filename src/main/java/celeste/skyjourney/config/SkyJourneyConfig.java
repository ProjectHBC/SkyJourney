package celeste.skyjourney.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class SkyJourneyConfig {

    // サーバー
    public static boolean enableTerrainBakingOptimization = true;
    public static int bakingYBuffer = 32;
    public static int memoryPollInterval = 20;

    // 機能トグル
    public static boolean enableSneakFix = true;
    public static boolean enableVillagerFix = true;

    // クライアント
    public static boolean showDebugHUD = false;

    // サーバー管理フラグ
    private static boolean managedByServer = false;

    public static boolean isManagedByServer() {
        return managedByServer;
    }

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("skyjourney.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void applyServerConfig(String json) {
        try {
            ConfigData data = GSON.fromJson(json, ConfigData.class);
            if (data != null) {
                enableTerrainBakingOptimization = data.enableTerrainBakingOptimization;
                bakingYBuffer = data.bakingYBuffer;
                memoryPollInterval = data.memoryPollInterval;
                enableSneakFix = data.enableSneakFix;
                enableVillagerFix = data.enableVillagerFix;
                // showDebugHUD is client-side only, so we don't sync it

                managedByServer = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void restoreLocalConfig() {
        managedByServer = false;
        load();
    }

    public static String serialize() {
        ConfigData data = new ConfigData();
        data.enableTerrainBakingOptimization = enableTerrainBakingOptimization;
        data.bakingYBuffer = bakingYBuffer;
        data.memoryPollInterval = memoryPollInterval;
        data.enableSneakFix = enableSneakFix;
        data.enableVillagerFix = enableVillagerFix;

        data.showDebugHUD = showDebugHUD;
        return GSON.toJson(data);
    }

    // 設定データを保持する内部クラス
    private static class ConfigData {
        boolean enableTerrainBakingOptimization = true;
        int bakingYBuffer = 32;
        int memoryPollInterval = 20;
        boolean enableSneakFix = true;
        boolean enableVillagerFix = true;
        boolean showDebugHUD = false;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) {
                        if (data.bakingYBuffer < 0)
                            data.bakingYBuffer = 32;
                        if (data.memoryPollInterval <= 0)
                            data.memoryPollInterval = 20;

                        enableTerrainBakingOptimization = data.enableTerrainBakingOptimization;
                        bakingYBuffer = data.bakingYBuffer;
                        memoryPollInterval = data.memoryPollInterval;
                        enableSneakFix = data.enableSneakFix;
                        enableVillagerFix = data.enableVillagerFix;
                        showDebugHUD = data.showDebugHUD;
                        save();
                    }
                }
            } else {
                save(); // デフォルトで作成
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        if (managedByServer)
            return; // サーバー管理中はローカル保存しない

        try {
            ConfigData data = new ConfigData();
            data.enableTerrainBakingOptimization = enableTerrainBakingOptimization;
            data.bakingYBuffer = bakingYBuffer;
            data.memoryPollInterval = memoryPollInterval;
            data.enableSneakFix = enableSneakFix;
            data.enableVillagerFix = enableVillagerFix;
            data.showDebugHUD = showDebugHUD;

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
