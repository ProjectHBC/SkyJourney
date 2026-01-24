package celeste.skyjourney.command;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import celeste.skyjourney.command.data.DataCommand;
import celeste.skyjourney.common.VSHelper;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;

public class SkyJourneyCommand {
    /**
     * skyjourney用の基礎コマンド登録
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("sj")
                .then(DataCommand.build()));
    }

    public static CompletableFuture<Suggestions> suggestShipSlug(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        try {
            ServerWorld world = context.getSource().getWorld();
            List<String> shipSlugList = VSHelper.getShipSlugList(world);
            if (shipSlugList != null) {
                for (String slug : shipSlugList) {
                    builder.suggest(slug);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // 補完でエラーが出てもクラッシュさせない
        }
        return builder.buildFuture();
    }

    @Nullable
    public static ServerShip getShipBySlug(ServerWorld world, String slug) {
        QueryableShipData<Ship> shipData = VSGameUtilsKt.getAllShips(world);
        for (Ship ship : shipData) {
            if (ship.getSlug().equals(slug)) { return (ServerShip) ship; }
        }
        return null; // 見つからなかったら null
    }
}