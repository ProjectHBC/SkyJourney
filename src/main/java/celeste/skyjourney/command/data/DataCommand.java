package celeste.skyjourney.command.data;

import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServer;
import org.valkyrienskies.eureka.EurekaConfig;
import org.valkyrienskies.eureka.ship.EurekaShipControl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import celeste.skyjourney.command.SkyJourneyCommand;
import celeste.skyjourney.common.VSHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


import javax.annotation.Nullable;

public class DataCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("data")
            .then(CommandManager.literal("get")
                .then(CommandManager.literal("slug")
                    .executes(DataCommand::getSlug)) // 名前(slug)を調べるのでslug補助は付けない
                .then(CommandManager.literal("name")
                    .executes(DataCommand::getSlug))
                .then(CommandManager.literal("ship_mass")
                    .executes(DataCommand::getShipMass)
                    .then(CommandManager.argument("slug", StringArgumentType.string())
                        .suggests(SkyJourneyCommand::suggestShipSlug)
                        .executes(DataCommand::getShipMass)))
                .then(CommandManager.literal("weight")
                    .executes(DataCommand::getWeight)
                    .then(CommandManager.argument("slug", StringArgumentType.string())
                        .suggests(SkyJourneyCommand::suggestShipSlug)
                        .executes(DataCommand::getWeight)))
                .then(CommandManager.literal("info")
                    .executes(DataCommand::getInfo)
                    .then(CommandManager.argument("slug", StringArgumentType.string())
                        .suggests(SkyJourneyCommand::suggestShipSlug)
                        .executes(DataCommand::getInfo)))
                .then(CommandManager.literal("speed")
                    .executes(DataCommand::getSpeed)
                    .then(CommandManager.argument("slug", StringArgumentType.string())
                        .suggests(SkyJourneyCommand::suggestShipSlug)
                        .executes(DataCommand::getSpeed))));
    }

    // slugとnameの処理
    private static int getSlug(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerShip ship = searchShip(context);
        if (ship == null) {
            source.sendMessage(Text.literal("船が見つかりません").formatted(Formatting.RED));
            return 0;
        }
        source.sendMessage(Text.literal("船の名前: " + ship.getSlug()).formatted(Formatting.AQUA));
        return 1;
    }

    // ship_massの処理
    private static int getShipMass(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerShip ship = searchShip(context);
        if (ship == null) {
            source.sendMessage(Text.literal("船が見つかりません").formatted(Formatting.RED));
            return 0;
        }

        double mass = ship.getInertiaData().getMass();
        source.sendMessage(Text.literal(ship.getSlug() + " の質量: " + formatMass(mass)).formatted(Formatting.AQUA));
        return 1;
    }

    // weightの処理
    private static int getWeight(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerShip ship = searchShip(context);
        if (ship == null) {
            source.sendMessage(Text.literal("船が見つかりません").formatted(Formatting.RED));
            return 0;
        }

        // 風船の数を調べる
        EurekaShipControl control = getShipControl(ship);
        Integer balloons = null;
        if (control != null) { balloons = control.getBalloons(); }
        if (balloons == null) {
            source.sendMessage(Text.literal("取得に失敗しました").formatted(Formatting.RED));
            return 0;
        }

        double mass = ship.getInertiaData().getMass();
        double maxWeight = balloons * EurekaConfig.SERVER.getMassPerBalloon(); // デフォルトは5000
        double weight = maxWeight - mass; // 船の総重量を調べる
        source.sendMessage(Text.literal(ship.getSlug() + " : " + formatMass(weight) + " / " + formatMass(maxWeight) + " | 風船: " + balloons).formatted(Formatting.AQUA));
        return 1;
    }

    // infoの処理
    private static int getInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerShip ship = searchShip(context);
        if (ship == null) {
            source.sendMessage(Text.literal("船が見つかりません").formatted(Formatting.RED));
            return 0;
        }

        // データの取得
        String slug = ship.getSlug();
        double mass = ship.getInertiaData().getMass();
        Vector3dc velocity = ship.getVelocity();
        double speedMetersPerSecond = velocity.length(); // 秒速の取得
        Vector3dc worldPos = ship.getTransform().getPositionInWorld();
        AABBic aabb = ship.getShipAABB();

        source.sendMessage(Text.literal(
            "Slug: " + slug + ",\n" + 
            "Pos: " + String.format("%.2f", worldPos.x()) + ", " + String.format("%.2f", worldPos.y()) + ", " + String.format("%.2f", worldPos.z()) + ",\n" +
            "Mass: " + formatMass(mass) + ",\n" +
            "Speed: " + String.format("%.2f", speedMetersPerSecond) + "m/s,\n" +
            "AABB: " + aabb
        ).formatted(Formatting.AQUA));
        return 1;
    }

    // speedの処理
    private static int getSpeed(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerShip ship = searchShip(context);
        if (ship == null) {
            source.sendMessage(Text.literal("船が見つかりません").formatted(Formatting.RED));
            return 0;
        }

        // データの取得
        String slug = ship.getSlug();
        Vector3dc velocity = ship.getVelocity();
        double speedMetersPerSecond = (velocity != null) ? velocity.length() : 0.0; // 直線移動速度(m/s)の取得
        Quaterniondc rotation = ship.getTransform().getShipToWorldRotation();

        source.sendMessage(Text.literal(
            "Slug: " + slug + ",\n" +
            "Speed: " + String.format("%.2f", speedMetersPerSecond) + "m/s,\n" +
            "Rotation: " + rotation.toString()
        ).formatted(Formatting.AQUA));
        return 1;
    }

    /**
     * コントロールを取得する
     * 
     * @param ship 船
     * @return EurekaShipControl
     */
    @Nullable
    private static EurekaShipControl getShipControl(ServerShip ship) {
        EurekaShipControl control = null;
        if (ship instanceof ShipData) {
            control = ((ShipData) ship).getAttachment(EurekaShipControl.class);
        } else if (ship instanceof ShipObjectServer) {
            control = ((ShipObjectServer) ship).getAttachment(EurekaShipControl.class);
        }

        return control;
    }

    // kg -> t
    private static String formatMass(double massKg) {
        double massTons = massKg / 1000.0;
        return String.format("%,.2f t", massTons);
    }

    @Nullable
    private static ServerShip searchShip(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        ServerShip ship = null;
        String slug = null;

        // slugが指定されている場合はそれを使用する
        try { slug = StringArgumentType.getString(context, "slug"); } catch (IllegalArgumentException ignored) {}

        if (slug != null) {
            ship = SkyJourneyCommand.getShipBySlug(world, slug);
        } else {
            if (source.getEntity() == null) {
                source.sendMessage(Text.literal("このコマンドはプレイヤーのみ実行可能です").formatted(Formatting.RED));
                return null;
            }
            ship = (ServerShip) VSHelper.getShipManagingOrIntersecting(source.getEntity());
        }

        return ship;
    }
}