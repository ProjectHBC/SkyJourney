package celeste.skyjourney.common;

import net.minecraft.server.world.ServerWorld;

public interface WorldAwareStorage {
    void skyjourney$setWorld(ServerWorld world);

    ServerWorld skyjourney$getWorld();
}
