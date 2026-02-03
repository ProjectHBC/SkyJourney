package celeste.skyjourney.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.eureka.block.BalloonBlock;

import celeste.skyjourney.config.SkyJourneyConfig;

@Mixin(BalloonBlock.class)
public class BalloonBlockMixin {
    @Inject(method = "onProjectileHit", at = @At("HEAD"), cancellable = true)
    private void preventBalloonPop(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        if (!SkyJourneyConfig.getInstance().enableBalloonPPEFix) { return; }
        
        // PersistentProjectileEntity系(矢やトライデントなど)が当たった場合は割れないようにする
        if (projectile instanceof PersistentProjectileEntity) {
            ci.cancel();
        }
    }
}