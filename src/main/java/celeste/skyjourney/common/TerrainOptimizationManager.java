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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TerrainOptimizationManager {

    // パケット互換用グローバル値
    public static int allowedMinY = -2048;
    public static int allowedMaxY = 2048;

    // ディメンションID -> 許可範囲リストのマッピング
    public static final Map<String, List<int[]>> allowedRangesMap = new ConcurrentHashMap<>();

    // メトリクス
    public static int skippedChunks = 0;
    public static int totalChunks = 0;

    // デバッグキュー
    public static final ConcurrentLinkedQueue<int[]> debugQueue = new ConcurrentLinkedQueue<>();

    // ディメンションID -> スキップされたセクション座標セットのマッピング
    public static final Map<String, Set<Long>> skippedSectionsMap = new ConcurrentHashMap<>();

    // チャンクロード時にディメンションIDを紐付けるイベントリスナー
    public static void onChunkLoad(ServerWorld world, Chunk chunk) {
        if (world == null || chunk == null)
            return;

        String dimId = world.getRegistryKey().getValue().toString();
        ChunkSection[] sections = chunk.getSectionArray();

        for (ChunkSection section : sections) {
            if (section != null && section instanceof IDimensionAware) {
                ((IDimensionAware) section).skyjourney$setDimensionId(dimId);
            }
        }
    }

    public static void onWorldTick(ServerWorld world) {
        if (world.isClient)
            return;

        // メモリ監視と範囲計算の実行
        if (world.getTime() % SkyJourneyConfig.getInstance().memoryPollInterval == 0) {
            runMemoryPoll(world);
        } else {
            // プレイヤー移動への追従性を確保するため、範囲計算は毎ティック行う
            calculateDynamicLimits(world);
        }

        // デバッグキューを処理してクライアントへ送信
        processDebugQueue(world);

        // 現在のワールドに対するスキップ済みチャンクの再チェック（復元処理）
        recheckSkippedChunks(world);
    }

    private static void processDebugQueue(ServerWorld world) {
        if (debugQueue.isEmpty())
            return;

        List<Integer> flatData = new ArrayList<>();
        int count = 0;
        // パケットサイズ制限
        while (!debugQueue.isEmpty() && count < 50) {
            int[] data = debugQueue.poll();
            if (data != null) {
                flatData.add(data[0]); // x
                flatData.add(data[1]); // y
                flatData.add(data[2]); // z
                flatData.add(data[3]); // ステータス
                count++;
            }
        }
        if (!flatData.isEmpty()) {
            PacketHandler.sendChunkDebug(world.getPlayers(), flatData);
        }
    }

    private static void recheckSkippedChunks(ServerWorld world) {
        String currentDimId = world.getRegistryKey().getValue().toString();
        Set<Long> skippedInThisDim = skippedSectionsMap.get(currentDimId);

        if (skippedInThisDim == null || skippedInThisDim.isEmpty())
            return;

        // ワールドごとの処理用キューを取得
        Queue<Long> localQueue = getOrCreateQueue(currentDimId);

        // キューが空なら補充
        if (localQueue.isEmpty()) {
            localQueue.addAll(skippedInThisDim);
        }

        if (localQueue.isEmpty())
            return;

        int processedCount = 0;
        int restoredCount = 0;
        int maxChecksPerTick = 1000; // 1ティックあたりの確認上限
        int maxRestoresPerTick = 5; // 1ティックあたりの復元上限

        Set<Long> restoredChunks = new HashSet<>();
        List<int[]> ranges = allowedRangesMap.getOrDefault(currentDimId, Collections.emptyList());
        boolean hasRanges = !ranges.isEmpty();

        while (!localQueue.isEmpty() && processedCount < maxChecksPerTick) {
            Long posLong = localQueue.poll();

            // 既にセットから削除済み（復元済み等）ならスキップ
            if (!skippedInThisDim.contains(posLong))
                continue;
            processedCount++;

            BlockPos pos = BlockPos.fromLong(posLong);
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;

            // チャンクがアンロードされている場合は管理リストから除外して終了
            if (!world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                skippedInThisDim.remove(posLong);
                continue;
            }

            int sectionMinY = (pos.getY() >> 4) * 16;
            int sectionMaxY = sectionMinY + 15;

            boolean isAllowed = false;
            // 現在の許可範囲内に入ったかチェック
            if (hasRanges) {
                for (int[] range : ranges) {
                    if (sectionMinY <= range[1] && sectionMaxY >= range[0]) {
                        isAllowed = true;
                        break;
                    }
                }
            }

            // 範囲内に入った場合、チャンクを復元（再生成）
            if (isAllowed) {
                try {
                    restoreChunk(world, chunkX, chunkZ, posLong, skippedInThisDim, restoredChunks);
                    if (restoredChunks.contains(ChunkPos.toLong(chunkX, chunkZ))) {
                        restoredCount++;
                    }
                    if (restoredCount >= maxRestoresPerTick)
                        break;
                } catch (Exception e) {
                    e.printStackTrace();
                    skippedInThisDim.remove(posLong);
                }
            }
        }
    }

    // ワールドごとの再チェックキュー管理
    private static final Map<String, Queue<Long>> worldQueues = new ConcurrentHashMap<>();

    private static Queue<Long> getOrCreateQueue(String dimId) {
        return worldQueues.computeIfAbsent(dimId, k -> new ArrayDeque<>());
    }

    private static void restoreChunk(ServerWorld world, int chunkX, int chunkZ, long posLong, Set<Long> skippedSet,
            Set<Long> restoredChunks) {
        long chunkPosLong = ChunkPos.toLong(chunkX, chunkZ);

        // 同じチャンク内で既にセクション復元が行われている場合、重複処理を避けてセットから削除のみ行う
        if (restoredChunks.contains(chunkPosLong)) {
            skippedSet.remove(posLong);
            return;
        }

        // VSワールドの場合、明示的に地形更新パケットを送信して再同期
        if (world instanceof VSServerLevel) {
            Chunk chunk = world.getChunk(chunkX, chunkZ);
            List<TerrainUpdate> updates = new ArrayList<>();
            int bottomSection = world.getBottomSectionCoord();

            if (chunk != null) {
                // 全セクション削除の更新を作成
                for (int i = 0; i < chunk.getSectionArray().length; i++) {
                    int sectionIndexY = bottomSection + i;
                    updates.add(ValkyrienSkiesMod.getVsCore().newDeleteTerrainUpdate(chunkX, sectionIndexY, chunkZ));
                }

                // ShipObjectWorldに更新を適用
                if (world instanceof IShipObjectWorldServerProvider && world instanceof DimensionIdProvider) {
                    ((IShipObjectWorldServerProvider) world).getShipObjectWorld().addTerrainUpdates(
                            ((DimensionIdProvider) world).getDimensionId(), updates);
                }

                // ブロック状態のリフレッシュ（再送用）
                for (int sY = 0; sY < chunk.getSectionArray().length; sY++) {
                    ChunkSection section = chunk.getSectionArray()[sY];
                    if (section == null || section.isEmpty())
                        continue;

                    int realSectionY = bottomSection + sY;
                    boolean dirtied = false;

                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int ly = 0; ly < 16; ly++) {
                                BlockState state = section.getBlockState(lx, ly, lz);
                                if (!state.isAir()) {
                                    // 一度AIRにしてから戻すことで更新をトリガーする
                                    int bx = (chunkX << 4) + lx;
                                    int by = (realSectionY << 4) + ly;
                                    int bz = (chunkZ << 4) + lz;
                                    BlockPos blockPos = new BlockPos(bx, by, bz);
                                    world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 4);
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
            // VS側のチャンク管理から削除して再読み込みを促す
            ((VSServerLevel) world).removeChunk(chunkX, chunkZ);
            restoredChunks.add(chunkPosLong);
            skippedSet.remove(posLong);
        } else {
            // 通常ワールドの場合はリストから削除のみ
            skippedSet.remove(posLong);
        }
    }

    private static void runMemoryPoll(ServerWorld world) {
        if (world.getPlayers().isEmpty())
            return;

        calculateDynamicLimits(world);

        long max = Runtime.getRuntime().maxMemory();
        long totalMem = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();

        PacketHandler.sendToAll(world.getPlayers(), totalMem, free, max, skippedChunks, totalChunks, allowedMinY,
                allowedMaxY);

        // 統計リセット
        skippedChunks = 0;
        totalChunks = 0;
    }

    private static void calculateDynamicLimits(ServerWorld world) {
        String dimId = world.getRegistryKey().getValue().toString();
        List<int[]> newRanges = new ArrayList<>();
        int globalMin = Integer.MAX_VALUE;
        int globalMax = Integer.MIN_VALUE;
        boolean foundTargets = false;

        List<ServerPlayerEntity> allPlayers = world.getPlayers();

        for (ServerPlayerEntity player : allPlayers) {
            foundTargets = true;

            // プレイヤーが船に乗っている場合はその船の高さを基準にする
            Ship ship = VSGameUtilsKt.getShipManagingPos(world, player.getBlockPos());
            double targetY;

            if (ship != null) {
                targetY = ship.getTransform().getPositionInWorld().y();
            } else {
                targetY = player.getY();
            }

            int buffer = SkyJourneyConfig.getInstance().bakingYBuffer;
            int rangeMin = (int) targetY - buffer;
            int rangeMax = (int) targetY + buffer;
            newRanges.add(new int[] { rangeMin, rangeMax });

            if (rangeMin < globalMin)
                globalMin = rangeMin;
            if (rangeMax > globalMax)
                globalMax = rangeMax;
        }

        if (!foundTargets) {
            allowedRangesMap.put(dimId, Collections.emptyList());
        } else {
            allowedMinY = globalMin;
            allowedMaxY = globalMax;
            allowedRangesMap.put(dimId, Collections.unmodifiableList(newRanges));
        }
    }
}
