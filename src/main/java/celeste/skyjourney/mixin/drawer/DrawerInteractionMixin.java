package celeste.skyjourney.mixin.drawer;

import io.github.mattidragon.extendeddrawers.block.ShadowDrawerBlock;
import io.github.mattidragon.extendeddrawers.block.base.StorageDrawerBlock;
import io.github.mattidragon.extendeddrawers.misc.DrawerRaycastUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import celeste.skyjourney.config.SkyJourneyConfig;

@Mixin({StorageDrawerBlock.class, ShadowDrawerBlock.class})
public class DrawerInteractionMixin {
    @Redirect(
        method = "onBlockBreakStart",
        at = @At(
            value = "INVOKE",
            target = "Lio/github/mattidragon/extendeddrawers/misc/DrawerRaycastUtil;getTarget(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/hit/BlockHitResult;"
        )
    )
    private BlockHitResult fixRaycastCoordinates(PlayerEntity player, BlockPos pos) {
        if (!SkyJourneyConfig.getInstance().enableDrawerFix) { return DrawerRaycastUtil.getTarget(player, pos); }

        // 通常のレイキャストを行う
        HitResult hit = player.raycast(5.0d, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) { return BlockHitResult.createMissed(player.getEyePos(), Direction.UP, pos); }

        BlockHitResult blockHit = (BlockHitResult) hit;
        World world = player.getWorld();

        Ship ship = VSGameUtilsKt.getShipManagingPos(world, pos);
        if (ship != null) {
            // ワールド座標をローカル座標に変換
            Vec3d worldHitVec = blockHit.getPos();
            Vector3d jomlPos = new Vector3d(worldHitVec.x, worldHitVec.y, worldHitVec.z);
            ship.getWorldToShip().transformPosition(jomlPos);

            return new BlockHitResult(
                new Vec3d(jomlPos.x, jomlPos.y, jomlPos.z), // ローカル座標
                blockHit.getSide(),
                blockHit.getBlockPos(),
                blockHit.isInsideBlock()
            );
        }

        return blockHit;
    }
}