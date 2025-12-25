package celeste.skyjourney.feature.sneak;

import celeste.skyjourney.common.VSHelper;
import celeste.skyjourney.mixin.LivingEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;

/**
 * 船上でのスニーク（しゃがみ移動）時の落下防止補正を行うクラス。
 * <p>
 * BlockState (World座標) ではなく、World.getCollisions (物理形状) を使用して、
 * 船の傾きや回転に関わらず、ローカル座標系での安全な移動を保証します。
 */
public class SneakGroundFix {

    /**
     * スニーク中の移動ベクトルを補正し、落下する場合は移動を制限
     *
     * @param entity   対象エンティティ
     * @param movement 元の移動ベクトル
     * @return 補正後の移動ベクトル
     */
    public static Vec3d adjustMovement(Entity entity, Vec3d movement) {
        Ship ship = VSHelper.getShipManagingOrIntersecting(entity);
        if (ship == null) {
            return movement;
        }

        // ジャンプ中であれば補正を行わず、移動（落下含む）を許可
        if (entity instanceof LivingEntity && ((LivingEntityAccessor) entity).isJumping()) {
            return movement;
        }

        if (movement.x == 0 && movement.z == 0) {
            return movement;
        }

        // ローカル座標系への変換
        Vector3d localMove = new Vector3d(movement.x, movement.y, movement.z);
        ship.getTransform().getWorldToShip().transformDirection(localMove);

        Vector3d localPos = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        ship.getTransform().getWorldToShip().transformPosition(localPos);

        World world = entity.getWorld();

        // 現在位置が既に空中（足場がない）であれば、補正を行わずに移動を許可
        if (!hasSupport(world, ship, localPos.x, localPos.y, localPos.z, entity)) {
            return movement;
        }

        // X方向の移動安全確認
        double nextLocalX = localPos.x + localMove.x;
        boolean safeX = hasSupport(world, ship, nextLocalX, localPos.y, localPos.z, entity);
        if (!safeX) {
            localMove.x = 0;
        }

        // Z方向の移動安全確認
        double nextLocalZ = localPos.z + localMove.z;
        boolean safeZ = hasSupport(world, ship, localPos.x, localPos.y, nextLocalZ, entity);
        if (!safeZ) {
            localMove.z = 0;
        }

        // 斜め移動の安全確認
        if (localMove.x != 0 && localMove.z != 0) {
            double diagX = localPos.x + localMove.x;
            double diagZ = localPos.z + localMove.z;
            boolean safeDiag = hasSupport(world, ship, diagX, localPos.y, diagZ, entity);
            if (!safeDiag) {
                localMove.x = 0;
                localMove.z = 0;
            }
        }

        // ワールド座標系へ戻す
        ship.getTransform().getShipToWorld().transformDirection(localMove);
        return new Vec3d(localMove.x, localMove.y, localMove.z);
    }

    /**
     * 指定されたローカル座標の足元に、エンティティを支えるコリジョンが存在するか判定
     *
     * @param localX ローカルX座標
     * @param localY ローカルY座標 (エンティティの足元)
     * @param localZ ローカルZ座標
     * @return 足場が存在する場合はtrue
     */
    private static boolean hasSupport(World world, Ship ship, double localX, double localY, double localZ,
                                      Entity entity) {
        // 足元領域の定義
        double boxSizeXZ = 0.05;
        double lowerY = localY - 0.75;
        double upperY = localY - 0.01;

        Box checkArea = new Box(
                localX - boxSizeXZ, lowerY, localZ - boxSizeXZ,
                localX + boxSizeXZ, upperY, localZ + boxSizeXZ);

        // World.getCollisions は配置されたShipチャンクに対して判定を行えるため、座標変換なしでチェック
        Iterable<net.minecraft.util.shape.VoxelShape> collisions = world.getCollisions(entity, checkArea);

        for (net.minecraft.util.shape.VoxelShape shape : collisions) {
            if (!shape.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * エンティティが船の表面（上または内部）にいるか判定
     */
    public static boolean isOnShipSurface(Entity entity) {
        Ship ship = VSHelper.getShipManagingOrIntersecting(entity);
        return ship != null;
    }
}
