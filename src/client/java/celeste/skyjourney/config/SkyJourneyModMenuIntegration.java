package celeste.skyjourney.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class SkyJourneyModMenuIntegration implements ModMenuApi {
        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
                return parent -> {
                        ConfigBuilder builder = ConfigBuilder.create()
                                        .setParentScreen(parent)
                                        .setTitle(Text.of("SkyJourney Config"))
                                        .setSavingRunnable(SkyJourneyConfig::save);

                        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                        ConfigCategory serverCategory = builder.getOrCreateCategory(Text.of("Server Settings"));

                        serverCategory.addEntry(entryBuilder
                                        .startBooleanToggle(Text.of("Enable Terrain Baking Optimization"),
                                                        SkyJourneyConfig.enableTerrainBakingOptimization)
                                        .setDefaultValue(true)
                                        .setTooltip(Text
                                                        .of("If enabled, terrain outside the configured Y range will be ignored by VS2 physics."))
                                        .setSaveConsumer(
                                                        newValue -> SkyJourneyConfig.enableTerrainBakingOptimization = newValue)
                                        .build());

                        serverCategory.addEntry(entryBuilder
                                        .startIntField(Text.of("Baking Y Buffer"), SkyJourneyConfig.bakingYBuffer)
                                        .setDefaultValue(32)
                                        .setTooltip(Text.of(
                                                        "Distance (blocks) above/below ships to permit terrain baking."))
                                        .setSaveConsumer(newValue -> SkyJourneyConfig.bakingYBuffer = newValue)
                                        .build());

                        serverCategory.addEntry(entryBuilder
                                        .startIntField(Text.of("Memory Poll Interval (Ticks)"),
                                                        SkyJourneyConfig.memoryPollInterval)
                                        .setDefaultValue(20)
                                        .setTooltip(Text.of("How often to calculate and sync memory stats."))
                                        .setSaveConsumer(newValue -> SkyJourneyConfig.memoryPollInterval = newValue)
                                        .build());

                        ConfigCategory clientCategory = builder.getOrCreateCategory(Text.of("Client Settings"));

                        clientCategory
                                        .addEntry(entryBuilder
                                                        .startBooleanToggle(Text.of("Show Debug HUD"),
                                                                        SkyJourneyConfig.showDebugHUD)
                                                        .setDefaultValue(false)
                                                        .setTooltip(Text.of(
                                                                        "Displays memory usage and optimization stats on screen."))
                                                        .setSaveConsumer(
                                                                        newValue -> SkyJourneyConfig.showDebugHUD = newValue)
                                                        .build());

                        return builder.build();
                };
        }
}
