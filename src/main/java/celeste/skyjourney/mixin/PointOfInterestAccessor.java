package celeste.skyjourney.mixin;

import net.minecraft.world.poi.PointOfInterest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PointOfInterest.class)
public interface PointOfInterestAccessor {
    @Invoker("reserveTicket")
    boolean invokeReserveTicket();

    @org.spongepowered.asm.mixin.gen.Accessor("freeTickets")
    int getFreeTickets();
}
