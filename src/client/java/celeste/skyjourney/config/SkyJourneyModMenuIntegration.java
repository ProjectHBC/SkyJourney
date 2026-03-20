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
            ConfigBuilder builder = ConfigBuilder
                .create()
                .setParentScreen(parent)
                .setTitle(Text.of("SkyJourney Config"))
                .setSavingRunnable(SkyJourneyConfig::save);
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // サーバーコンフィグ
            ConfigCategory serverCategory = builder.getOrCreateCategory(Text.of("Server Settings"));
            SkyJourneyServerConfig.buildCategory(entryBuilder, serverCategory);

            // クライアントコンフィグ
            ConfigCategory clientCategory = builder.getOrCreateCategory(Text.of("Client Settings"));
            SkyJourneyClientConfig.buildCategory(entryBuilder, clientCategory);

			return builder.build();
		};
	}
}
