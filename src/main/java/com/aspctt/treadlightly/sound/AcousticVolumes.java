// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

/**
 * The two layer volumes the acoustic player needs from configuration. Read at playback rather
 * than captured, so a change in the config screen is heard on the next step.
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
