package celeste.skyjourney.mixin.plugin;

import celeste.skyjourney.SkyJourneyMod;
import celeste.skyjourney.feature.FeatureManager.Config;

// Mixin用のプラグイン状態管理
public class SkyJourneyPluginState {
    // Accessor
    public static boolean isExtendedDrawersLoaded = false;
    static void setExtendedDrawersLoaded(boolean loaded) { isExtendedDrawersLoaded = loaded; }
    public static boolean getExtendedDrawersLoaded() { return isExtendedDrawersLoaded; }

    /**
     * 各機能のプラグイン(Mod導入)の有無をチェックする
     */
    public static void checkPlugin(Config config) {
        if (!getExtendedDrawersLoaded()) {
            SkyJourneyMod.LOGGER.warn("ExtendedDrawersMod is not loaded. DrawerInteractionFix is disabled.");
            config.enableDrawerFix = false;
        }
    }
}
