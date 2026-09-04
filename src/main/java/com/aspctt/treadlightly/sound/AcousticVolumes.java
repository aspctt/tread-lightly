// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

/**
 * The two layer volumes the acoustic player needs from configuration.
 * <p>
 * Narrow on purpose, and handed in rather than reached for: a value type that calls out to a
 * global to answer a question cannot be built in a test, and ties the acoustics to a running
 * game. Values are read at playback rather than captured, so a change in the config screen is
 * heard on the next step without anything being rebuilt.
 */
public interface AcousticVolumes {
    /** Everything at full volume, for tests and for running without a config. */
    AcousticVolumes FULL = new AcousticVolumes() {
        @Override
        public float wet() {
            return 1;
        }

        @Override
        public float foliage() {
            return 1;
        }
    };

    /** Multiplier for the wet layer, 0 to 1. */
    float wet();

    /** Multiplier for the foliage layer, 0 to 1. */
    float foliage();
}
