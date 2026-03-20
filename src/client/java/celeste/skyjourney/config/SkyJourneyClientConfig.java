package celeste.skyjourney.config;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class SkyJourneyClientConfig {
 
    /**
     * クライアント設定
     */
    protected static void buildCategory(ConfigEntryBuilder entryBuilder, ConfigCategory category) {
        var showDebugHUDEntry = entryBuilder
            .startBooleanToggle(Text.of("Show Debug HUD"), SkyJourneyConfig.getInstance().showDebugHUD)
            .setDefaultValue(false)
            .setTooltip(Text.of("Displays memory usage and optimization stats on screen."))
            .setSaveConsumer(newValue -> SkyJourneyConfig.getInstance().showDebugHUD = newValue)
            .build();
        category.addEntry(showDebugHUDEntry);

        var showDebugSneakBoxEntry = entryBuilder
            .startBooleanToggle(Text.of("Show Debug Sneak Box"), SkyJourneyConfig.getInstance().showDebugSneakBox)
            .setDefaultValue(false)
            .setTooltip(Text.of("Displays bounding box used when sneaking on the ship visible."))
            .setSaveConsumer(newValue -> SkyJourneyConfig.getInstance().showDebugSneakBox = newValue)
            .build();
        category.addEntry(showDebugSneakBoxEntry);
    }
}
