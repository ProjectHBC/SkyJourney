package celeste.skyjourney.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.Ship;

import celeste.skyjourney.common.VSHelper;
import celeste.skyjourney.config.SkyJourneyConfig;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin {

    @Inject(method = "onBlockHit", at = @At("HEAD"))
    private void removeProjectilesOnShip(BlockHitResult blockHitResult,CallbackInfo ci) {
        if (!SkyJourneyConfig.getInstance().enablePPEOnShipFix) { return; }

        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        if (projectile.getWorld().isClient()) { return; } // クライアントでは何もしない

        // 矢、スペクタルアロー、トライデント以外なら何もしない
        EntityType<?> type = projectile.getType();
        if (type != EntityType.ARROW && type != EntityType.SPECTRAL_ARROW && type != EntityType.TRIDENT) { return; }

        // 船の領域内にいなければ何もしない
        Ship ship = VSHelper.getShipManagingOrIntersecting(projectile);
        if (ship == null) { return; }

        // 横方向のベクトルが指定範囲内であるか判定する
        Vec3d velocity = projectile.getVelocity(); // 現在の速度を取得
        boolean isXStopped = Math.abs(velocity.x) < 0.05;
        boolean isZStopped = Math.abs(velocity.z) < 0.05;

        // 条件を満たしたら削除する
        if (isXStopped && isZStopped) {
            projectile.discard();
        }
    }
}
