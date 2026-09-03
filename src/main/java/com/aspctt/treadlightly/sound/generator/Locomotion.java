// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * How an entity gets about, which decides the shape of its gait.
 * <p>
 * The original also carried two flying stances, which existed for Mine Little Pony's pegasi.
 * No vanilla entity ever used them: every flying mob is mapped to {@link #NONE}, so they and
 * the winged generator behind them are gone.
 */
public enum Locomotion {
    /** Makes no footsteps at all. Fish, bats, slimes, anything that does not walk. */
    NONE,
    /** Two legs, evenly spaced strides. */
    BIPED((entity, context) -> new TerrestrialStepSoundGenerator(entity, context, new Modifier<>())),
    /** Four legs, hooves falling in uneven pairs. */
    QUADRUPED((entity, context) -> new TerrestrialStepSoundGenerator(entity, context, new QuadrupedModifier()));

    private static final Map<String, Locomotion> REGISTRY = new Object2ObjectOpenHashMap<>();

    static {
        for (Locomotion value : values()) {
            REGISTRY.put(value.name(), value);
            // Older packs wrote the ordinal rather than the name.
            REGISTRY.put(String.valueOf(value.ordinal()), value);
        }
    }

    private static final String AUTO_TRANSLATION_KEY = "menu.treadlightly.stance.auto";

    private final BiFunction<LivingEntity, StepSoundContext, Optional<StepSoundGenerator>> constructor;
    private final String translationKey = "menu.treadlightly.stance." + name().toLowerCase(Locale.ROOT);

    Locomotion() {
        this.constructor = (entity, context) -> Optional.empty();
    }

    Locomotion(BiFunction<LivingEntity, StepSoundContext, StepSoundGenerator> generator) {
        this.constructor = (entity, context) -> Optional.of(generator.apply(entity, context));
    }

    public Optional<StepSoundGenerator> supplyGenerator(LivingEntity entity, StepSoundContext context) {
        return constructor.apply(entity, context);
    }

    public Component getOptionName() {
        return Component.translatable("menu.treadlightly.stance",
                Component.translatable(this == NONE ? AUTO_TRANSLATION_KEY : translationKey));
    }

    public Component getOptionTooltip() {
        return Component.translatable(translationKey + ".tooltip");
    }

    /** Unknown names fall back to two legs, which is right more often than silence. */
    public static Locomotion byName(String name) {
        return REGISTRY.getOrDefault(name, BIPED);
    }

    /**
     * Players are whatever they chose in the config, and {@link #NONE} there means "decide for
     * me" rather than "no sound", since a player who walks should always be heard.
     */
    public static Locomotion forPlayer(Player player, Locomotion preference) {
        return preference == NONE ? BIPED : preference;
    }
}
