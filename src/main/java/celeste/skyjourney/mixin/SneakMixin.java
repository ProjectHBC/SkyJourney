package celeste.skyjourney.mixin;

import celeste.skyjourney.feature.FeatureManager;
import celeste.skyjourney.feature.sneak.SneakGroundFix;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class SneakMixin {

    /**
     * Entity.move メソッドの先頭で、移動ベクトル (movement) をインターセプトして補正する。
     */
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3d onMoveHead(Vec3d movement) {
        if (!FeatureManager.isSneakFixEnabled())
            return movement;

        Entity self = (Entity) (Object) this;

        if (self.isSneaking() && SneakGroundFix.isOnShipSurface(self)) {
            return SneakGroundFix.adjustMovement(self, movement);
        }

        return movement;
    }
}