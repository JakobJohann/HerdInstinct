package jakobify.herdinstinct;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HerdInstinctConfigScreen {
    private HerdInstinctConfigScreen() {
    }

    public static Screen create(Screen parent) {
        HerdInstinctConfig defaults = HerdInstinctConfig.HANDLER.defaults();
        HerdInstinctConfig config = HerdInstinctConfig.get();
        config.includedMobs = HerdMobConfigUtil.normalizeIncludedMobIds(config.includedMobs);
        defaults.includedMobs = HerdMobConfigUtil.defaultIncludedMobIds();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Herd Instinct"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("General"))
                        .tooltip(Component.literal("Core behaviour and runtime settings for herd panic."))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Status"))
                                .description(OptionDescription.of(Component.literal("Global mod enablement and trigger selection.")))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Enabled"))
                                        .description(OptionDescription.of(Component.literal("Disables all Herd Instinct logic when turned off.")))
                                        .binding(defaults.enabled, () -> config.enabled, value -> config.enabled = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<HerdInstinctConfig.PanicTriggerMode>createBuilder()
                                        .name(Component.literal("Panic Trigger"))
                                        .description(OptionDescription.of(Component.literal("Choose whether panic starts on player hit, on death, or never.")))
                                        .binding(defaults.panicTriggerMode, () -> config.panicTriggerMode, value -> config.panicTriggerMode = value)
                                        .controller(option -> EnumControllerBuilder.create(option)
                                                .enumClass(HerdInstinctConfig.PanicTriggerMode.class))
                                        .build())
                                .option(Option.<HerdInstinctConfig.PanicTargetMode>createBuilder()
                                        .name(Component.literal("Panic Target Mode"))
                                        .description(OptionDescription.of(Component.literal("Choose whether panic affects only the same species or every configured herd mob in range.")))
                                        .binding(defaults.panicTargetMode, () -> config.panicTargetMode, value -> config.panicTargetMode = value)
                                        .controller(option -> EnumControllerBuilder.create(option)
                                                .enumClass(HerdInstinctConfig.PanicTargetMode.class))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Panic"))
                                .description(OptionDescription.of(Component.literal("Range and duration settings for the panic response.")))
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Herd Radius"))
                                        .description(OptionDescription.of(Component.literal("How many blocks around an injured animal are searched for herd panic targets.")))
                                        .binding(defaults.herdRadius, () -> config.herdRadius, value -> config.herdRadius = value)
                                        .controller(option -> IntegerSliderControllerBuilder.create(option)
                                                .range(4, 30)
                                                .step(1))
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Panic Duration"))
                                        .description(OptionDescription.of(Component.literal("How long animals remain in panic. Range: 1 to 15 seconds.")))
                                        .binding(defaults.panicDurationSeconds, () -> config.panicDurationSeconds, value -> config.panicDurationSeconds = value)
                                        .controller(option -> IntegerSliderControllerBuilder.create(option)
                                                .range(1, 15)
                                                .step(1))
                                        .build())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Compatibility"))
                        .tooltip(Component.literal("Mod compatibility and the list of passive mobs that may use herd panic."))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Compatibility"))
                                .description(OptionDescription.of(Component.literal("Use this to allow modded passive mobs from loaded registries to appear in the herd mob include list.")))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Include Modded Passive Mobs"))
                                        .description(OptionDescription.of(Component.literal("If enabled, modded passive Animal entities can be added to the include list.")))
                                        .binding(defaults.affectModdedAnimals, () -> config.affectModdedAnimals, value -> config.affectModdedAnimals = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .option(ListOption.<String>createBuilder(String.class)
                                .name(Component.literal("Included Passive Mobs"))
                                .description(OptionDescription.of(Component.literal("Only mobs in this list are affected by herd panic. Add to include, remove to exclude.")))
                                .binding(defaults.includedMobs, () -> config.includedMobs, value -> config.includedMobs = HerdMobConfigUtil.normalizeIncludedMobIds(value))
                                .controller(option -> DropdownStringControllerBuilder.create(option)
                                        .values(HerdMobConfigUtil.getSelectableMobIds(config.affectModdedAnimals, config.includedMobs))
                                        .allowAnyValue(false)
                                        .allowEmptyValue(false))
                                .initial(() -> firstSelectableMobId(config))
                                .build())
                        .build())
                .save(() -> {
                    config.includedMobs = HerdMobConfigUtil.normalizeIncludedMobIds(config.includedMobs);
                    HerdInstinctConfig.HANDLER.save();
                })
                .build()
                .generateScreen(parent);
    }

    private static String firstSelectableMobId(HerdInstinctConfig config) {
        return HerdMobConfigUtil.getSelectableMobIds(config.affectModdedAnimals, config.includedMobs).stream()
                .findFirst()
                .orElse("minecraft:cow");
    }
}
