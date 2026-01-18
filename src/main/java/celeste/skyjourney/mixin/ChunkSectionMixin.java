package celeste.skyjourney.mixin;

import celeste.skyjourney.common.IDimensionAware;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IDimensionAware {

    @Unique
    private String dimensionId;

    @Override
    public String skyjourney$getDimensionId() {
        return this.dimensionId;
    }

    @Override
    public void skyjourney$setDimensionId(String dimensionId) {
        this.dimensionId = dimensionId;
    }
}
