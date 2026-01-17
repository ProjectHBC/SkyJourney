package celeste.skyjourney.config;

public class SkyJourneyConfig {

    // サーバー
    public static boolean enableTerrainBakingOptimization = true;
    public static int bakingYBuffer = 128;
    public static int memoryPollInterval = 20;

    // クライアント
    public static boolean showDebugHUD = false;

    private static final java.nio.file.Path CONFIG_PATH = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getConfigDir().resolve("skyjourney.properties");

    public static void load() {
        java.util.Properties props = new java.util.Properties();
        try {
            if (java.nio.file.Files.exists(CONFIG_PATH)) {
                try (java.io.InputStream in = java.nio.file.Files.newInputStream(CONFIG_PATH)) {
                    props.load(in);
                }
                enableTerrainBakingOptimization = Boolean
                        .parseBoolean(props.getProperty("enableTerrainBakingOptimization", "true"));
                bakingYBuffer = Integer.parseInt(props.getProperty("bakingYBuffer", "128"));
                memoryPollInterval = Integer.parseInt(props.getProperty("memoryPollInterval", "20"));
                showDebugHUD = Boolean.parseBoolean(props.getProperty("showDebugHUD", "false"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("enableTerrainBakingOptimization", String.valueOf(enableTerrainBakingOptimization));
        props.setProperty("bakingYBuffer", String.valueOf(bakingYBuffer));
        props.setProperty("memoryPollInterval", String.valueOf(memoryPollInterval));
        props.setProperty("showDebugHUD", String.valueOf(showDebugHUD));
        try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "SkyJourney Configuration");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
