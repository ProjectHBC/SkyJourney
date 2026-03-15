package celeste.skyjourney.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import celeste.skyjourney.mixin.plugin.SkyJourneyPluginState;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class SkyJourneyModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder
                .create()
                .setParentScreen(parent)
                .setTitle(Text.of("SkyJourney Config"))
                .setSavingRunnable(SkyJourneyConfig::save);

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            boolean isLocked = SkyJourneyConfig.getInstance().isManagedByServer();
            boolean isExtendedDrawersLocked = !SkyJourneyPluginState.getExtendedDrawersLoaded();
            Text tooltipSuffix = isLocked ? Text.of(" (Managed by Server - Cannot Edit)") : Text.of("");

            ConfigCategory serverCategory = builder.getOrCreateCategory(Text.of("Server Settings"));

            if (isLocked) { serverCategory.addEntry(entryBuilder.startTextDescription(Text.of("These settings are enforced by the server.")).build()); }

            // --- サーバーコンフィグ設定領域 ---
            var terrainBakingEntry = entryBuilder
                .startBooleanToggle(Text.of("Enable Terrain Baking Optimization"), SkyJourneyConfig.getInstance().enableTerrainBakingOptimization)
                .setDefaultValue(true)
                .setTooltip(Text.of("If enabled, terrain outside the configured Y range will be ignored by VS2 physics.").copy().append(tooltipSuffix))
                .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableTerrainBakingOptimization = newValue; }})
                .build();
            if (isLocked) { terrainBakingEntry.setEditable(false); }
            serverCategory.addEntry(terrainBakingEntry);

            var bakingYEntry = entryBuilder
                .startIntField(Text.of("Baking Y Buffer"), SkyJourneyConfig.getInstance().bakingYBuffer)
                .setDefaultValue(32)
                .setTooltip(Text.of("Distance (blocks) above/below ships to permit terrain baking.").copy().append(tooltipSuffix))
                .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().bakingYBuffer = newValue; }})
                .build();
            if (isLocked) { bakingYEntry.setEditable(false); }
            serverCategory.addEntry(bakingYEntry);

            var sneakEntry = entryBuilder
                .startBooleanToggle(Text.of("Enable Sneak Fix"), SkyJourneyConfig.getInstance().enableSneakFix)
                .setDefaultValue(true)
                .setTooltip(Text.of("Prevents falling off inclined ships while sneaking.").copy().append(tooltipSuffix))
                .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableSneakFix = newValue; }})
                .build();
            if (isLocked) { sneakEntry.setEditable(false); }
            serverCategory.addEntry(sneakEntry);

            var villagerEntry = entryBuilder
                .startBooleanToggle(Text.of("Enable Villager Fix"), SkyJourneyConfig.getInstance().enableVillagerFix)
                .setDefaultValue(true)
                .setTooltip(Text.of("Allows villagers to work and restock on ships.").copy().append(tooltipSuffix))
                .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableVillagerFix = newValue; }})
                .build();
            if (isLocked) { villagerEntry.setEditable(false); }
            serverCategory.addEntry(villagerEntry);

            var balloonPPEEntry = entryBuilder
                .startBooleanToggle(Text.of("Enable Balloon PersistentProjectileEntity Fix"), SkyJourneyConfig.getInstance().enableBalloonPPEFix)
                .setDefaultValue(true)
                .setTooltip(Text.of("Prevents balloons from being broken by projectiles (e.g., arrows, tridents).").copy().append(tooltipSuffix))
                .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableBalloonPPEFix = newValue; }})
                .build();
            if (isLocked) { balloonPPEEntry.setEditable(false); }
            serverCategory.addEntry(balloonPPEEntry);

            var drawerFixEntry = entryBuilder
                .startBooleanToggle(Text.of("Enable Extended Drawers Fix"), SkyJourneyConfig.getInstance().enableDrawerFix)
                .setDefaultValue(false)
                .setTooltip(Text.of("Patches Extended Drawers to work correctly on ships.").copy().append(tooltipSuffix))
                .setSaveConsumer(newValue -> { if (!isLocked || !isExtendedDrawersLocked) { SkyJourneyConfig.getInstance().enableDrawerFix = newValue; }})
                .build();
            if (isLocked || isExtendedDrawersLocked) { drawerFixEntry.setEditable(false); }
            serverCategory.addEntry(drawerFixEntry);

            var memoryEntry = entryBuilder
                .startIntField(Text.of("Memory Poll Interval (Ticks)"), SkyJourneyConfig.getInstance().memoryPollInterval)
                .setDefaultValue(20)
                .setTooltip(Text.of("How often to calculate and sync memory stats.").copy().append(tooltipSuffix))
                .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().memoryPollInterval = newValue; }})
                .build();
            if (isLocked) { memoryEntry.setEditable(false); }
            serverCategory.addEntry(memoryEntry);

			// --- クライアント設定領域 ---
			ConfigCategory clientCategory = builder.getOrCreateCategory(Text.of("Client Settings"));

			var showDebugHUDEntry = entryBuilder
				.startBooleanToggle(Text.of("Show Debug HUD"), SkyJourneyConfig.getInstance().showDebugHUD)
				.setDefaultValue(false)
				.setTooltip(Text.of("Displays memory usage and optimization stats on screen."))
				.setSaveConsumer(newValue -> SkyJourneyConfig.getInstance().showDebugHUD = newValue)
				.build();
			clientCategory.addEntry(showDebugHUDEntry);

            var showDebugSneakBoxEntry = entryBuilder
				.startBooleanToggle(Text.of("Show Debug Sneak Box"), SkyJourneyConfig.getInstance().showDebugSneakBox)
				.setDefaultValue(false)
				.setTooltip(Text.of("Displays bounding box used when sneaking on the ship visible."))
				.setSaveConsumer(newValue -> SkyJourneyConfig.getInstance().showDebugSneakBox = newValue)
				.build();
			clientCategory.addEntry(showDebugSneakBoxEntry);

			return builder.build();
		};
	}
}
