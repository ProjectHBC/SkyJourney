package celeste.skyjourney.mixin.client.drawer;

import io.github.mattidragon.extendeddrawers.client.renderer.AbstractDrawerBlockEntityRenderer;
import io.github.mattidragon.extendeddrawers.config.category.ClientCategory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import celeste.skyjourney.config.SkyJourneyConfig;

import org.valkyrienskies.core.api.ships.ClientShip;
import org.joml.Vector3d;

@Mixin(AbstractDrawerBlockEntityRenderer.class)
public class AbstractDrawerBlockEntityRendererMixin {

    // 視界判定の突破
    public boolean rendersOutsideBoundingBox(BlockEntity blockEntity) {
        if (!SkyJourneyConfig.enableDrawerFix) { return false; }
        if (blockEntity.hasWorld() && VSGameUtilsKt.getShipManagingPos(blockEntity.getWorld(), blockEntity.getPos()) != null) { return true; }
        return false;
    }

    // 遮蔽判定 (Occlusion Culling) の対策
    @Redirect(
        method = "shouldRender",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntity;getPos()Lnet/minecraft/util/math/BlockPos;")
    )
    private BlockPos onShouldRender(BlockEntity instance) {
        World world = instance.getWorld();
        BlockPos pos = instance.getPos();
        if (!SkyJourneyConfig.enableDrawerFix) { return pos; }
        if (world != null) {
            ClientShip ship = (ClientShip) VSGameUtilsKt.getShipManagingPos(world, pos);
            if (ship != null) {
                Vector3d jomlPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                ship.getShipToWorld().transformPosition(jomlPos);
                return new BlockPos((int) jomlPos.x, (int) jomlPos.y, (int) jomlPos.z);
            }
        }
        return pos;
    }

    // レンダリング距離の拡張
    @WrapOperation(
        method = "renderSlot",
        at = @At(value = "INVOKE", target = "Lio/github/mattidragon/extendeddrawers/config/category/ClientCategory;textRenderDistance()I")
    )
    private int wrapTextDistance(ClientCategory instance, Operation<Integer> original, @Local(argsOnly = true) World world, @Local(argsOnly = true) BlockPos pos) {
        if (!SkyJourneyConfig.enableDrawerFix) { return original.call(instance); }
        if (world != null && VSGameUtilsKt.getShipManagingPos(world, pos) != null) { return Integer.MAX_VALUE; }
        return original.call(instance);
    }

    @WrapOperation(
        method = "renderSlot",
        at = @At(value = "INVOKE", target = "Lio/github/mattidragon/extendeddrawers/config/category/ClientCategory;iconRenderDistance()I")
    )
    private int wrapIconDistance(ClientCategory instance, Operation<Integer> original, @Local(argsOnly = true) World world, @Local(argsOnly = true) BlockPos pos) {
        if (!SkyJourneyConfig.enableDrawerFix) { return original.call(instance); }
        if (world != null && VSGameUtilsKt.getShipManagingPos(world, pos) != null) { return Integer.MAX_VALUE; }
        return original.call(instance);
    }

    @WrapOperation(
        method = "renderSlot",
        at = @At(value = "INVOKE", target = "Lio/github/mattidragon/extendeddrawers/config/category/ClientCategory;itemRenderDistance()I")
    )
    private int wrapItemDistance(ClientCategory instance, Operation<Integer> original, @Local(argsOnly = true) World world, @Local(argsOnly = true) BlockPos pos) {
        if (!SkyJourneyConfig.enableDrawerFix) { return original.call(instance); }
        if (world != null && VSGameUtilsKt.getShipManagingPos(world, pos) != null) { return Integer.MAX_VALUE; }
        return original.call(instance);
    }
}