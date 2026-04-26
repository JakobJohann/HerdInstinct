package jakobify.herdinstinct;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HerdPanicEvents {
    public static final int HERD_RADIUS = 12;
    public static final double PANIC_SPEED = 1.5D;
    public static final double PANIC_DISTANCE = 14.0D;

    private HerdPanicEvents() {
    }

    public static void onAnimalDamaged(LivingEntity entity, DamageSource source, float damageTaken) {
        if (HerdInstinctConfig.get().panicTriggerMode != HerdInstinctConfig.PanicTriggerMode.PLAYER_HIT) {
            return;
        }
        if (damageTaken <= 0.0F) {
            return;
        }
        if (!(entity instanceof Animal victim)) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!HerdInstinctConfig.isEnabled() || !HerdGroupUtil.canTriggerPanic(victim)) {
            return;
        }
        if (!(source.getEntity() instanceof Player player)) {
            return;
        }

        long currentGameTime = serverLevel.getGameTime();
        if (HerdPanicUtil.isOnCooldown(victim, currentGameTime)) {
            return;
        }

        triggerPanic(victim, serverLevel, currentGameTime, resolveThreatPosition(source, victim), player);
    }

    public static void onAnimalDeath(LivingEntity entity, DamageSource source) {
        if (HerdInstinctConfig.get().panicTriggerMode != HerdInstinctConfig.PanicTriggerMode.ON_DEATH) {
            return;
        }
        if (!(entity instanceof Animal victim)) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!HerdInstinctConfig.isEnabled() || !HerdGroupUtil.canTriggerPanic(victim)) {
            return;
        }

        panicNearbyAnimals(victim, serverLevel, serverLevel.getGameTime(), resolveThreatPosition(source, victim));
    }

    public static void onServerTick(MinecraftServer server) {
        HerdPanicUtil.tickActivePanics(server);
    }

    private static void triggerPanic(Animal victim, ServerLevel serverLevel, long currentGameTime, Vec3 threatPosition, Player player) {
        panicNearbyAnimals(victim, serverLevel, currentGameTime, threatPosition);
    }

    private static void panicNearbyAnimals(Animal victim, ServerLevel serverLevel, long currentGameTime, Vec3 threatPosition) {
        if (threatPosition == null) {
            threatPosition = victim.position();
        }

        double herdRadius = HerdInstinctConfig.getHerdRadius();
        AABB searchBox = victim.getBoundingBox().inflate(herdRadius);
        List<Animal> nearbyAnimals = serverLevel.getEntitiesOfClass(Animal.class, searchBox,
                animal -> animal.isAlive()
                        && HerdGroupUtil.canTriggerPanic(animal)
                        && shouldAffectAnimal(victim, animal));
        if (nearbyAnimals.isEmpty()) {
            return;
        }

        for (Animal animal : nearbyAnimals) {
            HerdPanicUtil.applyPanic(animal, threatPosition, PANIC_SPEED, PANIC_DISTANCE, HerdInstinctConfig.getPanicDurationTicks(), currentGameTime);
            if (animal.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY)) {
                animal.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
            }
        }
    }

    private static boolean shouldAffectAnimal(Animal victim, Animal candidate) {
        if (HerdInstinctConfig.get().panicTargetMode == HerdInstinctConfig.PanicTargetMode.ALL) {
            return true;
        }

        return HerdGroupUtil.isSameSpecies(victim, candidate);
    }

    private static Vec3 resolveThreatPosition(DamageSource source, Animal victim) {
        Entity directEntity = source.getDirectEntity();
        if (directEntity != null) {
            return directEntity.position();
        }

        Entity causingEntity = source.getEntity();
        if (causingEntity != null) {
            return causingEntity.position();
        }

        return victim.position();
    }
}
