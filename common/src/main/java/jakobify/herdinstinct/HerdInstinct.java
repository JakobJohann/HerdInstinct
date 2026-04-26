package jakobify.herdinstinct;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public final class HerdInstinct {
    public static final String MODID = "herdinstinct";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;

    private HerdInstinct() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        HerdInstinctConfig.HANDLER.load();
        LOGGER.info("Herd Instinct initialized");
    }
}
