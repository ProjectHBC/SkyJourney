package celeste.skyjourney.feature.sneak;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;

import celeste.skyjourney.common.VSHelper;
import celeste.skyjourney.mixin.LivingEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * 船上でのスニーク（しゃがみ移動）時の落下防止補正を行うクラス。
 * <p>
 * BlockState (World座標) ではなく、World.getCollisions (物理形状) を使用して、
 * 船の傾きや回転に関わらず、ローカル座標系での安全な移動を保証します。
 */
public class SneakGroundFix {

    // プレイヤーの中心からどこまでを「アンカー（十字の先端）」とするか
    private static final double OUTER = 0.28;
    private static final double INNER = 0.20;

    // 独立した隙間のない中央の判定ボックス
    private static final double EDGE_THICKNESS = OUTER - INNER; // 0.08
    private static final double CENTER = INNER - EDGE_THICKNESS; // 0.12

    public static Vec3d adjustMovement(Entity entity, Vec3d movement) {
        // 船の上でなければ返す
        Ship ship = VSHelper.getShipManagingOrIntersecting(entity);
        if (ship == null) { return movement; }

        // ジャンプ中であれば補正を行わず、移動を許可
        if (movement.y > 0.0 || (entity instanceof LivingEntity && ((LivingEntityAccessor) entity).isJumping())) { return movement; }
        if (movement.x == 0 && movement.z == 0) { return movement; }

        // ローカル座標系への変換
        Vector3d localMove = new Vector3d(movement.x, movement.y, movement.z);
        ship.getTransform().getWorldToShip().transformDirection(localMove);

        Vector3d localPos = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        ship.getTransform().getWorldToShip().transformPosition(localPos);

        World world = entity.getWorld();

        // 現在位置が既に空中（足場がない）であれば、補正を行わずに移動を許可
        if (isAir(world, localPos.x, localPos.y, localPos.z, -OUTER, OUTER, -OUTER, OUTER, entity, ship)) { return movement; }

        // 共通項目のX,Z方向それぞれの予測した値
        double nextLocalX = localPos.x + localMove.x;
        double nextLocalZ = localPos.z + localMove.z;

        // X方向の移動安全確認
        if (localMove.x != 0) {
            boolean centerAir = isAir(world, nextLocalX, localPos.y, localPos.z, -CENTER, CENTER, -CENTER, CENTER, entity, ship);
            
            if (centerAir) {
                boolean air_pZ = isAir(world, nextLocalX, localPos.y, localPos.z, -INNER, INNER, INNER, OUTER, entity, ship);   // 下
                boolean air_nZ = isAir(world, nextLocalX, localPos.y, localPos.z, -INNER, INNER, -OUTER, -INNER, entity, ship); // 上
                if (air_nZ && air_pZ) {
                    localMove.x = 0;
                }
            }
        }

        // Z方向の移動安全確認
        if (localMove.z != 0) {
            boolean centerAir = isAir(world, localPos.x, localPos.y, nextLocalZ, -CENTER, CENTER, -CENTER, CENTER, entity, ship);
            
            if (centerAir) {
                boolean air_pX = isAir(world, localPos.x, localPos.y, nextLocalZ, INNER, OUTER, -INNER, INNER, entity, ship);   // 右
                boolean air_nX = isAir(world, localPos.x, localPos.y, nextLocalZ, -OUTER, -INNER, -INNER, INNER, entity, ship); // 左
                
                if (air_nX && air_pX) {
                    localMove.z = 0;
                }
            }
        }

        // 斜め移動の安全確認
        if (localMove.x != 0 && localMove.z != 0) {
            boolean centerAir = isAir(world, nextLocalX, localPos.y, nextLocalZ, -CENTER, CENTER, -CENTER, CENTER, entity, ship);

            if (centerAir) {
                boolean air_pX = isAir(world, nextLocalX, localPos.y, nextLocalZ, INNER, OUTER, -INNER, INNER, entity, ship);   // 右
                boolean air_nX = isAir(world, nextLocalX, localPos.y, nextLocalZ, -OUTER, -INNER, -INNER, INNER, entity, ship); // 左
                boolean air_pZ = isAir(world, nextLocalX, localPos.y, nextLocalZ, -INNER, INNER, INNER, OUTER, entity, ship);   // 下
                boolean air_nZ = isAir(world, nextLocalX, localPos.y, nextLocalZ, -INNER, INNER, -OUTER, -INNER, entity, ship); // 上

                if (localMove.x < 0 && localMove.z < 0 && (air_pX && air_pZ)) {
                    localMove.x = 0;
                    localMove.z = 0;
                }
                else if (localMove.x < 0 && localMove.z > 0 && (air_pX && air_nZ)) {
                    localMove.x = 0;
                    localMove.z = 0;
                }
                else if (localMove.x > 0 && localMove.z < 0 && (air_nX && air_pZ)) {
                    localMove.x = 0;
                    localMove.z = 0;
                }
                else if (localMove.x > 0 && localMove.z > 0 && (air_nX && air_nZ)) {
                    localMove.x = 0;
                    localMove.z = 0;
                }
            }
        }

        // ワールド座標系へ戻す
        ship.getTransform().getShipToWorld().transformDirection(localMove);
        return new Vec3d(localMove.x, localMove.y, localMove.z);
    }

    /**
     * 指定された領域がブロックに触れていないAirであるかどうかを判定する
     */
    private static boolean isAir(World world, double localX, double localY, double localZ, double minXOffset, double maxXOffset, double minZOffset, double maxZOffset, Entity entity, Ship ship) {
        // 足元領域の定義
        double lowerY = localY - 3.0;
        double upperY = localY - 0.01;

        Box checkArea = new Box(
                localX + minXOffset, lowerY, localZ + minZOffset,
                localX + maxXOffset, upperY, localZ + maxZOffset);

        Iterable<VoxelShape> collisions = world.getCollisions(entity, checkArea);
        boolean hitBlock = false;

        for (VoxelShape shape : collisions) {
            if (!shape.isEmpty()) {
                double shapeMaxY = shape.getMax(net.minecraft.util.math.Direction.Axis.Y);
                if (shapeMaxY <= localY + 0.65 && localY - shapeMaxY <= 0.75) {
                    hitBlock = true;
                    break;
                }
            }
        }

        // デバッグ用のレンダラー
        if (world.isClient() && ship != null) {
            SneakDebugData.addBox(checkArea, ship.getTransform().getShipToWorld(), hitBlock);
        }

        return !hitBlock;
    }

    /**
     * エンティティが船の表面（上または内部）にいるか判定
     */
    public static boolean isOnShipSurface(Entity entity) {
        Ship ship = VSHelper.getShipManagingOrIntersecting(entity);
        return ship != null;
    }
}
