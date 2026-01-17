package celeste.skyjourney.network;

import celeste.skyjourney.SkyJourneyMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

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
}
