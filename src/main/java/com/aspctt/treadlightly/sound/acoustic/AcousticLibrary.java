// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.world.Association;
import com.aspctt.treadlightly.world.SoundsKey;

/** Every acoustic a pack defined, by name, and the means to play them. */
public interface AcousticLibrary {
    void addAcoustic(String name, Acoustic acoustic);

    /** Plays what the solver decided a foot landed on, including its wet and foliage layers. */
    void playStep(Association association, State event, Options options);

    /** Plays acoustics by name, for sounds the solver is not involved in. */
    void playAcoustic(LivingEntity location, SoundsKey sounds, State event, Options options);

    /** Called once a frame, to release any sound whose delay has elapsed. */
    void think();
}
