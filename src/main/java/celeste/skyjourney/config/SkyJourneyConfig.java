package celeste.skyjourney.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import celeste.skyjourney.mixin.plugin.SkyJourneyPluginState;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class SkyJourneyConfig {
    // サーバー
    public boolean enableTerrainBakingOptimization = true;
    public int bakingYBuffer = 32;
    public int memoryPollInterval = 20;

    // 機能トグル
    public boolean enableSneakFix = true;
    public boolean enableVillagerFix = true;
    public boolean enableBalloonPPEFix = true;
    public boolean enableDrawerFix = false;

    // クライアント
    public boolean showDebugHUD = false;

    // サーバー管理フラグ
    private transient boolean managedByServer = false;

    // --- システム部分 ---
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("skyjourney.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Singleton
    private SkyJourneyConfig() {}
    private static SkyJourneyConfig instance = new SkyJourneyConfig();

    // 外部アクセス
    public static SkyJourneyConfig getInstance() { return instance; }
    public boolean isManagedByServer() { return managedByServer; }

    // サーバー設定を適用
    public static void applyServerConfig(String json) {
        try {
            SkyJourneyConfig data = GSON.fromJson(json, SkyJourneyConfig.class);
            if (data != null) {
                data.managedByServer = true;
                data.validate();

                // クライアント設定は同期しない
                data.showDebugHUD = instance.showDebugHUD;

                // それ以外を同期させる
                instance = data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void restoreLocalConfig() {
        instance.managedByServer = false;
        load();
    }

    public static String serialize() {
        return GSON.toJson(instance);
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    SkyJourneyConfig loaded = GSON.fromJson(reader, SkyJourneyConfig.class);
                    if (loaded != null) {
                        loaded.validate();
                        instance = loaded;
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
        if (instance.managedByServer) { return; } // サーバー管理中はローカル保存しない

        try {
            instance.validate();
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 整合性チェック
    private void validate() {
        if (bakingYBuffer < 0) { bakingYBuffer = 32; }
        if (memoryPollInterval <= 0) { memoryPollInterval = 20; }
        SkyJourneyPluginState.validatePlugin();
    }
}