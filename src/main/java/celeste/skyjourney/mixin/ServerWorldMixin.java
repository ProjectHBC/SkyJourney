package celeste.skyjourney.mixin;

import celeste.skyjourney.common.WorldAwareStorage;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ServerWorld self = (ServerWorld) (Object) this;
        // POIストレージにワールド参照に
        if (self.getPointOfInterestStorage() instanceof WorldAwareStorage) {
            ((WorldAwareStorage) self.getPointOfInterestStorage()).skyjourney$setWorld(self);
            celeste.skyjourney.SkyJourneyMod.LOGGER
                    .info("[ServerWorldMixin] Injected World reference into PointOfInterestStorage for {}",
                            self.getRegistryKey().getValue());
        }
    }
}
