// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.util;

import java.util.Random;

import com.google.gson.JsonObject;

import net.minecraft.util.GsonHelper;

import com.aspctt.treadlightly.sound.Options;

/**
 * A delay window in milliseconds.
 * <p>
 * It doubles as the {@link Options} carrying that window, so a delayed acoustic can hand its
 * own timing straight to the player without wrapping it in anything.
 */
public record Period(long min, long max) implements Options {
    public static final Period ZERO = new Period(0, 0);

    public static Period of(long value) {
        return of(value, value);
    }

    public static Period of(long min, long max) {
        return (min == max && max == 0) ? ZERO : new Period(min, max);
    }

    /** Reads either {@code key} as a single number, or {@code key_min} and {@code key_max}. */
    public static Period fromJson(JsonObject json, String key) {
        if (json.has(key)) {
            return of(json.get(key).getAsLong());
        }

        return of(
                GsonHelper.getAsLong(json, key + "_min", 0),
                GsonHelper.getAsLong(json, key + "_max", 0)
        );
    }

    public float random(Random rand) {
        return MathUtil.randAB(rand, min, max);
    }

    public float on(float value) {
        return MathUtil.between(min, max, value);
    }

    @Override
    public boolean containsKey(String option) {
        return "delay_min".equals(option)
            || "delay_max".equals(option);
    }

    @Override
    public float get(String option) {
        return "delay_min".equals(option) ? min
             : "delay_max".equals(option) ? max
             : 0;
    }
}
