// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.util;

import java.util.Random;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A volume or pitch window, stored as a multiplier although packs write it as a percentage.
 * <p>
 * A pack may give a single number, an object with {@code min} and {@code max}, or a pair of
 * {@code name_min} and {@code name_max} fields. Volume additionally accepts the older
 * {@code vol} spelling. All four forms are still read, because packs in the wild use all four.
 */
public record Range(float min, float max) {
    public static final Range DEFAULT = exactly(1);

    public static Range exactly(float value) {
        return new Range(value, value);
    }

    /** Reads a named range from the object, falling back to this range's bounds when absent. */
    public Range read(String name, JsonObject json) {
        if ("volume".equals(name) && (json.has("vol") || json.has("vol_min") || json.has("vol_max"))) {
            return read("vol", json);
        }
        if (json.has(name)) {
            JsonElement element = json.get(name);
            if (element.isJsonObject()) {
                return new Range(
                    getPercentage(element.getAsJsonObject(), "min", min),
                    getPercentage(element.getAsJsonObject(), "max", max)
                );
            }
            return exactly(getPercentage(json, name, min));
        }

        return new Range(
                getPercentage(json, name + "_min", min),
                getPercentage(json, name + "_max", max)
        );
    }

    /** A value drawn anywhere in the window. */
    public float random(Random rand) {
        return MathUtil.randAB(rand, min, max);
    }

    /** The point {@code value} of the way through the window, for a fade rather than a draw. */
    public float on(float value) {
        return MathUtil.between(min, max, value);
    }

    private static float getPercentage(JsonObject object, String param, float fallback) {
        if (!object.has(param)) {
            return fallback;
        }
        return object.get(param).getAsFloat() / 100F;
    }
}
