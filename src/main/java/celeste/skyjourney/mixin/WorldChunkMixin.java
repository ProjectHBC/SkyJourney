package celeste.skyjourney.mixin;

import celeste.skyjourney.common.IDimensionAware;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/UpgradeData;Lnet/minecraft/world/tick/ChunkTickScheduler;Lnet/minecraft/world/tick/ChunkTickScheduler;J[Lnet/minecraft/world/chunk/ChunkSection;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;Lnet/minecraft/world/gen/chunk/BlendingData;)V", at = @At("RETURN"))
    private void onInit(World world, net.minecraft.util.math.ChunkPos pos,
            net.minecraft.world.chunk.UpgradeData upgradeData,
            net.minecraft.world.tick.ChunkTickScheduler blockTickScheduler,
            net.minecraft.world.tick.ChunkTickScheduler fluidTickScheduler,
            long inhabitedTime, ChunkSection[] sections, net.minecraft.world.chunk.WorldChunk.EntityLoader entityLoader,
            net.minecraft.world.gen.chunk.BlendingData blendingData, CallbackInfo ci) {

        // サーバーワールドの場合のみマッピングを登録
        if (world instanceof ServerWorld) {
            String dimId = world.getRegistryKey().getValue().toString();
            // コンストラクタ引数ではなく、実際に保持されているセクション配列を取得して登録
            ChunkSection[] storedSections = ((WorldChunk) (Object) this).getSectionArray();
            for (ChunkSection section : storedSections) {
                if (section != null) {
                    // Duck TypingでフィールドにディメンションIDをセット
                    ((IDimensionAware) section).skyjourney$setDimensionId(dimId);
                }
            }
        }
    }
}
