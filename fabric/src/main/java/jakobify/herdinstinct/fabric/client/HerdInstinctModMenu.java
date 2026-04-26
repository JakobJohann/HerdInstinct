package jakobify.herdinstinct.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import jakobify.herdinstinct.HerdInstinctConfigScreen;

public final class HerdInstinctModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HerdInstinctConfigScreen::create;
    }
}
