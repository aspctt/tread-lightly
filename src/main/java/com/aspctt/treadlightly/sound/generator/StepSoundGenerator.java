// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

/**
 * Watches one entity and decides when it has taken a step.
 * <p>
 * Driven once a frame rather than once a tick, because the point is to catch the moment a foot
 * lands rather than to hear it at the next tick boundary.
 */
public interface StepSoundGenerator {
    /** Biome pitch trim, part way between the last two frames. */
    float getLocalPitch(float tickDelta);

    /** Biome volume trim, part way between the last two frames. */
    float getLocalVolume(float tickDelta);

    /** Where the entity's speed and distance travelled are worked out. */
    MotionTracker getMotionTracker();

    /** Advances the simulation and plays whatever it decides has happened. */
    void generateFootsteps();
}
