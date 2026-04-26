package jakobify.herdinstinct;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class HerdPanicUtil {
    private static final Map<UUID, Long> PANIC_UNTIL = new HashMap<>();
    private static final Map<UUID, PanicState> ACTIVE_PANIC = new HashMap<>();
    private static final int PANIC_UPDATE_INTERVAL_TICKS = 10;

    private HerdPanicUtil() {
    }

    public static boolean isOnCooldown(Animal animal, long currentGameTime) {
        Long panicUntil = PANIC_UNTIL.get(animal.getUUID());
        if (panicUntil == null) {
            return false;
        }

        if (currentGameTime >= panicUntil) {
            PANIC_UNTIL.remove(animal.getUUID());
            ACTIVE_PANIC.remove(animal.getUUID());
            return false;
        }

        return true;
    }

    public static void applyPanic(Animal animal, Vec3 threatPosition, double panicSpeed, double panicDistance, int cooldownTicks, long currentGameTime) {
        if (!(animal instanceof PathfinderMob mob)) {
            return;
        }

        long panicUntil = currentGameTime + cooldownTicks;
        UUID entityUuid = animal.getUUID();
        PANIC_UNTIL.put(entityUuid, panicUntil);
        animal.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        ACTIVE_PANIC.put(entityUuid, new PanicState(
                entityUuid,
                animal.level().dimension(),
                threatPosition,
                panicSpeed,
                panicDistance,
                panicUntil
        ));
        updatePanicPath(mob, threatPosition, panicSpeed, panicDistance);
    }

    public static void tickActivePanics(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PanicState>> iterator = ACTIVE_PANIC.entrySet().iterator();
        while (iterator.hasNext()) {
            PanicState state = iterator.next().getValue();
            ServerLevel level = server.getLevel(state.dimension());
            if (level == null) {
                PANIC_UNTIL.remove(state.entityUuid());
                iterator.remove();
                continue;
            }

            Entity entity = level.getEntity(state.entityUuid());
            if (!(entity instanceof PathfinderMob mob) || !(entity instanceof Animal animal) || !animal.isAlive()) {
                PANIC_UNTIL.remove(state.entityUuid());
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
            mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, panicSpeed);
            mob.getMoveControl().setWantedPosition(fleeTarget.x, fleeTarget.y, fleeTarget.z, panicSpeed);
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
        PANIC_UNTIL.remove(animal.getUUID());
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
