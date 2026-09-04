// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.player;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.generator.StepSoundGenerator;

/**
 * What a sound player needs to know about the entity it is playing for.
 * <p>
 * Implemented by the engine. An interface so the players can be built and exercised without
 * one, and so nothing down here reaches back up through a global.
 */
public interface PlaybackSource {
    /** Combined volume for this entity, from the global, per-category and running settings. */
    float getVolumeFor(LivingEntity source);

    /** The entity's generator, for its biome trim. Null if it makes no footsteps. */
    @Nullable
    StepSoundGenerator getGeneratorFor(LivingEntity entity);
}
