package celeste.skyjourney.feature.sneak;

import net.minecraft.util.math.Box;
import org.joml.Matrix4dc;
import java.util.ArrayList;
import java.util.List;

public class SneakDebugData {
    public static class SneakBoxRecord {
        public Box box;
        public Matrix4dc shipToWorld;
        public long expireTime;
        public boolean isSafe;

        public SneakBoxRecord(Box box, Matrix4dc shipToWorld, boolean isSafe) {
            this.box = box;
            this.shipToWorld = shipToWorld;
            this.isSafe = isSafe;
            this.expireTime = System.currentTimeMillis() + 100; // 0.1秒で消える
        }
    }

    // 共通の箱データ置き場
    public static final List<SneakBoxRecord> BOXES = new ArrayList<>();

    public static void addBox(Box box, Matrix4dc shipToWorld, boolean isSafe) {
        synchronized (BOXES) {
            BOXES.add(new SneakBoxRecord(box, shipToWorld, isSafe));
        }
    }
}