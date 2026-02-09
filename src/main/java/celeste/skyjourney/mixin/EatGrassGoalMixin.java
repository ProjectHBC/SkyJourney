package celeste.skyjourney.mixin;

import net.minecraft.entity.ai.goal.EatGrassGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Optional;

@Mixin(EatGrassGoal.class)
public abstract class EatGrassGoalMixin {

    @Shadow @Final private World world;

    //判定座標だけを船上に書き換え
    @Redirect(method = {"canStart", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;getBlockPos()Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos redirectGetBlockPos(MobEntity mob) {
        BlockPos originalPos = mob.getBlockPos();

        if (mob instanceof SheepEntity sheep && !sheep.isSheared()) {
            return originalPos;
        }

        return skyjourney$getShipPos(mob).orElse(originalPos);
    }

    //羊の足元の船上座標を取得
    @Unique
    private Optional<BlockPos> skyjourney$getShipPos(MobEntity mob) {
        Box box = mob.getBoundingBox();
        Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(this.world, box);

        for (Ship ship : ships) {
            Vector3d localPos = new Vector3d(mob.getX(), mob.getY(), mob.getZ());
            ship.getTransform().getWorldToShip().transformPosition(localPos);
            return Optional.of(BlockPos.ofFloored(localPos.x, localPos.y, localPos.z));
        }

        return Optional.empty();
    }
}
