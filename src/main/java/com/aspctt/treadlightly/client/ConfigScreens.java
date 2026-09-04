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

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.aspctt.treadlightly.config.EntitySelector;
import com.aspctt.treadlightly.config.TreadLightlyConfig;
import com.aspctt.treadlightly.sound.generator.Locomotion;

/**
 * Builds the config screen.
 * <p>
 * Kept in its own class so the types it touches are only loaded when the screen is opened.
 * Nothing else in the mod refers to it, so an installation without the library never resolves
 * any of them.
 */
public final class ConfigScreens {
    private static final String ROOT = "menu.treadlightly.";

    private ConfigScreens() {
    }

    public static Screen create(TreadLightlyConfig config, Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable(ROOT + "title"))
                .category(volumes(config))
                .category(behaviour(config))
                .save(config::save)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory volumes(TreadLightlyConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(ROOT + "category.volume"))
                .tooltip(Component.translatable(ROOT + "category.volume.tooltip"))
                .group(group("sources")
                        .option(percentage("volume", 70, () -> config.volume, v -> config.volume = v))
                        .option(percentage("client_player", 100, () -> config.clientPlayerVolume, v -> config.clientPlayerVolume = v))
                        .option(percentage("other_player", 100, () -> config.otherPlayerVolume, v -> config.otherPlayerVolume = v))
                        .option(percentage("hostile", 100, () -> config.hostileEntitiesVolume, v -> config.hostileEntitiesVolume = v))
                        .option(percentage("passive", 100, () -> config.passiveEntitiesVolume, v -> config.passiveEntitiesVolume = v))
                        .build())
                .group(group("layers")
                        .option(percentage("wet", 50, () -> config.wetSoundsVolume, v -> config.wetSoundsVolume = v))
                        .option(percentage("foliage", 100, () -> config.foliageSoundsVolume, v -> config.foliageSoundsVolume = v))
                        .option(Option.<Integer>createBuilder()
                                .name(name("running"))
                                .description(describe("running"))
                                .binding(0, () -> config.runningVolumeIncrease, v -> config.runningVolumeIncrease = v)
                                .controller(option -> IntegerSliderControllerBuilder.create(option)
                                        .range(-100, 100).step(5)
                                        .formatValue(ConfigScreens::signedPercent))
                                .build())
                        .build())
                .build();
    }

    private static ConfigCategory behaviour(TreadLightlyConfig config) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(ROOT + "category.behaviour"))
                .tooltip(Component.translatable(ROOT + "category.behaviour.tooltip"))
                .group(group("general")
                        .option(Option.<Boolean>createBuilder()
                                .name(name("enabled"))
                                .description(describe("enabled"))
                                .binding(true, () -> !config.disabled, v -> config.setDisabled(!v))
                                .controller(ConfigScreens::onOff)
                                .build())
                        .option(toggle("multiplayer", true, () -> config.multiplayer, v -> config.multiplayer = v))
                        .option(toggle("exclusive", true, () -> config.exclusive, v -> config.exclusive = v))
                        .option(toggle("footwear", true, () -> config.footwear, v -> config.footwear = v))
                        .build())
                .group(group("entities")
                        .option(Option.<EntitySelector>createBuilder()
                                .name(name("targets"))
                                .description(describe("targets"))
                                .binding(EntitySelector.ALL, config::getEntitySelector, v -> config.targetEntities = v)
                                .controller(option -> EnumControllerBuilder.create(option)
                                        .enumClass(EntitySelector.class)
                                        .formatValue(v -> Component.translatable(ROOT + "targets." + lower(v.name()))))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(name("max_entities"))
                                .description(describe("max_entities"))
                                .binding(50, () -> config.maxSteppingEntities, v -> config.maxSteppingEntities = v)
                                .controller(option -> IntegerSliderControllerBuilder.create(option)
                                        .range(1, 200).step(1))
                                .build())
                        .build())
                // Collapsed: almost nobody changes their own gait, and leaving it open pushes
                // the settings people do use further down the page.
                .group(group("stance")
                        .collapsed(true)
                        .option(Option.<Locomotion>createBuilder()
                                .name(name("stance"))
                                .description(describe("stance"))
                                .binding(Locomotion.NONE, config::getStance, v -> config.stance = v)
                                .controller(option -> EnumControllerBuilder.create(option)
                                        .enumClass(Locomotion.class)
                                        .formatValue(v -> Component.translatable(ROOT + "stance." + lower(v.name()))))
                                .build())
                        .build())
                .build();
    }

    private static OptionGroup.Builder group(String key) {
        return OptionGroup.createBuilder()
                .name(Component.translatable(ROOT + "group." + key))
                .description(OptionDescription.of(Component.translatable(ROOT + "group." + key + ".tooltip")));
    }

    /** A whole-percentage slider, which is how every volume in the config is stored. */
    private static Option<Integer> percentage(String key, int fallback, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(name(key))
                .description(describe(key))
                .binding(fallback, getter, setter)
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(0, 100).step(5)
                        .formatValue(ConfigScreens::percent))
                .build();
    }

    private static Option<Boolean> toggle(String key, boolean fallback, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(name(key))
                .description(describe(key))
                .binding(fallback, getter, setter)
                .controller(ConfigScreens::onOff)
                .build();
    }

    /** Coloured so on and off read at a glance rather than needing the word. */
    private static BooleanControllerBuilder onOff(Option<Boolean> option) {
        return BooleanControllerBuilder.create(option).coloured(true).onOffFormatter();
    }

    private static Component name(String key) {
        return Component.translatable(ROOT + "option." + key);
    }

    /**
     * A description for the side panel: what the setting does, then a dimmed second paragraph
     * saying what it costs or when it matters. The panel is the best part of this library and
     * a single line leaves it looking empty.
     */
    private static OptionDescription describe(String key) {
        String base = ROOT + "option." + key;
        return OptionDescription.createBuilder()
                .text(Component.translatable(base + ".tooltip"))
                .text(Component.empty())
                .text(Component.translatable(base + ".detail").withStyle(ChatFormatting.GRAY))
                .build();
    }

    private static Component percent(int value) {
        return Component.literal(value + "%").withStyle(value == 0 ? ChatFormatting.GRAY : ChatFormatting.WHITE);
    }

    private static Component signedPercent(int value) {
        return Component.literal((value > 0 ? "+" : "") + value + "%")
                .withStyle(value == 0 ? ChatFormatting.GRAY : ChatFormatting.WHITE);
    }

    private static String lower(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
