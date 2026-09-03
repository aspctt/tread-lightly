// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.util;

import net.minecraft.util.Mth;

/**
 * A value that eases towards a target at a fixed rate, and can be read part way between ticks.
 * <p>
 * Used for the biome volume and pitch trim, so that crossing a border shifts the sound over a
 * moment rather than snapping mid-stride.
 */
public class Lerp {
    private float previous;
    private float current;

    /**
     * Whether a target has ever been supplied.
     * <p>
     * Without this the value starts at zero and creeps towards its first target at the easing
     * rate, so footsteps faded in from silence over the first second or so every time an entity
     * came into range. The first sample is what the value should already be, not something to
     * ease towards.
     */
    private boolean started;

    public void update(float target, float rate) {
        if (!started) {
            started = true;
            previous = target;
            current = target;
            return;
        }

        previous = current;
        if (current < target) {
            current = Math.min(current + rate, target);
        }
        if (current > target) {
            current = Math.max(current - rate, target);
        }
    }

    public float get(float tickDelta) {
        return Mth.lerp(tickDelta, previous, current);
    }
}
