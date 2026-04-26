package jakobify.herdinstinct;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.isxander.yacl3.api.NameableEnum;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class HerdInstinctConfig {
    public static final ConfigClassHandler<HerdInstinctConfig> HANDLER = ConfigClassHandler.createBuilder(HerdInstinctConfig.class)
            .id(Identifier.fromNamespaceAndPath(HerdInstinct.MODID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve(HerdInstinct.MODID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry(comment = "How far the herd panic search reaches from the damaged animal.")
    public int herdRadius = HerdPanicEvents.HERD_RADIUS;

    @SerialEntry(comment = "Whether the mod logic is globally enabled.")
    public boolean enabled = true;

    @SerialEntry(comment = "Controls when herd panic is triggered.")
    public PanicTriggerMode panicTriggerMode = PanicTriggerMode.PLAYER_HIT;

    @SerialEntry(comment = "Controls whether panic affects only the same species or every eligible herd animal in range.")
    public PanicTargetMode panicTargetMode = PanicTargetMode.ALL;

    @SerialEntry(comment = "How long animals stay in panic in seconds.")
    public int panicDurationSeconds = 5;

    @SerialEntry(comment = "Whether modded passive mobs may be included in the compatibility mob list.")
    public boolean affectModdedAnimals = true;

    @SerialEntry(comment = "Only entity ids in this list are affected by herd panic.")
    public List<String> includedMobs = new ArrayList<>(HerdMobConfigUtil.defaultIncludedMobIds());

    public static HerdInstinctConfig get() {
        return HANDLER.instance();
    }

    public static boolean isEnabled() {
        return get().enabled;
    }

    public static int getHerdRadius() {
        return Math.max(1, get().herdRadius);
    }

    public static int getPanicDurationTicks() {
        return clamp(get().panicDurationSeconds, 1, 15) * 20;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum PanicTriggerMode implements NameableEnum {
        PLAYER_HIT,
        ON_DEATH,
        NEVER;

        @Override
        public Component getDisplayName() {
            return Component.literal(name().replace('_', ' ').toLowerCase(Locale.ROOT));
        }
    }

    public enum PanicTargetMode implements NameableEnum {
        SAME_SPECIES,
        ALL;

        @Override
        public Component getDisplayName() {
            return Component.literal(name().replace('_', ' ').toLowerCase(Locale.ROOT));
        }
    }
}
