package celeste.skyjourney.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4dc;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import celeste.skyjourney.feature.sneak.SneakDebugData;

public class SneakDebugRenderer {

    static class SneakBoxRecord {
        Box box;
        Matrix4dc shipToWorld; // VS2の座標変換行列
        long expireTime;
        boolean isSafe; // true=緑, false=赤

        public SneakBoxRecord(Box box, Matrix4dc shipToWorld, boolean isSafe) {
            this.box = box;
            this.shipToWorld = shipToWorld;
            this.isSafe = isSafe;
            this.expireTime = System.currentTimeMillis() + 100; // 毎ティック呼ばれるので0.1秒表示
        }
    }

    private static final List<SneakBoxRecord> boxes = new ArrayList<>();

    // スニーク処理からこれを呼び出して箱を登録する
    public static void addBox(Box box, Matrix4dc shipToWorld, boolean isSafe) {
        synchronized (boxes) {
            boxes.add(new SneakBoxRecord(box, shipToWorld, isSafe));
        }
    }

    public static void render(WorldRenderContext context) {
        if (!celeste.skyjourney.config.SkyJourneyConfig.getInstance().showDebugSneakBox) {
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
        RenderSystem.disableCull(); 

        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // 共通データ置き場 (SneakDebugData.BOXES) を読みに行く
        synchronized (SneakDebugData.BOXES) {
            Iterator<SneakDebugData.SneakBoxRecord> it = SneakDebugData.BOXES.iterator();
            while (it.hasNext()) {
                SneakDebugData.SneakBoxRecord record = it.next();
                if (now > record.expireTime) {
                    it.remove();
                    continue;
                }

                float r = record.isSafe ? 0.0f : 1.0f;
                float g = record.isSafe ? 1.0f : 0.0f;
                float b = 0.0f;
                float alpha = 0.8f;

                drawTransformedBox(buffer, matrix, record.box, record.shipToWorld, r, g, b, alpha);
            }
        }

        tessellator.draw();
        RenderSystem.enableCull();
        pStack.pop();
        RenderSystem.lineWidth(1.0f);
    }

    // ローカル座標の箱をワールド座標に変換して描画する処理
    private static void drawTransformedBox(BufferBuilder buffer, Matrix4f matrix, Box box, Matrix4dc shipToWorld, float r, float g, float b, float a) {
        // 8つの頂点をワールド座標へ変換
        Vector3d c000 = transform(box.minX, box.minY, box.minZ, shipToWorld);
        Vector3d c100 = transform(box.maxX, box.minY, box.minZ, shipToWorld);
        Vector3d c010 = transform(box.minX, box.maxY, box.minZ, shipToWorld);
        Vector3d c110 = transform(box.maxX, box.maxY, box.minZ, shipToWorld);
        Vector3d c001 = transform(box.minX, box.minY, box.maxZ, shipToWorld);
        Vector3d c101 = transform(box.maxX, box.minY, box.maxZ, shipToWorld);
        Vector3d c011 = transform(box.minX, box.maxY, box.maxZ, shipToWorld);
        Vector3d c111 = transform(box.maxX, box.maxY, box.maxZ, shipToWorld);

        // 下面
        line(buffer, matrix, c000, c100, r, g, b, a);
        line(buffer, matrix, c100, c101, r, g, b, a);
        line(buffer, matrix, c101, c001, r, g, b, a);
        line(buffer, matrix, c001, c000, r, g, b, a);
        // 上面
        line(buffer, matrix, c010, c110, r, g, b, a);
        line(buffer, matrix, c110, c111, r, g, b, a);
        line(buffer, matrix, c111, c011, r, g, b, a);
        line(buffer, matrix, c011, c010, r, g, b, a);
        // 柱
        line(buffer, matrix, c000, c010, r, g, b, a);
        line(buffer, matrix, c100, c110, r, g, b, a);
        line(buffer, matrix, c001, c011, r, g, b, a);
        line(buffer, matrix, c101, c111, r, g, b, a);
    }

    private static Vector3d transform(double x, double y, double z, Matrix4dc shipToWorld) {
        Vector3d vec = new Vector3d(x, y, z);
        if (shipToWorld != null) {
            shipToWorld.transformPosition(vec);
        }
        return vec;
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, Vector3d v1, Vector3d v2, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, a).next();
        buffer.vertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, a).next();
    }
}