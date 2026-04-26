package jakobify.herdinstinct.neoforge;

import jakobify.herdinstinct.HerdInstinct;
import jakobify.herdinstinct.HerdPanicEvents;
import jakobify.herdinstinct.neoforge.client.HerdInstinctNeoForgeClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(HerdInstinct.MODID)
public final class HerdInstinctNeoForge {
    public HerdInstinctNeoForge(IEventBus modEventBus) {
        HerdInstinct.init();

        NeoForge.EVENT_BUS.addListener(HerdInstinctNeoForge::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(HerdInstinctNeoForge::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(HerdInstinctNeoForge::onServerTick);

        if (FMLEnvironment.dist.isClient()) {
            HerdInstinctNeoForgeClient.registerConfigScreen();
        }
    }

    private static void onLivingDamagePost(LivingDamageEvent.Post event) {
        HerdPanicEvents.onAnimalDamaged(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        HerdPanicEvents.onAnimalDeath(event.getEntity(), event.getSource());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        HerdPanicEvents.onServerTick(event.getServer());
    }
}
