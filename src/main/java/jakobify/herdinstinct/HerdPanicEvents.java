package jakobify.herdinstinct;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class HerdPanicEvents {
    public static final int HERD_RADIUS = 12;
    public static final int PANIC_COOLDOWN_TICKS = 80;
    public static final double PANIC_SPEED = 1.5D;
    public static final double PANIC_DISTANCE = 14.0D;

    private HerdPanicEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HerdPanicEvents::onPlayerAttackEntity);
        NeoForge.EVENT_BUS.addListener(HerdPanicEvents::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(HerdPanicEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(HerdPanicEvents::onServerTick);
    }

    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        if (HerdInstinctConfig.get().panicTriggerMode != HerdInstinctConfig.PanicTriggerMode.PLAYER_HIT) {
            return;
        }
        if (!(event.getTarget() instanceof Animal victim)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        HerdInstinct.LOGGER.info(
                "HerdInstinct player attack seen: victim={}, attacker={}, pos={}",
                victim.getType(),
                player.getType(),
                victim.blockPosition()
        );
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!HerdInstinctConfig.isEnabled() || !HerdGroupUtil.canTriggerPanic(victim)) {
            return;
        }
        long currentGameTime = serverLevel.getGameTime();
        if (HerdPanicUtil.isOnCooldown(victim, currentGameTime)) {
            return;
        }

        triggerPanic(victim, serverLevel, currentGameTime, player.position(), player);
    }

    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (HerdInstinctConfig.get().panicTriggerMode != HerdInstinctConfig.PanicTriggerMode.PLAYER_HIT) {
            return;
        }
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        if (!(event.getEntity() instanceof Animal victim)) {
            return;
        }
        HerdInstinct.LOGGER.info(
                "HerdInstinct damage post seen: victim={}, source={}, damage={}",
                victim.getType(),
                event.getSource().getMsgId(),
                event.getNewDamage()
        );
        if (!(victim.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!HerdInstinctConfig.isEnabled() || !HerdGroupUtil.canTriggerPanic(victim)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        long currentGameTime = serverLevel.getGameTime();
        if (HerdPanicUtil.isOnCooldown(victim, currentGameTime)) {
            return;
        }

        triggerPanic(victim, serverLevel, currentGameTime, resolveThreatPosition(event.getSource(), victim), player);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (HerdInstinctConfig.get().panicTriggerMode != HerdInstinctConfig.PanicTriggerMode.ON_DEATH) {
            return;
        }
        if (!(event.getEntity() instanceof Animal victim)) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!HerdInstinctConfig.isEnabled() || !HerdGroupUtil.canTriggerPanic(victim)) {
            return;
        }

        panicNearbyAnimals(victim, serverLevel, serverLevel.getGameTime(), resolveThreatPosition(event.getSource(), victim));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        HerdPanicUtil.tickActivePanics(event);
    }

    private static void triggerPanic(Animal victim, ServerLevel serverLevel, long currentGameTime, Vec3 threatPosition, Player player) {
        HerdInstinct.LOGGER.info(
                "HerdInstinct damage trigger: victim={}, attacker={}, pos={}",
                victim.getType(),
                player.getType(),
                victim.blockPosition()
        );
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

        HerdInstinct.LOGGER.info(
                "HerdInstinct herd search: victim={}, radius={}, matches={}",
                victim.getType(),
                herdRadius,
                nearbyAnimals.size()
        );
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
