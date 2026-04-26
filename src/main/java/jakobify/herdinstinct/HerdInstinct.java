package jakobify.herdinstinct;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(HerdInstinct.MODID)
public class HerdInstinct {
    public static final String MODID = "herdinstinct";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HerdInstinct(IEventBus modEventBus) {
        HerdInstinctConfig.HANDLER.load();
        HerdPanicEvents.register();
        if (FMLEnvironment.dist.isClient()) {
            HerdInstinctClient.registerConfigScreen();
        }
        LOGGER.info("HerdInstinct loaded");
    }
}
