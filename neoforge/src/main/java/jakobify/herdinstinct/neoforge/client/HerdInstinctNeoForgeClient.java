package jakobify.herdinstinct.neoforge.client;

import jakobify.herdinstinct.HerdInstinctConfigScreen;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class HerdInstinctNeoForgeClient {
    private HerdInstinctNeoForgeClient() {
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (client, parent) -> HerdInstinctConfigScreen.create(parent)
        );
    }
}
