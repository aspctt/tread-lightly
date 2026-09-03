// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

/**
 * Loose named parameters threaded through the acoustic tree down to the sound player.
 * <p>
 * Acoustics are composed at load time from pack data and cannot know what the caller will want
 * to say about a particular playback, so values like a fade volume or a delay window ride along
 * beside the call rather than sitting in the acoustic. Implementations are built once and read
 * lazily, so passing them costs nothing per footstep.
 */
public interface Options {
    Options EMPTY = new Options() {
        @Override
        public boolean containsKey(String option) {
            return false;
        }

        @Override
        public float get(String option) {
            return 0;
        }
    };

    static Options singular(String key, float value) {
        return ofGetter(key, () -> value);
    }

    /** A single option whose value is read at playback rather than captured now. */
    static Options ofGetter(String key, FloatSupplier value) {
        return new Options() {
            @Override
            public boolean containsKey(String option) {
                return key.equals(option);
            }

            @Override
            public float get(String option) {
                return containsKey(option) ? value.get() : 0;
            }
        };
    }

    boolean containsKey(String option);

    float get(String option);

    default float getOrDefault(String option, float defaultValue) {
        return containsKey(option) ? get(option) : defaultValue;
    }

    /** This set with {@code other} layered over it, so other's values win on collision. */
    default Options and(Options other) {
        final Options self = this;
        if (self == EMPTY) {
            return other;
        }
        if (other == EMPTY) {
            return self;
        }
        return new Options() {
            @Override
            public boolean containsKey(String o) {
                return other.containsKey(o) || self.containsKey(o);
            }

            @Override
            public float get(String o) {
                return other.containsKey(o) ? other.get(o) : self.get(o);
            }
        };
    }

    interface FloatSupplier {
        float get();
    }
}
