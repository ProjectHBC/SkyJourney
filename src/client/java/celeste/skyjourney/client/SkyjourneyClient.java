package celeste.skyjourney.client;

import celeste.skyjourney.SkyJourneyMod;
import celeste.skyjourney.network.PacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class SkyjourneyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SkyJourneyMod.LOGGER.info("[SkyJourney] Initializing Client...");

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.MEMORY_STATS_ID,
                (client, handler, buf, responseSender) -> {
                    long total = buf.readLong();
                    long free = buf.readLong();
                    long max = buf.readLong();
                    int skipped = buf.readInt();
                    int totalChunks = buf.readInt();
                    int minY = buf.readInt();
                    int maxY = buf.readInt();

                    client.execute(() -> {
                        MemoryStatsHUD.updateStats(total, free, max, skipped, totalChunks, minY, maxY);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.CHUNK_DEBUG_ID,
                (client, handler, buf, responseSender) -> {
                    int count = buf.readInt();
                    List<int[]> updates = new ArrayList<>();
                    for (int i = 0; i < count; i += 4) {
                        int x = buf.readInt();
                        int y = buf.readInt();
                        int z = buf.readInt();
                        int status = buf.readInt();
                        updates.add(new int[] { x, y, z, status });
                    }

                    client.execute(() -> {
                        for (int[] u : updates) {
                            OptimizationDebugRenderer.addBox(u[0], u[1], u[2], u[3]);
                        }
                    });
                });

        HudRenderCallback.EVENT.register(new MemoryStatsHUD());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(OptimizationDebugRenderer::render);
    }
}
