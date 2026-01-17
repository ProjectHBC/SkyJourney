package celeste.skyjourney.client;

import celeste.skyjourney.config.SkyJourneyConfig;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class MemoryStatsHUD implements HudRenderCallback {

    // クライアントサイドHUD用キャッシュ
    public static long lastTotalMemory = 0;
    public static long lastFreeMemory = 0;
    public static long lastMaxMemory = 0;
    public static int skippedChunks = 0;
    public static int totalChunks = 0;

    public static int clientMinY = 0;
    public static int clientMaxY = 0;

    public static void updateStats(long total, long free, long max, int skipped, int totalC, int minY, int maxY) {
        lastTotalMemory = total;
        lastFreeMemory = free;
        lastMaxMemory = max;
        skippedChunks = skipped;
        totalChunks = totalC;
        clientMinY = minY;
        clientMaxY = maxY;
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (!SkyJourneyConfig.showDebugHUD) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.debugEnabled) {
            return;
        }

        long used = lastTotalMemory - lastFreeMemory;
        long max = lastMaxMemory;
        int pct = (int) ((double) used / max * 100);

        String memText = String.format("Server Mem: %d%% (%d/%d MB)", pct, used / 1024 / 1024, max / 1024 / 1024);

        String bakingText;
        if (totalChunks > 0) {
            int eff = (int) ((double) skippedChunks / totalChunks * 100);
            bakingText = String.format("Baking Opt: %d%% (Skip: %d / Total: %d)", eff, skippedChunks, totalChunks);
        } else {
            bakingText = "Baking Opt: 100% (Idle)";
        }

        int x = 10;
        int y = 10;

        context.drawText(client.textRenderer, memText, x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, bakingText, x, y + 10, 0xFFAA00, true);
    }
}
