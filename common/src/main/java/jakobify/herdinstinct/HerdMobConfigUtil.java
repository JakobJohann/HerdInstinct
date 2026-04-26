package jakobify.herdinstinct;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.EntityType;

public final class HerdMobConfigUtil {
    private static final List<String> DEFAULT_INCLUDED_MOBS = List.of(
            "minecraft:armadillo",
            "minecraft:camel",
            "minecraft:chicken",
            "minecraft:cow",
            "minecraft:donkey",
            "minecraft:frog",
            "minecraft:goat",
            "minecraft:horse",
            "minecraft:llama",
            "minecraft:mooshroom",
            "minecraft:mule",
            "minecraft:pig",
            "minecraft:rabbit",
            "minecraft:sheep",
            "minecraft:sniffer",
            "minecraft:trader_llama",
            "minecraft:turtle"
    );

    private HerdMobConfigUtil() {
    }

    public static List<String> defaultIncludedMobIds() {
        return new ArrayList<>(DEFAULT_INCLUDED_MOBS);
    }

    public static List<String> normalizeIncludedMobIds(List<String> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            return defaultIncludedMobIds();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String rawId : rawIds) {
            if (rawId == null || rawId.isBlank()) {
                continue;
            }

            Identifier parsedId = Identifier.tryParse(rawId.trim().toLowerCase(Locale.ROOT));
            if (parsedId == null) {
                continue;
            }

            normalized.add(parsedId.toString());
        }

        if (normalized.isEmpty()) {
            return defaultIncludedMobIds();
        }

        return new ArrayList<>(normalized);
    }

    public static List<String> getSelectableMobIds(boolean affectModdedAnimals, List<String> currentIds) {
        Set<String> selectableIds = new LinkedHashSet<>(DEFAULT_INCLUDED_MOBS);
        selectableIds.addAll(normalizeIncludedMobIds(currentIds));

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            if (!Animal.class.isAssignableFrom(entityType.getBaseClass())) {
                continue;
            }

            Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (entityId == null) {
                continue;
            }
            if (!affectModdedAnimals && !"minecraft".equals(entityId.getNamespace())) {
                continue;
            }
            selectableIds.add(entityId.toString());
        }

        return selectableIds.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public static boolean isIncluded(Animal animal) {
        Identifier entityId = EntityType.getKey(animal.getType());
        if (entityId == null) {
            return false;
        }

        return normalizeIncludedMobIds(HerdInstinctConfig.get().includedMobs).contains(entityId.toString());
    }
}
