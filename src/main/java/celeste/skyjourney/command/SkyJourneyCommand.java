package celeste.skyjourney.command;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;

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
import net.minecraft.world.World;

public class SkyJourneyCommand {
    /**
     * skyjourney用の基礎コマンド登録
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("sj")
                // data: データに関するコマンド
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
        try {
            // VSGameUtilsKt から ShipObjectWorld を取得 (リフレクション)
            Class<?> utilsClass = Class.forName("org.valkyrienskies.mod.common.VSGameUtilsKt");
            java.lang.reflect.Method getShipWorldMethod = utilsClass.getMethod("getShipObjectWorld", World.class);
            Object shipWorld = getShipWorldMethod.invoke(null, world);

            if (shipWorld != null) {
                // 全船データを取得
                java.lang.reflect.Method getDataMethod = shipWorld.getClass().getMethod("getQueryableShipData");
                @SuppressWarnings("unchecked")
                Iterable<Ship> allShips = (Iterable<Ship>) getDataMethod.invoke(shipWorld);

                for (Ship ship : allShips) {
                    if (slug.equals(ship.getSlug())) {
                        return (ServerShip) ship;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // 見つからなかったら null
    }
}