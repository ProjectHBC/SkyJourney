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

                        boolean isLocked = SkyJourneyConfig.isManagedByServer();
                        Text tooltipSuffix = isLocked ? Text.of(" (Managed by Server - Cannot Edit)") : Text.of("");

                        ConfigCategory serverCategory = builder.getOrCreateCategory(Text.of("Server Settings"));

                        if (isLocked) {
                                serverCategory.addEntry(entryBuilder
                                                .startTextDescription(
                                                                Text.of("These settings are enforced by the server."))
                                                .build());
                        }

                        var terrainBakingEntry = entryBuilder
                                        .startBooleanToggle(Text.of("Enable Terrain Baking Optimization"),
                                                        SkyJourneyConfig.enableTerrainBakingOptimization)
                                        .setDefaultValue(true)
                                        .setTooltip(Text.of(
                                                        "If enabled, terrain outside the configured Y range will be ignored by VS2 physics.")
                                                        .copy().append(tooltipSuffix))
                                        .setSaveConsumer(newValue -> {
                                                if (!isLocked)
                                                        SkyJourneyConfig.enableTerrainBakingOptimization = newValue;
                                        })
                                        .build();
                        if (isLocked)
                                terrainBakingEntry.setEditable(false);
                        serverCategory.addEntry(terrainBakingEntry);

                        var bakingYEntry = entryBuilder
                                        .startIntField(Text.of("Baking Y Buffer"),
                                                        SkyJourneyConfig.bakingYBuffer)
                                        .setDefaultValue(32)
                                        .setTooltip(Text.of(
                                                        "Distance (blocks) above/below ships to permit terrain baking.")
                                                        .copy().append(tooltipSuffix))
                                        .setSaveConsumer(newValue -> {
                                                if (!isLocked)
                                                        SkyJourneyConfig.bakingYBuffer = newValue;
                                        })
                                        .build();
                        if (isLocked)
                                bakingYEntry.setEditable(false);
                        serverCategory.addEntry(bakingYEntry);

                        var sneakEntry = entryBuilder
                                        .startBooleanToggle(Text.of("Enable Sneak Fix"),
                                                        SkyJourneyConfig.enableSneakFix)
                                        .setDefaultValue(true)
                                        .setTooltip(Text.of("Prevents falling off inclined ships while sneaking.")
                                                        .copy().append(tooltipSuffix))
                                        .setSaveConsumer(newValue -> {
                                                if (!isLocked)
                                                        SkyJourneyConfig.enableSneakFix = newValue;
                                        })
                                        .build();
                        if (isLocked)
                                sneakEntry.setEditable(false);
                        serverCategory.addEntry(sneakEntry);

                        var villagerEntry = entryBuilder
                                        .startBooleanToggle(Text.of("Enable Villager Fix"),
                                                        SkyJourneyConfig.enableVillagerFix)
                                        .setDefaultValue(true)
                                        .setTooltip(Text.of("Allows villagers to work and restock on ships.").copy()
                                                        .append(tooltipSuffix))
                                        .setSaveConsumer(newValue -> {
                                                if (!isLocked)
                                                        SkyJourneyConfig.enableVillagerFix = newValue;
                                        })
                                        .build();
                        if (isLocked)
                                villagerEntry.setEditable(false);
                        serverCategory.addEntry(villagerEntry);

                        var memoryEntry = entryBuilder
                                        .startIntField(Text.of("Memory Poll Interval (Ticks)"),
                                                        SkyJourneyConfig.memoryPollInterval)
                                        .setDefaultValue(20)
                                        .setTooltip(Text.of("How often to calculate and sync memory stats.").copy()
                                                        .append(tooltipSuffix))
                                        .setSaveConsumer(newValue -> {
                                                if (!isLocked)
                                                        SkyJourneyConfig.memoryPollInterval = newValue;
                                        })
                                        .build();
                        if (isLocked)
                                memoryEntry.setEditable(false);
                        serverCategory.addEntry(memoryEntry);

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
