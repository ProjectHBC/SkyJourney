package celeste.skyjourney.mixin;

import celeste.skyjourney.config.SkyJourneyConfig;
import net.minecraft.world.chunk.ChunkSection;
import org.joml.Vector3ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.apigame.world.chunks.TerrainUpdate;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import java.util.List;

@Mixin(targets = "org.valkyrienskies.mod.common.VSGameUtilsKt")
public class TerrainBakingOptimizationMixin {

    @Inject(method = "toDenseVoxelUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onToDenseVoxelUpdate(ChunkSection section, Vector3ic chunkPos,
            CallbackInfoReturnable<TerrainUpdate> cir) {

        if (!SkyJourneyConfig.enableTerrainBakingOptimization) {
            return;
        }

        // 船のチャンクを無視するチェック
        if (Math.abs(chunkPos.x()) >= 30000000 || Math.abs(chunkPos.z()) >= 30000000) {
            return;
        }

        celeste.skyjourney.common.TerrainOptimizationManager.totalChunks++; // スキップ判定前の総数をカウント

        int sectionY = chunkPos.y();
        int sectionMinY = sectionY * 16;
        int sectionMaxY = sectionMinY + 15;

        // 許可範囲リストとの交差判定を実行
        boolean isAllowed = false;
        List<int[]> ranges = celeste.skyjourney.common.TerrainOptimizationManager.allowedRanges;

        // 範囲リスト空時は全スキップ（プレイヤー不在）
        if (ranges != null && !ranges.isEmpty()) {
            for (int[] range : ranges) {
                // セクションと範囲の交差を確認
                if (sectionMinY <= range[1] && sectionMaxY >= range[0]) {
                    isAllowed = true;
                    break;
                }
            }
        }

        // 範囲外のため処理をスキップ
        if (!isAllowed) {
            celeste.skyjourney.common.TerrainOptimizationManager.skippedChunks++;
            celeste.skyjourney.common.TerrainOptimizationManager.debugQueue
                    .add(new int[] { chunkPos.x(), chunkPos.y(), chunkPos.z(), 0 }); // 0 = スキップ（赤）

            // 復元処理用にスキップ済み座標を追跡リストへ追加
            // セクション原点のBlockPosを保存
            long posLong = net.minecraft.util.math.BlockPos.asLong(chunkPos.x() * 16, chunkPos.y() * 16,
                    chunkPos.z() * 16);
            celeste.skyjourney.common.TerrainOptimizationManager.skippedSections.add(posLong);

            TerrainUpdate empty = ValkyrienSkiesMod.getVsCore()
                    .newEmptyVoxelShapeUpdate(chunkPos.x(), chunkPos.y(), chunkPos.z(), true);
            cir.setReturnValue(empty);
        } else {
            // 1 = 処理対象（緑）
            celeste.skyjourney.common.TerrainOptimizationManager.debugQueue
                    .add(new int[] { chunkPos.x(), chunkPos.y(), chunkPos.z(), 1 });
        }
    }
}
