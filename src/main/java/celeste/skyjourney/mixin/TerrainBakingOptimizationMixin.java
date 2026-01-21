package celeste.skyjourney.mixin;

import celeste.skyjourney.common.IDimensionAware;
import celeste.skyjourney.common.TerrainOptimizationManager;
import celeste.skyjourney.config.SkyJourneyConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import org.joml.Vector3ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.apigame.world.chunks.TerrainUpdate;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(targets = "org.valkyrienskies.mod.common.VSGameUtilsKt")
public class TerrainBakingOptimizationMixin {

    @Inject(method = "toDenseVoxelUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onToDenseVoxelUpdate(ChunkSection section, Vector3ic chunkPos,
            CallbackInfoReturnable<TerrainUpdate> cir) {

        if (!SkyJourneyConfig.enableTerrainBakingOptimization) {
            return;
        }

        // 船のチャンクを無視するチェック
        if (Math.abs(chunkPos.x()) >= 1250000 || Math.abs(chunkPos.z()) >= 1250000) {
            return;
        }

        TerrainOptimizationManager.totalChunks++; // スキップ判定前の総数をカウント

        // ディメンションIDを取得
        String dimId = null;
        if (section instanceof IDimensionAware) {
            dimId = ((IDimensionAware) section).skyjourney$getDimensionId();
        }

        // ディメンションIDが不明な場合
        if (dimId == null) {
            TerrainOptimizationManager.debugQueue
                    .add(new int[] { chunkPos.x(), chunkPos.y(), chunkPos.z(), 1 });
            return;
        }

        int sectionY = chunkPos.y();
        int sectionMinY = sectionY * 16;
        int sectionMaxY = sectionMinY + 15;

        // ディメンションごとの許可範囲リストを取得
        List<int[]> ranges = TerrainOptimizationManager.allowedRangesMap.get(dimId);
        boolean isAllowed = false;

        if (ranges == null) {
            // 範囲データ未生成の場合は許可（安全策）
            isAllowed = true;
        } else if (ranges.isEmpty()) {
            // 範囲リストが空（プレイヤー不在）の場合はスキップ
            isAllowed = false;
        } else {
            // 許可範囲に含まれているかチェック
            for (int[] range : ranges) {
                if (sectionMinY <= range[1] && sectionMaxY >= range[0]) {
                    isAllowed = true;
                    break;
                }
            }
        }

        if (!isAllowed) {
            // 範囲外のため処理をスキップ
            TerrainOptimizationManager.skippedChunks++;
            // デバッグ表示：スキップ（赤）
            TerrainOptimizationManager.debugQueue
                    .add(new int[] { chunkPos.x(), chunkPos.y(), chunkPos.z(), 0 });

            // 復元処理用にスキップ済み座標を記録
            long posLong = BlockPos.asLong(chunkPos.x() * 16, chunkPos.y() * 16, chunkPos.z() * 16);

            TerrainOptimizationManager.skippedSectionsMap
                    .computeIfAbsent(dimId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(posLong);

            // 空の更新データを返して処理をキャンセル
            TerrainUpdate empty = ValkyrienSkiesMod.getVsCore()
                    .newEmptyVoxelShapeUpdate(chunkPos.x(), chunkPos.y(), chunkPos.z(), true);
            cir.setReturnValue(empty);
        } else {
            // 1 = 処理対象（緑）
            TerrainOptimizationManager.debugQueue
                    .add(new int[] { chunkPos.x(), chunkPos.y(), chunkPos.z(), 1 });
        }
    }
}
