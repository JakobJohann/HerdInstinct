package jakobify.herdinstinct;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class HerdPanicUtil {
    private static final String PANIC_UNTIL_KEY = HerdInstinct.MODID + "_panic_until";
    private static final Map<Integer, PanicState> ACTIVE_PANIC = new ConcurrentHashMap<>();
    private static final int PANIC_UPDATE_INTERVAL_TICKS = 10;

    private HerdPanicUtil() {
    }

    public static boolean isOnCooldown(Animal animal, long currentGameTime) {
        return currentGameTime < animal.getPersistentData().getLong(PANIC_UNTIL_KEY);
    }

    public static void applyPanic(Animal animal, Vec3 threatPosition, double panicSpeed, double panicDistance, int cooldownTicks, long currentGameTime) {
        if (!(animal instanceof PathfinderMob mob)) {
            return;
        }

        long panicUntil = currentGameTime + cooldownTicks;
        HerdInstinct.LOGGER.info(
                "HerdInstinct panic applied: entity={}, uuid={}, pos={}, threat={}, untilTick={}",
                animal.getType(),
                animal.getUUID(),
                animal.blockPosition(),
                threatPosition,
                panicUntil
        );
        animal.getPersistentData().putLong(PANIC_UNTIL_KEY, panicUntil);
        animal.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        ACTIVE_PANIC.put(animal.getId(), new PanicState(
                animal.getUUID(),
                animal.level().dimension(),
                threatPosition,
                panicSpeed,
                panicDistance,
                panicUntil
        ));
        updatePanicPath(mob, threatPosition, panicSpeed, panicDistance);
    }

    public static void tickActivePanics(ServerTickEvent.Post event) {
        Iterator<Map.Entry<Integer, PanicState>> iterator = ACTIVE_PANIC.entrySet().iterator();
        while (iterator.hasNext()) {
            PanicState state = iterator.next().getValue();
            ServerLevel level = event.getServer().getLevel(state.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }

            Entity entity = level.getEntity(state.entityUuid());
            if (!(entity instanceof PathfinderMob mob) || !(entity instanceof Animal animal) || !animal.isAlive()) {
                iterator.remove();
                continue;
            }

            if (level.getGameTime() >= state.panicUntil()) {
                stopPanic(mob, animal);
                iterator.remove();
                continue;
            }

            if (level.getGameTime() % PANIC_UPDATE_INTERVAL_TICKS == 0 || mob.getNavigation().isDone()) {
                updatePanicPath(mob, state.threatPosition(), state.panicSpeed(), state.panicDistance());
            }
        }
    }

    private static void updatePanicPath(PathfinderMob mob, Vec3 threatPosition, double panicSpeed, double panicDistance) {
        Vec3 fleeTarget = findFleeTarget(mob, threatPosition, panicDistance);
        if (fleeTarget != null) {
            boolean started = mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, panicSpeed);
            mob.getMoveControl().setWantedPosition(fleeTarget.x, fleeTarget.y, fleeTarget.z, panicSpeed);
            if (!started) {
                HerdInstinct.LOGGER.info(
                        "HerdInstinct panic fallback move: entity={}, from={}, threat={}, target={}",
                        mob.getType(),
                        mob.blockPosition(),
                        threatPosition,
                        fleeTarget
                );
            }
        }
    }

    private static Vec3 findFleeTarget(PathfinderMob mob, Vec3 threatPosition, double panicDistance) {
        Vec3 awayTarget = DefaultRandomPos.getPosAway(mob, (int) Math.ceil(panicDistance), 7, threatPosition);
        if (awayTarget != null) {
            return awayTarget;
        }

        Vec3 direction = mob.position().subtract(threatPosition);
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = new Vec3(1.0D, 0.0D, 0.0D);
        }
        return mob.position().add(direction.normalize().scale(panicDistance));
    }

    private static void stopPanic(PathfinderMob mob, Animal animal) {
        mob.getNavigation().stop();
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0D);
        animal.getPersistentData().remove(PANIC_UNTIL_KEY);
    }

    private record PanicState(
            java.util.UUID entityUuid,
            ResourceKey<Level> dimension,
            Vec3 threatPosition,
            double panicSpeed,
            double panicDistance,
            long panicUntil
    ) {
    }
}
