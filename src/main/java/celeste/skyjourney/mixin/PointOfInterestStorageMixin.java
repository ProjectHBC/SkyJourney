package celeste.skyjourney.mixin;

import celeste.skyjourney.common.GhostPOIManager;
import celeste.skyjourney.common.WorldAwareStorage;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.spongepowered.asm.mixin.Unique;

import java.lang.ref.WeakReference;
import java.util.Optional;

@Mixin(PointOfInterestStorage.class)
public abstract class PointOfInterestStorageMixin implements WorldAwareStorage {

    @Shadow
    public abstract Optional<RegistryEntry<PointOfInterestType>> getType(BlockPos pos);

    @Unique
    private WeakReference<ServerWorld> skyjourney$worldRef;

    @Unique
    private final ThreadLocal<Boolean> skyjourney$isRedirecting = ThreadLocal.withInitial(() -> false);

    @Override
    public void skyjourney$setWorld(ServerWorld world) {
        this.skyjourney$worldRef = new WeakReference<>(world);
    }

    @Override
    public ServerWorld skyjourney$getWorld() {
        return skyjourney$worldRef != null ? skyjourney$worldRef.get() : null;
    }

    @Inject(method = "getType", at = @At("HEAD"), cancellable = true)
    private void onGetType(BlockPos pos, CallbackInfoReturnable<Optional<RegistryEntry<PointOfInterestType>>> cir) {
        if (skyjourney$isRedirecting.get())
            return;

        if (!GhostPOIManager.isValid(pos))
            return;

        ServerWorld world = skyjourney$getWorld();
        if (world == null)
            return;

        if (!world.getServer().isOnThread())
            return;

        // 造船所チェックロジック
        try {
            net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(pos).expand(0.5);
            Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(world, box);
            for (Ship ship : ships) {
                // ワールド座標 -> 造船所座標
                Vector3d localPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                ship.getTransform().getWorldToShip().transformPosition(localPos);
                BlockPos shipyardPos = new BlockPos((int) localPos.x, (int) localPos.y, (int) localPos.z);

                // クエリを造船所座標へリダイレクト
                skyjourney$isRedirecting.set(true);
                try {
                    Optional<RegistryEntry<PointOfInterestType>> result = this.getType(shipyardPos);
                    if (result.isPresent()) {
                        cir.setReturnValue(result);
                    }
                } finally {
                    skyjourney$isRedirecting.set(false);
                }

                if (cir.isCancelled())
                    return;
            }
        } catch (Exception e) {
        }
    }

    @Inject(method = "hasTypeAt", at = @At("HEAD"), cancellable = true)
    private void onHasTypeAt(net.minecraft.registry.RegistryKey<PointOfInterestType> type, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (skyjourney$isRedirecting.get())
            return;
        if (!GhostPOIManager.isValid(pos))
            return;

        ServerWorld world = skyjourney$getWorld();
        if (world == null)
            return;

        if (!world.getServer().isOnThread())
            return;

        try {
            net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(pos).expand(0.5);
            Iterable<Ship> ships = VSGameUtilsKt.getShipsIntersecting(world, box);

            for (Ship ship : ships) {
                Vector3d localPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                ship.getTransform().getWorldToShip().transformPosition(localPos);
                BlockPos shipyardPos = new BlockPos((int) localPos.x, (int) localPos.y, (int) localPos.z);

                skyjourney$isRedirecting.set(true);
                try {
                    boolean result = this.hasTypeAt(type, shipyardPos);
                    if (result) {
                        cir.setReturnValue(true);
                        return;
                    }
                } finally {
                    skyjourney$isRedirecting.set(false);
                }
            }
        } catch (Exception e) {
        }
    }

    @Shadow
    public abstract boolean hasTypeAt(net.minecraft.registry.RegistryKey<PointOfInterestType> type, BlockPos pos);
}
