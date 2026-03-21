package celeste.skyjourney.mixin.accessor;

import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VillagerEntity.class)
public interface VillagerEntityAccessor {
    @Invoker("restock")
    void invokeRestock();

    @Invoker("playWorkSound")
    void invokePlayWorkSound();

    @Accessor("lastRestockTime")
    long getLastRestockTime();

    @Accessor("restocksToday")
    int getRestocksToday();

    @Accessor("restocksToday")
    void setRestocksToday(int count);
}
