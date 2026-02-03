package celeste.skyjourney.mixin;

import celeste.skyjourney.common.GhostPOIManager;
import celeste.skyjourney.common.VSHelper;
import celeste.skyjourney.config.SkyJourneyConfig;

import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.registry.Registries;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.core.api.ships.Ship;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(VillagerEntity.class)
public class VillagerShipMixin {

    @Unique
    private int tickCounter = 0;

    @Unique
    private boolean skyjourney$foundShipPOI = false;

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        if (!SkyJourneyConfig.getInstance().enableVillagerFix)
            return;

        VillagerEntity entity = (VillagerEntity) (Object) this;
        // 20tickごとに実行
        if (tickCounter++ % 20 != 0)
            return;

        if (!(entity.getWorld() instanceof ServerWorld world))
            return;

        try {
            // 船のチェック
            if (!VSHelper.isOnShip(entity) && VSHelper.getShipManagingOrIntersecting(entity) == null) {
                skyjourney$foundShipPOI = false;
                return;
            }
            Ship ship = VSHelper.getShipManagingOrIntersecting(entity);
            if (ship == null) {
                skyjourney$foundShipPOI = false;
                return;
            }

            // 座標計算
            Vector3d localPos = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
            ship.getTransform().getWorldToShip().transformPosition(localPos);
            BlockPos shipyardPos = new BlockPos((int) localPos.x, (int) localPos.y, (int) localPos.z);

            PointOfInterestStorage poiStorage = world.getPointOfInterestStorage();
            int range = 4;

            // 早期手動補充チェック
            Brain<?> brain = entity.getBrain();
            if (brain.hasMemoryModule(MemoryModuleType.JOB_SITE)) {
                Optional<GlobalPos> jobSite = brain.getOptionalMemory(MemoryModuleType.JOB_SITE);

                if (jobSite != null && jobSite.isPresent()) {
                    GlobalPos currentJobWorld = jobSite.get();
                    if (currentJobWorld.getDimension() == world.getRegistryKey()) {
                        GhostPOIManager.add(currentJobWorld.getPos());
                        double distSq = entity.squaredDistanceTo(currentJobWorld.getPos().getX() + 0.5,
                                currentJobWorld.getPos().getY() + 0.5, currentJobWorld.getPos().getZ() + 0.5);

                        if (distSq < 9.0) {
                            tryManualRestock(world, entity);
                        }
                    }
                }
            }

            boolean isLocked = entity.getExperience() > 0;
            VillagerProfession currentProf = entity.getVillagerData().getProfession();

            // 空きのある候補を取得
            List<PointOfInterest> candidates = poiStorage.getInSquare(
                    (type) -> true,
                    shipyardPos,
                    range,
                    PointOfInterestStorage.OccupationStatus.ANY)
                    .filter(poi -> {
                        // ロック済みの場合、現在の職業と一致する必要がある
                        if (isLocked) {
                            return currentProf.heldWorkstation().test(poi.getType());
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // 現在の仕事場所を追加（有効な場合）
            BlockPos currentJobShipyardPos = null;
            if (brain.hasMemoryModule(MemoryModuleType.JOB_SITE)) {
                Optional<GlobalPos> jobSite = brain.getOptionalMemory(MemoryModuleType.JOB_SITE);
                if (jobSite != null && jobSite.isPresent()) {
                    GlobalPos currentJobWorld = jobSite.get();
                    // このゴースト座標を維持
                    GhostPOIManager.add(currentJobWorld.getPos());

                    Vector3d currentJobLocal = new Vector3d(currentJobWorld.getPos().getX() + 0.5,
                            currentJobWorld.getPos().getY() + 0.5, currentJobWorld.getPos().getZ() + 0.5);
                    ship.getTransform().getWorldToShip().transformPosition(currentJobLocal);
                    currentJobShipyardPos = new BlockPos((int) currentJobLocal.x, (int) currentJobLocal.y,
                            (int) currentJobLocal.z);

                    final BlockPos targetPos = currentJobShipyardPos;
                    poiStorage.getInSquare(t -> true, targetPos, 1, PointOfInterestStorage.OccupationStatus.ANY)
                            .filter(poi -> poi.getPos().equals(targetPos))
                            .findFirst()
                            .ifPresent(poi -> {
                                boolean matches = true;
                                if (isLocked) {
                                    matches = currentProf.heldWorkstation().test(poi.getType());
                                }

                                if (matches && candidates.stream().noneMatch(c -> c.getPos().equals(targetPos))) {
                                    candidates.add(poi);
                                }
                            });
                }
            }

            if (candidates.isEmpty()) {
                skyjourney$foundShipPOI = false;
                return;
            }

            // 距離順にソート
            candidates.sort((p1, p2) -> {
                Vector3d gp1 = new Vector3d(p1.getPos().getX() + 0.5, p1.getPos().getY() + 0.5,
                        p1.getPos().getZ() + 0.5);
                ship.getTransform().getShipToWorld().transformPosition(gp1);

                Vector3d gp2 = new Vector3d(p2.getPos().getX() + 0.5, p2.getPos().getY() + 0.5,
                        p2.getPos().getZ() + 0.5);
                ship.getTransform().getShipToWorld().transformPosition(gp2);

                double d1 = entity.squaredDistanceTo(gp1.x, gp1.y, gp1.z);
                double d2 = entity.squaredDistanceTo(gp2.x, gp2.y, gp2.z);
                return Double.compare(d1, d2);
            });

            // 最適なPOIを選択
            boolean success = false;
            for (PointOfInterest bestPOI : candidates) {
                boolean isCurrent = bestPOI.getPos().equals(currentJobShipyardPos);

                if (isCurrent) {
                    success = true;
                    skyjourney$foundShipPOI = true;
                    if (!isLocked) {
                        ensureProfession(entity, bestPOI.getType());
                    }
                    break;
                } else {
                    PointOfInterestAccessor accessor = (PointOfInterestAccessor) bestPOI;
                    boolean canClaim = false;

                    if (accessor.getFreeTickets() > 0) {
                        canClaim = accessor.invokeReserveTicket();
                    } else {
                        // リーク修正 / 乗っ取り: 近くにいて満員の場合、ゴースト予約とみなして乗っ取り/リセットを行う。
                        Vector3d gp = new Vector3d(bestPOI.getPos().getX() + 0.5, bestPOI.getPos().getY() + 0.5,
                                bestPOI.getPos().getZ() + 0.5);
                        ship.getTransform().getShipToWorld().transformPosition(gp);
                        double distSq = entity.squaredDistanceTo(gp.x, gp.y, gp.z);

                        if (distSq < 9.0) {
                            // 強制解放して再取得
                            poiStorage.releaseTicket(bestPOI.getPos());
                            canClaim = accessor.invokeReserveTicket();

                            if (canClaim) {
                                tryManualRestock(world, entity);
                            }
                        }
                    }

                    if (canClaim) {
                        if (currentJobShipyardPos != null) {
                            poiStorage.releaseTicket(currentJobShipyardPos);
                        }
                        injectPOI(world, entity, ship, bestPOI.getPos());
                        if (!isLocked) {
                            ensureProfession(entity, bestPOI.getType());
                        }

                        success = true;
                        skyjourney$foundShipPOI = true;
                        break;
                    }
                }
            }

            if (!success) {
                skyjourney$foundShipPOI = false;
            }

            // セカンダリ（ベッド等）の注入
            if (brain.hasMemoryModule(MemoryModuleType.SECONDARY_JOB_SITE)) {
                List<GlobalPos> secList = new ArrayList<>();
                for (PointOfInterest poi : candidates) {
                    if (isLocked && !currentProf.heldWorkstation().test(poi.getType()))
                        continue;

                    Vector3d gp = new Vector3d(poi.getPos().getX() + 0.5, poi.getPos().getY() + 0.5,
                            poi.getPos().getZ() + 0.5);
                    ship.getTransform().getShipToWorld().transformPosition(gp);
                    BlockPos ghostBlockPos = new BlockPos((int) gp.x, (int) gp.y, (int) gp.z);
                    GhostPOIManager.add(ghostBlockPos);
                    secList.add(GlobalPos.create(world.getRegistryKey(), ghostBlockPos));
                }
                brain.remember(MemoryModuleType.SECONDARY_JOB_SITE, secList);
            }

            // 手動補充ロジック
            // foundShipPOIとは独立して、もし仕事を持っていてそこにいるなら、働く。
            if (currentJobShipyardPos != null) {
                // 距離チェックのためのワールド座標計算
                Vector3d ghostPosVec = new Vector3d(currentJobShipyardPos.getX() + 0.5,
                        currentJobShipyardPos.getY() + 0.5, currentJobShipyardPos.getZ() + 0.5);
                ship.getTransform().getShipToWorld().transformPosition(ghostPosVec);
                double distSq = entity.squaredDistanceTo(ghostPosVec.x, ghostPosVec.y, ghostPosVec.z);

                if (distSq < 9.0) {
                    tryManualRestock(world, entity);
                }
            }

        } catch (Exception e) {
            skyjourney$foundShipPOI = false;
        }
    }

    @Unique
    private void tryManualRestock(ServerWorld world, VillagerEntity entity) {
        long time = world.getTimeOfDay() % 24000;
        boolean isWorkTime = time > 2000 && time < 9000;

        VillagerEntityAccessor accessor = (VillagerEntityAccessor) entity;
        long lastRestock = accessor.getLastRestockTime();
        int count = accessor.getRestocksToday();

        // 日付リセット
        if (count > 0 && time < 2500) {
            accessor.setRestocksToday(0);
            count = 0;
        }

        if (isWorkTime) {
            // 制限チェック
            if (count < 2 && world.getTime() > lastRestock + 200) {
                accessor.invokeRestock();
                accessor.invokePlayWorkSound();
            }
        }
    }

    @Unique
    private void injectPOI(ServerWorld world, VillagerEntity entity, Ship ship, BlockPos shipyardPos) {
        Vector3d ghostPosVec = new Vector3d(shipyardPos.getX() + 0.5, shipyardPos.getY() + 0.5,
                shipyardPos.getZ() + 0.5);
        ship.getTransform().getShipToWorld().transformPosition(ghostPosVec);
        BlockPos ghostBlockPos = new BlockPos((int) ghostPosVec.x, (int) ghostPosVec.y, (int) ghostPosVec.z);
        GlobalPos target = GlobalPos.create(world.getRegistryKey(), ghostBlockPos);

        // この座標へのリダイレクトを許可
        GhostPOIManager.add(target.getPos());

        Brain<?> brain = entity.getBrain();
        brain.remember(MemoryModuleType.POTENTIAL_JOB_SITE, target);
        brain.remember(MemoryModuleType.JOB_SITE, target);
    }

    @Unique
    private void ensureProfession(VillagerEntity entity, RegistryEntry<PointOfInterestType> poiType) {
        if (entity.getVillagerData().getProfession() == VillagerProfession.NONE) {
            for (VillagerProfession prof : Registries.VILLAGER_PROFESSION) {
                if (prof.heldWorkstation().test(poiType)) {
                    entity.setVillagerData(entity.getVillagerData().withProfession(prof));
                    break;
                }
            }
        }
    }

    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void onSetVillagerData(VillagerData data, CallbackInfo ci) {
        if (!SkyJourneyConfig.getInstance().enableVillagerFix)
            return;

        VillagerEntity entity = (VillagerEntity) (Object) this;

        // ロック済み（XP > 0）: 職業を失ってはならない
        if (entity.getExperience() > 0 && data.getProfession() == VillagerProfession.NONE) {
            ci.cancel();
            return;
        }

        // 船上: POIが見つかった場合、職業を保護
        if (data.getProfession() == VillagerProfession.NONE
                && entity.getVillagerData().getProfession() != VillagerProfession.NONE) {

            if (skyjourney$foundShipPOI) {
                ci.cancel();
            }
        }
    }
}
