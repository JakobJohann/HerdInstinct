package jakobify.herdinstinct.fabric;

import jakobify.herdinstinct.HerdInstinct;
import jakobify.herdinstinct.HerdPanicEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class HerdInstinctFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HerdInstinct.init();

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) ->
                HerdPanicEvents.onAnimalDamaged(entity, source, damageTaken));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damageAmount) -> {
            HerdPanicEvents.onAnimalDeath(entity, source);
            return true;
        });
        ServerTickEvents.END_SERVER_TICK.register(HerdPanicEvents::onServerTick);
    }
}
