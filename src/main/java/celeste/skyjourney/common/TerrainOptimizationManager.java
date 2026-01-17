package celeste.skyjourney.common;

import celeste.skyjourney.config.SkyJourneyConfig;
import celeste.skyjourney.network.PacketHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.world.chunks.TerrainUpdate;
import org.valkyrienskies.mod.common.IShipObjectWorldServerProvider;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;
import org.valkyrienskies.mod.common.util.DimensionIdProvider;
import org.valkyrienskies.mod.common.util.VSServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TerrainOptimizationManager {

    private static int tickCounter = 0;

    public static int allowedMinY = -2048;
    public static int allowedMaxY = 2048;

    // スレッドセーフな範囲リストを保持
    public static volatile List<int[]> allowedRanges = Collections.emptyList();

    // メトリクス
    public static int skippedChunks = 0;
    public static int totalChunks = 0;

    // デバッグキュー
    public static final ConcurrentLinkedQueue<int[]> debugQueue = new ConcurrentLinkedQueue<>();

    public static void onWorldTick(ServerWorld world) {
        if (world.isClient)
            return;

        if (world.getTime() % SkyJourneyConfig.memoryPollInterval == 0) {
            runMemoryPoll(world);
        }

        // デバッグキューを毎ティックフラッシュ
        if (!debugQueue.isEmpty()) {
            List<Integer> flatData = new ArrayList<>();
            int count = 0;
            // オーバーフローを防ぐため、1ティックあたり50イベント
            while (!debugQueue.isEmpty() && count < 50) {
                int[] data = debugQueue.poll();
                if (data != null) {
                    flatData.add(data[0]); // x
                    flatData.add(data[1]); // y
                    flatData.add(data[2]); // z
                    flatData.add(data[3]); // ステータス状態
                    count++;
                }
            }
            if (!flatData.isEmpty()) {
                PacketHandler.sendChunkDebug(world.getPlayers(), flatData);
            }
        }
        // スキップされたチャンクを再検査し、必要に応じてベーキング処理を実行
        recheckSkippedChunks(world);
    }

    // スキップされたチャンクセクションを追跡
    public static final Set<Long> skippedSections = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    private static void recheckSkippedChunks(ServerWorld world) {
        if (skippedSections.isEmpty())
            return;

        int restoredCount = 0;
        int maxRestoresPerTick = 20;

        // 同じチャンクに対する重複呼び出しを避けるため、このティックで復元されたチャンクを追跡
        Set<Long> restoredChunks = new HashSet<>();

        // ローカルコピーを取得して反復中の変更を防ぐ
        List<int[]> ranges = allowedRanges;

        Iterator<Long> it = skippedSections.iterator();
        while (it.hasNext()) {
            Long posLong = it.next();
            BlockPos pos = BlockPos.fromLong(posLong);
            int y = pos.getY() >> 4; // セクションY
            int sectionMinY = y * 16;
            int sectionMaxY = sectionMinY + 15;

            // 許可範囲内かどうかをチェック (いずれかの範囲に含まれればOK)
            boolean isAllowed = false;
            for (int[] range : ranges) {
                // セクションの下端が範囲の上端以下 かつ セクションの上端が範囲の下端以上
                if (sectionMinY <= range[1] && sectionMaxY >= range[0]) {
                    isAllowed = true;
                    break;
                }
            }

            if (isAllowed) {
                // チャンクがロードされているかチェック
                if (world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                    try {
                        int chunkX = pos.getX() >> 4;
                        int chunkZ = pos.getZ() >> 4;
                        long chunkPosLong = ChunkPos.toLong(chunkX, chunkZ);

                        if (restoredChunks.contains(chunkPosLong)) {
                            it.remove(); // 処理済みとしてスキップ済みセットから削除
                            continue;
                        }

                        // 強制的にチャンクを削除
                        if (world instanceof VSServerLevel) {
                            // VS2のキャッシュをフラッシュするために、地形を明示的に削除
                            Chunk chunk = world.getChunk(chunkX, chunkZ);
                            List<TerrainUpdate> updates = new ArrayList<>();

                            // すべてのセクションを反復処理し、削除更新を送信
                            int bottomSection = world.getBottomSectionCoord();

                            // チャンクがロードされている場合にのみ処理
                            if (chunk != null) {
                                for (int i = 0; i < chunk.getSectionArray().length; i++) {
                                    int sectionIndexY = bottomSection + i;

                                    // Delete更新を送信
                                    updates.add(ValkyrienSkiesMod.getVsCore()
                                            .newDeleteTerrainUpdate(chunkX, sectionIndexY, chunkZ));
                                }

                                // Kotlin/ClientWorldの問題を回避するためにインターフェースキャスト経由で削除更新を送信
                                if (world instanceof IShipObjectWorldServerProvider
                                        && world instanceof DimensionIdProvider) {
                                    ((IShipObjectWorldServerProvider) world).getShipObjectWorld().addTerrainUpdates(
                                            ((DimensionIdProvider) world).getDimensionId(),
                                            updates);
                                }

                                // セクションバージョン更新のためブロックを一時変更
                                // スキップされたセクション内のブロックを特定し更新通知を発行
                                for (int sY = 0; sY < chunk.getSectionArray().length; sY++) {
                                    ChunkSection section = chunk.getSectionArray()[sY];
                                    if (section == null || section.isEmpty())
                                        continue;

                                    int realSectionY = bottomSection + sY;

                                    // セクション内の固体ブロックを探索
                                    boolean dirtied = false;
                                    for (int lx = 0; lx < 16; lx++) {
                                        for (int lz = 0; lz < 16; lz++) {
                                            for (int ly = 0; ly < 16; ly++) {
                                                BlockState state = section.getBlockState(lx, ly,
                                                        lz);
                                                if (!state.isAir()) {
                                                    int bx = (chunkX << 4) + lx;
                                                    int by = (realSectionY << 4) + ly;
                                                    int bz = (chunkZ << 4) + lz;
                                                    BlockPos blockPos = new BlockPos(
                                                            bx, by, bz);

                                                    // ブロック更新イベントを発生（フラグ4: 再描画・近隣通知なし、変更のみ記録）
                                                    // 一瞬空気ブロックに置換して即時復元
                                                    world.setBlockState(blockPos,
                                                            Blocks.AIR.getDefaultState(), 4);
                                                    world.setBlockState(blockPos, state, 4);
                                                    dirtied = true;
                                                    break;
                                                }
                                            }
                                            if (dirtied)
                                                break;
                                        }
                                        if (dirtied)
                                            break;
                                    }
                                }
                            }

                            // リロードをトリガー
                            ((VSServerLevel) world).removeChunk(chunkX, chunkZ);

                            restoredChunks.add(chunkPosLong);
                            it.remove();
                            restoredCount++;
                            if (restoredCount >= maxRestoresPerTick) {
                                break;
                            }
                        } else {
                            it.remove();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        it.remove();
                    }
                }
            }
        }
    }

    private static void runMemoryPoll(ServerWorld world) {
        if (world.getPlayers().isEmpty())
            return;

        long max = Runtime.getRuntime().maxMemory();
        long totalMem = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();

        // 船に基づいて新しい制限を計算
        calculateDynamicLimits(world);

        // メトリクスを取得
        int skipped = skippedChunks;
        int total = totalChunks;

        // 次の間隔のためにメトリクスをリセット
        skippedChunks = 0;
        totalChunks = 0;

        PacketHandler.sendToAll(world.getPlayers(), totalMem, free, max, skipped, total, allowedMinY, allowedMaxY);
    }

    private static void calculateDynamicLimits(ServerWorld world) {
        List<int[]> newRanges = new ArrayList<>();
        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
        boolean foundTargets = false;

        // サーバー内の全プレイヤーを取得
        List<ServerPlayerEntity> allPlayers = world.getServer().getPlayerManager()
                .getPlayerList();

        for (ServerPlayerEntity player : allPlayers) {
            foundTargets = true;

            Ship ship = VSGameUtilsKt
                    .getShipManagingPos(player.getServerWorld(), player.getBlockPos());
            double targetY;

            if (ship != null) {
                targetY = ship.getTransform().getPositionInWorld().y();
            } else {
                targetY = player.getY();
            }

            // プレイヤーおよび船ごとに独立した計算範囲を作成
            int buffer = SkyJourneyConfig.bakingYBuffer;
            int rangeMin = (int) targetY - buffer;
            int rangeMax = (int) targetY + buffer;
            newRanges.add(new int[] { rangeMin, rangeMax });

            if (rangeMin < globalMin)
                globalMin = rangeMin;
            if (rangeMax > globalMax)
                globalMax = rangeMax;
        }

        if (!foundTargets) {
            allowedMinY = 10000;
            allowedMaxY = -10000;
            allowedRanges = Collections.emptyList();
        } else {
            // パケット互換性維持のためグローバル最小/最大値を更新
            allowedMinY = globalMin;
            allowedMaxY = globalMax;

            // 範囲リストを更新（不変リストとして保存）
            allowedRanges = Collections.unmodifiableList(newRanges);
        }
    }
}
