package celeste.skyjourney.mixin.accessor;

import net.minecraft.world.poi.PointOfInterest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PointOfInterest.class)
public interface PointOfInterestAccessor {
    @Invoker("reserveTicket")
    boolean invokeReserveTicket();

    @Accessor("freeTickets")
    int getFreeTickets();
}
