// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.util;

import java.util.Random;

import net.minecraft.util.Mth;

public interface MathUtil {
    /** A random float in [a, b], or a when the range is empty or inverted. */
    static float randAB(Random rng, float a, float b) {
        return a >= b ? a : a + rng.nextFloat() * (b - a);
    }

    /**
     * A random long in [a, b], or a when the range is empty or inverted.
     * <p>
     * Bounded by the width of the range rather than by {@code b}, so a delay declared as 100 to
     * 300 stays inside that window instead of reaching 400.
     */
    static long randAB(Random rng, long a, long b) {
        return a >= b ? a : a + rng.nextInt((int) Math.min(b - a + 1, Integer.MAX_VALUE));
    }

    /** Linear interpolation from {@code from} to {@code to}. */
    static float between(float from, float to, float value) {
        return from + (to - from) * value;
    }

    /** Where {@code number} falls between min and max, clamped to [0, 1]. */
    static float scalex(float number, float min, float max) {
        return Mth.clamp((number - min) / (max - min), 0, 1);
    }
}
