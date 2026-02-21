package celeste.skyjourney.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class OptimizationDebugRenderer {

    static class DebugBox {
        int x, y, z;
        int status; // 0=スキップ(赤), 1=ベイク済み(緑)
        long expireTime;

        public DebugBox(int x, int y, int z, int status) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.status = status;
            this.expireTime = System.currentTimeMillis() + 2000; // 2秒間表示
        }
    }

    private static final List<DebugBox> boxes = new ArrayList<>();

    public static void addBox(int x, int y, int z, int status) {
        synchronized (boxes) {
            boxes.add(new DebugBox(x, y, z, status));
        }
    }

    public static void render(WorldRenderContext context) {
        if (!celeste.skyjourney.config.SkyJourneyConfig.getInstance().showDebugHUD) {
            return;
        }

        long now = System.currentTimeMillis();

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

        MatrixStack pStack = context.matrixStack();
        pStack.push();
        pStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f matrix = pStack.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull(); // 内側/外側の両方を表示

        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        synchronized (boxes) {
            Iterator<DebugBox> it = boxes.iterator();
            while (it.hasNext()) {
                DebugBox box = it.next();
                if (now > box.expireTime) {
                    it.remove();
                    continue;
                }

                float alpha = (box.expireTime - now) / 2000f;
                float r = box.status == 0 ? 1.0f : 0.0f; // スキップ時は赤
                float g = box.status == 1 ? 1.0f : 0.0f; // ベイク済みは緑
                float b = 0.0f;

                drawBox(buffer, matrix, box.x * 16, box.y * 16, box.z * 16, r, g, b, alpha);
            }
        }

        tessellator.draw();

        RenderSystem.enableCull();
        pStack.pop();
        RenderSystem.lineWidth(1.0f);
    }

    private static void drawBox(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z, float r, float g,
            float b, float a) {
        double s = 16.0; // チャンクセクションのサイズ

        // Bottom
        line(buffer, matrix, x, y, z, x + s, y, z, r, g, b, a);
        line(buffer, matrix, x + s, y, z, x + s, y, z + s, r, g, b, a);
        line(buffer, matrix, x + s, y, z + s, x, y, z + s, r, g, b, a);
        line(buffer, matrix, x, y, z + s, x, y, z, r, g, b, a);

        // Top
        line(buffer, matrix, x, y + s, z, x + s, y + s, z, r, g, b, a);
        line(buffer, matrix, x + s, y + s, z, x + s, y + s, z + s, r, g, b, a);
        line(buffer, matrix, x + s, y + s, z + s, x, y + s, z + s, r, g, b, a);
        line(buffer, matrix, x, y + s, z + s, x, y + s, z, r, g, b, a);

        // Verticals
        line(buffer, matrix, x, y, z, x, y + s, z, r, g, b, a);
        line(buffer, matrix, x + s, y, z, x + s, y + s, z, r, g, b, a);
        line(buffer, matrix, x + s, y, z + s, x + s, y + s, z + s, r, g, b, a);
        line(buffer, matrix, x, y, z + s, x, y + s, z + s, r, g, b, a);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2,
            double y2, double z2, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a).next();
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a).next();
    }
}
