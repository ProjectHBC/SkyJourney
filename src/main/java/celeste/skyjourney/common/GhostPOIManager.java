package celeste.skyjourney.common;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GhostPOIManager {
    private static final Map<BlockPos, Long> validGhostPositions = new ConcurrentHashMap<>();
    private static final long EXPIRATION_MS = 5000;

    public static void add(BlockPos pos) {
        validGhostPositions.put(pos, System.currentTimeMillis());
    }

    public static boolean isValid(BlockPos pos) {
        Long timestamp = validGhostPositions.get(pos);
        if (timestamp == null)
            return false;

        if (System.currentTimeMillis() - timestamp > EXPIRATION_MS) {
            validGhostPositions.remove(pos);
            return false;
        }
        return true;
    }
}
