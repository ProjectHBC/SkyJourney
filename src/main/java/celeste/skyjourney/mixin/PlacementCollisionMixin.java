package celeste.skyjourney.mixin;

import celeste.skyjourney.common.VSHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.Ship;

@Mixin(BlockItem.class)
public class PlacementCollisionMixin {

    /**
     * ブロック設置時のプレイヤー重複チェックに介入
     */
    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void onCanPlace(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (context.getPlayer() == null)
            return;

        PlayerEntity player = context.getPlayer();
        // 船の管理下、もしくは船と交差しているかを確認
        Ship ship = VSHelper.getShipManagingOrIntersecting(player);

        BlockPos placePos = context.getBlockPos();

        // ブロックの衝突判定形状
        VoxelShape shape = state.getCollisionShape(context.getWorld(), placePos);
        if (shape.isEmpty()) {
            return; // 衝突判定がないブロックは制限しない
        }

        // バウンディングボックスを取得
        Box blockBox = shape.getBoundingBox().offset(placePos);

        // プレイヤーのバウンディングボックス
        Box worldPlayerBox = player.getBoundingBox().contract(0.01);

        boolean intersects = false;

        if (ship != null) {
            Box checkBlockBox = blockBox;
            Box checkPlayerBox = worldPlayerBox;

            // プレイヤー座標が造船所座標系にある場合、ワールド座標系へ変換
            if (isShipyardCoord(worldPlayerBox)) {
                checkPlayerBox = transformBoxShipToWorld(worldPlayerBox, ship);
            }

            // ブロック座標が造船所座標系にある場合、ワールド座標系へ変換
            if (isShipyardCoord(blockBox)) {
                checkBlockBox = transformBoxShipToWorld(blockBox, ship);
            }

            // ワールド座標空間で交差判定
            if (checkPlayerBox.intersects(checkBlockBox)) {
                intersects = true;
            }
        }

        // 交差している場合は設置を拒否
        if (intersects) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 座標が造船所座標系にあるかを簡易判定
     */
    @Unique
    private boolean isShipyardCoord(Box box) {
        return Math.abs(box.minX) > 20000000 || Math.abs(box.minZ) > 20000000;
    }

    // Ship
    @Unique
    private Box transformBoxShipToWorld(Box shipBox, Ship ship) {
        return transformBoxGeneric(shipBox, ship);
    }

    /**
     * Boxの8頂点を変換し、新しい変換済みBoxを生成
     * Ship座標からWorld座標へ変換
     */
    @Unique
    private Box transformBoxGeneric(Box box, Ship ship) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        // 8頂点をループして変換
        for (double x : new double[] { box.minX, box.maxX }) {
            for (double y : new double[] { box.minY, box.maxY }) {
                for (double z : new double[] { box.minZ, box.maxZ }) {
                    Vector3d pos = new Vector3d(x, y, z);
                    ship.getTransform().getShipToWorld().transformPosition(pos);

                    if (pos.x < minX)
                        minX = pos.x;
                    if (pos.y < minY)
                        minY = pos.y;
                    if (pos.z < minZ)
                        minZ = pos.z;
                    if (pos.x > maxX)
                        maxX = pos.x;
                    if (pos.y > maxY)
                        maxY = pos.y;
                    if (pos.z > maxZ)
                        maxZ = pos.z;
                }
            }
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
