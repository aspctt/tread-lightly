// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.client;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.aspctt.treadlightly.config.EntitySelector;
import com.aspctt.treadlightly.config.TreadLightlyConfig;
import com.aspctt.treadlightly.sound.generator.Locomotion;

/**
 * Builds the config screen.
 * <p>
 * Kept in its own class so that the classes it touches are only loaded when the screen is
 * actually opened. Nothing else in the mod refers to it directly, so an installation without
 * YetAnotherConfigLib never resolves any of these types.
 */
public final class ConfigScreens {
    private ConfigScreens() {
    }

    public static Screen create(TreadLightlyConfig config, Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("menu.treadlightly.title"))
                .category(volumes(config))
                .category(behaviour(config))
                .save(config::save)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory volumes(TreadLightlyConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("menu.treadlightly.category.volume"))
                .option(percentage("volume", 0, () -> config.volume, v -> config.volume = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("menu.treadlightly.group.sources"))
                        .option(percentage("client_player", 100, () -> config.clientPlayerVolume, v -> config.clientPlayerVolume = v))
                        .option(percentage("other_player", 100, () -> config.otherPlayerVolume, v -> config.otherPlayerVolume = v))
                        .option(percentage("hostile", 100, () -> config.hostileEntitiesVolume, v -> config.hostileEntitiesVolume = v))
                        .option(percentage("passive", 100, () -> config.passiveEntitiesVolume, v -> config.passiveEntitiesVolume = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("menu.treadlightly.group.layers"))
                        .option(percentage("wet", 50, () -> config.wetSoundsVolume, v -> config.wetSoundsVolume = v))
                        .option(percentage("foliage", 100, () -> config.foliageSoundsVolume, v -> config.foliageSoundsVolume = v))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("menu.treadlightly.option.running"))
                                .description(description("running"))
                                .binding(0, () -> config.runningVolumeIncrease, v -> config.runningVolumeIncrease = v)
                                .controller(option -> IntegerSliderControllerBuilder.create(option)
                                        .range(-100, 100).step(5)
                                        .formatValue(v -> Component.literal(v + "%")))
                                .build())
                        .build())
                .build();
    }

    private static ConfigCategory behaviour(TreadLightlyConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("menu.treadlightly.category.behaviour"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("menu.treadlightly.option.enabled"))
                        .description(description("enabled"))
                        .binding(true, () -> !config.disabled, v -> config.setDisabled(!v))
                        .controller(option -> BooleanControllerBuilder.create(option).onOffFormatter())
                        .build())
                .option(toggle(config, "multiplayer", true, () -> config.multiplayer, v -> config.multiplayer = v))
                .option(toggle(config, "exclusive", true, () -> config.exclusive, v -> config.exclusive = v))
                .option(toggle(config, "footwear", true, () -> config.footwear, v -> config.footwear = v))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("menu.treadlightly.group.entities"))
                        .option(Option.<EntitySelector>createBuilder()
                                .name(Component.translatable("menu.treadlightly.option.targets"))
                                .description(description("targets"))
                                .binding(EntitySelector.ALL, config::getEntitySelector, v -> config.targetEntities = v)
                                .controller(option -> EnumControllerBuilder.create(option)
                                        .enumClass(EntitySelector.class)
                                        .formatValue(v -> Component.translatable(
                                                "menu.treadlightly.targets." + v.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("menu.treadlightly.option.max_entities"))
                                .description(description("max_entities"))
                                .binding(50, () -> config.maxSteppingEntities, v -> config.maxSteppingEntities = v)
                                .controller(option -> IntegerSliderControllerBuilder.create(option)
                                        .range(1, 200).step(1))
                                .build())
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("menu.treadlightly.group.stance"))
                        .option(Option.<Locomotion>createBuilder()
                                .name(Component.translatable("menu.treadlightly.option.stance"))
                                .description(description("stance"))
                                .binding(Locomotion.NONE, config::getStance, v -> config.stance = v)
                                .controller(option -> EnumControllerBuilder.create(option)
                                        .enumClass(Locomotion.class)
                                        .formatValue(Locomotion::getOptionName))
                                .build())
                        .build())
                .build();
    }

    /** A whole-percentage slider, which is how every volume in the config is stored. */
    private static Option<Integer> percentage(String key, int fallback, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable("menu.treadlightly.option." + key))
                .description(description(key))
                .binding(fallback, getter, setter)
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(0, 100).step(5)
                        .formatValue(v -> Component.literal(v + "%")))
                .build();
    }

    private static Option<Boolean> toggle(TreadLightlyConfig config, String key, boolean fallback,
                                          Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("menu.treadlightly.option." + key))
                .description(description(key))
                .binding(fallback, getter, setter)
                .controller(option -> BooleanControllerBuilder.create(option).onOffFormatter())
                .build();
    }

    private static OptionDescription description(String key) {
        return OptionDescription.of(Component.translatable("menu.treadlightly.option." + key + ".tooltip"));
    }
}
