package celeste.skyjourney.common;

import net.minecraft.entity.Entity;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class VSHelper {

    /**
     * エンティティが現在 Ship の管理下（上に乗っている、または内部にいる）か判定
     */
    public static boolean isOnShip(Entity entity) {
        if (entity.getWorld() == null)
            return false;
        return getShipManaging(entity) != null;
    }

    /**
     * エンティティの現在位置を管理する Ship を取得
     *
     * @return 管理するShip、存在しない場合はnull
     */
    public static Ship getShipManaging(Entity entity) {
        return VSGameUtilsKt.getShipManagingPos(entity.getWorld(), entity.getBlockPos());
    }

    /**
     * 管理Shipが見つからない場合、エンティティと交差しているShipを探して返す。
     * プレイヤーが Overworld 座標系にいながら、Ship Entity の上に乗っている場合などの
     * 管理外状態での検出に使用
     *
     * @return 交差しているShip、見つからない場合はnull
     */
    public static Ship getShipManagingOrIntersecting(Entity entity) {
        Ship ship = getShipManaging(entity);
        if (ship != null)
            return ship;

        // プレイヤーの足元付近のバウンディングボックスで検索
        net.minecraft.util.math.Box searchBox = entity.getBoundingBox().expand(0.5, 1.0, 0.5);

        // 最初に見つかったShipを返す
        Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(entity.getWorld(), searchBox);
        for (Ship s : ships) {
            return s;
        }
        return null;
    }

}