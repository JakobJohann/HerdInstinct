package jakobify.herdinstinct;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

public final class HerdGroupUtil {
    public static final TagKey<EntityType<?>> HERD_ANIMALS_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(HerdInstinct.MODID, "herd_animals")
    );

    private HerdGroupUtil() {
    }

    public static boolean canTriggerPanic(Animal animal) {
        return isEligibleAnimal(animal);
    }

    public static boolean isEligibleAnimal(Animal animal) {
        Identifier entityId = EntityType.getKey(animal.getType());
        if (entityId == null) {
            HerdInstinct.LOGGER.info(
                    "HerdInstinct eligibility rejected: entity id missing for {}",
                    animal.getType()
            );
            return false;
        }

        if (!HerdInstinctConfig.get().affectModdedAnimals && !"minecraft".equals(entityId.getNamespace())) {
            return false;
        }

        return HerdMobConfigUtil.isIncluded(animal);
    }

    public static boolean isSameSpecies(Animal first, Animal second) {
        return first.getType() == second.getType();
    }
}
