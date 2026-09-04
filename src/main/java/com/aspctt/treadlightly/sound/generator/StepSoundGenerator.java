// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

/**
 * Watches one entity and decides when it has taken a step. Driven once per client tick; the
 * tick delta passed to the trim accessors is for the moment a sound actually plays.
 */
public interface StepSoundGenerator {
    /** Biome pitch trim, part way between the last two frames. */
    float getLocalPitch(float tickDelta);

    /** Biome volume trim, part way between the last two frames. */
    float getLocalVolume(float tickDelta);

    MotionTracker getMotionTracker();

    /** Advances the simulation and plays anything it decides has happened. */
    void generateFootsteps();
}
