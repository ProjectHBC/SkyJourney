package celeste.skyjourney.config;

import celeste.skyjourney.feature.sneak.SneakMode;
import celeste.skyjourney.mixin.plugin.SkyJourneyPluginState;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.text.Text;

public class SkyJourneyServerConfig {

    /**
     * サーバーコンフィグ設定
     */
    protected static void buildCategory(ConfigEntryBuilder entryBuilder, ConfigCategory category) {
        // 情報
        boolean isLocked = SkyJourneyConfig.getInstance().isManagedByServer();
        boolean isExtendedDrawersLocked = !SkyJourneyPluginState.getExtendedDrawersLoaded();
        Text tooltipSuffix = isLocked ? Text.of(" (Managed by Server - Cannot Edit)") : Text.of("");
        if (isLocked) { category.addEntry(entryBuilder.startTextDescription(Text.of("These settings are enforced by the server.")).build()); }

        // 最適化のサブカテゴリ
        SubCategoryBuilder optimizationSubCategory = entryBuilder.startSubCategory(Text.literal("Optimazation"));
        var terrainBakingEntry = entryBuilder
            .startBooleanToggle(Text.of("Enable Terrain Baking Optimization"), SkyJourneyConfig.getInstance().enableTerrainBakingOptimization)
            .setDefaultValue(true)
            .setTooltip(Text.of("If enabled, terrain outside the configured Y range will be ignored by VS2 physics.").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableTerrainBakingOptimization = newValue; }})
            .build();
        if (isLocked) { terrainBakingEntry.setEditable(false); }
        optimizationSubCategory.add(terrainBakingEntry);

        var bakingYEntry = entryBuilder
            .startIntField(Text.of("Baking Y Buffer"), SkyJourneyConfig.getInstance().bakingYBuffer)
            .setDefaultValue(32)
            .setTooltip(Text.of("Distance (blocks) above/below ships to permit terrain baking.").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().bakingYBuffer = newValue; }})
            .build();
        if (isLocked) { bakingYEntry.setEditable(false); }
        optimizationSubCategory.add(bakingYEntry);

         var memoryEntry = entryBuilder
            .startIntField(Text.of("Memory Poll Interval (Ticks)"), SkyJourneyConfig.getInstance().memoryPollInterval)
            .setDefaultValue(20)
            .setTooltip(Text.of("How often to calculate and sync memory stats.").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().memoryPollInterval = newValue; }})
            .build();
        if (isLocked) { memoryEntry.setEditable(false); }
        optimizationSubCategory.add(memoryEntry);

        category.addEntry(optimizationSubCategory.build()); // サブカテゴリの登録

        // スニークのサブカテゴリ
        SubCategoryBuilder sneakSubCategory = entryBuilder.startSubCategory(Text.literal("Sneak"));
        var sneakModeEntry = entryBuilder
            .startEnumSelector(Text.of("Sneak Fix Mode"), SneakMode.class, SkyJourneyConfig.getInstance().sneakMode)
            .setDefaultValue(SneakMode.SMART_ANCHOR)
            .setTooltip(Text.of("Select the collision logic for sneaking on ships.").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().sneakMode = newValue; }})
            .build();
        if (isLocked) { sneakModeEntry.setEditable(false); }
        sneakSubCategory.add(sneakModeEntry);

        var sneakEntry = entryBuilder
            .startBooleanToggle(Text.of("Enable Sneak Fix"), SkyJourneyConfig.getInstance().enableSneakFix)
            .setDefaultValue(true)
            .setTooltip(Text.of("Prevents falling off inclined ships while sneaking.").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableSneakFix = newValue; }})
            .build();
        if (isLocked) { sneakEntry.setEditable(false); }
        sneakSubCategory.add(sneakEntry);

        category.addEntry(sneakSubCategory.build()); // サブカテゴリの登録

        // エンティティのサブカテゴリ
        SubCategoryBuilder entitySubCategory = entryBuilder.startSubCategory(Text.literal("Entity"));
        var villagerEntry = entryBuilder
            .startBooleanToggle(Text.of("Enable Villager Fix"), SkyJourneyConfig.getInstance().enableVillagerFix)
            .setDefaultValue(true)
            .setTooltip(Text.of("Allows villagers to work and restock on ships.").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableVillagerFix = newValue; }})
            .build();
        if (isLocked) { villagerEntry.setEditable(false); }
        entitySubCategory.add(villagerEntry);

        var eatGrassEntry = entryBuilder
            .startBooleanToggle(Text.of("Enable Grass Fix"), SkyJourneyConfig.getInstance().enableEatGrassFix)
            .setDefaultValue(true)
            .setTooltip(Text.of("Allow sheep to eat grass on the ship.").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableEatGrassFix = newValue; }})
            .build();
        if (isLocked) { eatGrassEntry.setEditable(false); }
        entitySubCategory.add(eatGrassEntry);

        category.addEntry(entitySubCategory.build()); // サブカテゴリの登録

        // PPE(投擲物)のサブカテゴリ
        SubCategoryBuilder ppeSubCategory = entryBuilder.startSubCategory(Text.literal("Projectile"));
        var balloonPPEEntry = entryBuilder
            .startBooleanToggle(Text.of("Enable Balloon PersistentProjectileEntity Fix"), SkyJourneyConfig.getInstance().enableBalloonPPEFix)
            .setDefaultValue(true)
            .setTooltip(Text.of("Prevents balloons from being broken by projectiles (e.g., arrows, tridents).").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enableBalloonPPEFix = newValue; }})
            .build();
        if (isLocked) { balloonPPEEntry.setEditable(false); }
        ppeSubCategory.add(balloonPPEEntry);

        var ppeOnShipFixEntry = entryBuilder
            .startBooleanToggle(Text.of("Enable PersistentProjectileEntity Fix"), SkyJourneyConfig.getInstance().enablePPEOnShipFix)
            .setDefaultValue(true)
            .setTooltip(Text.of("Prevents projectiles from getting stuck on the ship (e.g., arrows, tridents).").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked) { SkyJourneyConfig.getInstance().enablePPEOnShipFix = newValue; }})
            .build();
        if (isLocked) { ppeOnShipFixEntry.setEditable(false); }
        ppeSubCategory.add(ppeOnShipFixEntry);

        category.addEntry(ppeSubCategory.build()); // サブカテゴリの登録

        // Plugin(他Mod関係)のサブカテゴリ
        SubCategoryBuilder pluginSubCategory = entryBuilder.startSubCategory(Text.literal("Plugin"));
        var drawerFixEntry = entryBuilder
            .startBooleanToggle(Text.of("Enable Extended Drawers Fix"), SkyJourneyConfig.getInstance().enableDrawerFix)
            .setDefaultValue(false)
            .setTooltip(Text.of("Patches Extended Drawers to work correctly on ships.").copy().append(tooltipSuffix))
            .setSaveConsumer(newValue -> { if (!isLocked || !isExtendedDrawersLocked) { SkyJourneyConfig.getInstance().enableDrawerFix = newValue; }})
            .build();
        if (isLocked || isExtendedDrawersLocked) { drawerFixEntry.setEditable(false); }
        pluginSubCategory.add(drawerFixEntry);

        category.addEntry(pluginSubCategory.build()); // サブカテゴリの登録
    }
}
