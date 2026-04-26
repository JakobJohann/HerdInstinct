package jakobify.herdinstinct;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class HerdInstinctClient {
    private HerdInstinctClient() {
    }

    public static void registerConfigScreen() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (client, parent) -> HerdInstinctConfigScreen.create(parent)
        );
    }
}
