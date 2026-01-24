package celeste.skyjourney.network;

import celeste.skyjourney.SkyJourneyMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class PacketHandler {

    // サーバーサイド・クライアントサイド共通のパケットハンドラー
    // メモリ統計情報の送受信を管理
    public static final Identifier MEMORY_STATS_ID = new Identifier(SkyJourneyMod.MOD_ID, "memory_stats");

    // 全プレイヤーにメモリ統計情報を送信
    public static void sendToAll(List<ServerPlayerEntity> players, long total, long free, long max, int skipped,
            int totalChunks, int minY, int maxY) {

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(total);
        buf.writeLong(free);
        buf.writeLong(max);
        buf.writeInt(skipped);
        buf.writeInt(totalChunks);
        buf.writeInt(minY);
        buf.writeInt(maxY);

        for (ServerPlayerEntity player : players) {
            ServerPlayNetworking.send(player, MEMORY_STATS_ID, buf);
        }
    }

    public static final Identifier CHUNK_DEBUG_ID = new Identifier(SkyJourneyMod.MOD_ID, "chunk_debug");

    // デバッグ用のチャンク情報を送信
    public static void sendChunkDebug(List<ServerPlayerEntity> players, List<Integer> data) {
        if (data.isEmpty())
            return;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(data.size());
        for (int i : data) {
            buf.writeInt(i);
        }

        for (ServerPlayerEntity player : players) {
            ServerPlayNetworking.send(player, CHUNK_DEBUG_ID, buf);
        }
    }

    public static final Identifier CONFIG_SYNC_ID = new Identifier(SkyJourneyMod.MOD_ID, "config_sync");

    // クライアントへサーバー設定を送信
    public static void sendConfigSync(ServerPlayerEntity player, String jsonConfig) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(jsonConfig);
        ServerPlayNetworking.send(player, CONFIG_SYNC_ID, buf);
    }
    // ブロックの重さデータを同期するパケットID
    public static final Identifier MASS_SYNC_ID = new Identifier(SkyJourneyMod.MOD_ID, "mass_sync");

    // クライアントへサーバーの重さデータを送信
    public static void sendMassSync(ServerPlayerEntity player) {
        try {
            // リフレクションを使用してMassDatapackResolver.INSTANCEを取得
            Class<?> resolverClass = Class.forName("org.valkyrienskies.mod.common.config.MassDatapackResolver");
            Field instanceField = resolverClass.getDeclaredField("INSTANCE");
            Object resolverInstance = instanceField.get(null);

            // 'map'重さデータを取得
            Field mapField = resolverClass.getDeclaredField("map");
            mapField.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) mapField.get(resolverInstance);

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(map.size());

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Identifier id = (Identifier) entry.getKey();
                Object info = entry.getValue();

                // VSBlockStateInfoから'mass'フィールドを取得
                Field massField = info.getClass().getDeclaredField("mass");
                massField.setAccessible(true);
                double mass = massField.getDouble(info);

                buf.writeIdentifier(id);
                buf.writeDouble(mass);
            }

            ServerPlayNetworking.send(player, MASS_SYNC_ID, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
