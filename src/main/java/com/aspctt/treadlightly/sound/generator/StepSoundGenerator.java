// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

/**
 * Watches one entity and decides when it has taken a step. Driven per frame, not per tick, so a
 * footfall is heard when it happens rather than at the next tick boundary.
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
