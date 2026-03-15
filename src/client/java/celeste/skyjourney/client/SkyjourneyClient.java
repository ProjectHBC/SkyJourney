package celeste.skyjourney.client;

import celeste.skyjourney.SkyJourneyMod;
import celeste.skyjourney.network.PacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import celeste.skyjourney.config.SkyJourneyConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.CONFIG_SYNC_ID,
                (client, handler, buf, responseSender) -> {
                    String json = buf.readString();
                    client.execute(() -> {
                        // シングルプレイ（ホスト）の場合は同期を無視し、ローカル設定を優先する
                        if (!client.isInSingleplayer()) {
                            SkyJourneyConfig.applyServerConfig(json);
                            SkyJourneyMod.LOGGER.info("[SkyJourney] Applied server configuration.");
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(PacketHandler.MASS_SYNC_ID,
            (client, handler, buf, responseSender) -> {
                int count = buf.readInt();
                Map<Identifier, Double> receivedMap = new HashMap<>();
                for (int i = 0; i < count; i++) {
                    Identifier id = buf.readIdentifier();
                    double mass = buf.readDouble();
                    receivedMap.put(id, mass);
                }

                client.execute(() -> {
                    try {
                        // MassDatapackResolver.INSTANCEを取得
                        Class<?> resolverClass = Class.forName("org.valkyrienskies.mod.common.config.MassDatapackResolver");
                        Field instanceField = resolverClass.getDeclaredField("INSTANCE");
                        Object resolverInstance = instanceField.get(null);

                        // 'map'フィールドを取得して更新
                        Field mapField = resolverClass.getDeclaredField("map");
                        mapField.setAccessible(true);
                        Map map = (Map) mapField.get(resolverInstance);

                        // VSBlockStateInfoクラスとコンストラクタを取得
                        Class<?> infoClass = Class.forName("org.valkyrienskies.mod.common.config.VSBlockStateInfo");
                        Constructor<?> constructor = infoClass.getDeclaredConstructors()[0]; 
                        constructor.setAccessible(true);

                        // 受信したデータでマップを更新
                        for (Map.Entry<Identifier, Double> entry : receivedMap.entrySet()) {
                            Identifier id = entry.getKey();
                            double mass = entry.getValue();
                            
                            // VSBlockStateInfoインスタンスを作成
                            Object info = constructor.newInstance(id, 200, mass, 0.5, 0.3, null);
                            
                            map.put(id, info);
                        }

                        // MassDatapackResolverのブロック状態を初期化
                        try {
                             Method registerMethod = resolverClass.getMethod("registerAllBlockStates", Iterable.class);
                             
                             // 全ての BlockStateを収集
                             List<BlockState> allStates = new ArrayList<>();
                             for (Block block : Registries.BLOCK) {
                                 allStates.addAll(block.getStateManager().getStates());
                             }
                             
                             registerMethod.invoke(resolverInstance, allStates);
                             
                        } catch (Exception e) {
                             SkyJourneyMod.LOGGER.error("[SkyJourney] Failed to register block states: " + e.getMessage());
                             e.printStackTrace();
                        }

                        // VS2 BlockStateInfo キャッシュをクリア
                        Class<?> blockStateInfoClass = Class.forName("org.valkyrienskies.mod.common.BlockStateInfo");
                        Field cacheField = blockStateInfoClass.getDeclaredField("_cache");
                        cacheField.setAccessible(true);
                        ThreadLocal<?> threadLocal = (ThreadLocal<?>) cacheField.get(null);
                        Object cacheInstance = threadLocal.get();
                        
                        if (cacheInstance != null) {
                            Field blockStateCacheField = cacheInstance.getClass().getDeclaredField("blockStateCache");
                            blockStateCacheField.setAccessible(true);
                            ((Map<?, ?>) blockStateCacheField.get(cacheInstance)).clear();
                        }

                        // SORTED_REGISTRYを強制初期化
                        try {
                            Class<?> blockStateInfoClassForReg = Class.forName("org.valkyrienskies.mod.common.BlockStateInfo");
                            Field registryField = blockStateInfoClassForReg.getDeclaredField("REGISTRY");
                            registryField.setAccessible(true);
                            Registry registry = (Registry) registryField.get(null);
                            
                            // 優先度順にソートするためのリストを作成
                            List<Object> providers = new ArrayList<>();
                            for (Object obj : registry) {
                                providers.add(obj);
                            }
                            
                            providers.sort((a, b) -> {
                                try {
                                    Method getPriority = a.getClass().getMethod("getPriority");
                                    int pA = (int) getPriority.invoke(a);
                                    int pB = (int) getPriority.invoke(b);
                                    return Integer.compare(pB, pA); // 降順
                                } catch (Exception e) {
                                    return 0;
                                }
                            });
                            
                            // SORTED_REGISTRYに設定
                            Field sortedRegistryField = blockStateInfoClassForReg.getDeclaredField("SORTED_REGISTRY");
                            sortedRegistryField.setAccessible(true);
                            sortedRegistryField.set(null, providers);

                        } catch (Exception e) {
                            SkyJourneyMod.LOGGER.error("[SkyJourney] Failed to force init SORTED_REGISTRY: " + e.getMessage());
                            e.printStackTrace();
                        }

                    } catch (ClassNotFoundException e) {
                        SkyJourneyMod.LOGGER.error("[SkyJourney] Class not found during mass sync: " + e.getMessage());
                    } catch (NoSuchFieldException e) {
                        SkyJourneyMod.LOGGER.error("[SkyJourney] Field not found during mass sync: " + e.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                        SkyJourneyMod.LOGGER.error("[SkyJourney] Failed to sync mass data: " + e.getMessage());
                    }
                });
            });

        ClientPlayConnectionEvents.DISCONNECT
                .register((handler, client) -> {
                    SkyJourneyConfig.restoreLocalConfig();
                    SkyJourneyMod.LOGGER.info("[SkyJourney] Restored local configuration.");
                });

        HudRenderCallback.EVENT.register(new MemoryStatsHUD());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(OptimizationDebugRenderer::render);
        WorldRenderEvents.LAST.register(SneakDebugRenderer::render);
    }
}
