// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

/**
 * What the engine needs from the user's configuration.
 * <p>
 * An interface rather than the config class itself, so the engine can be exercised without one
 * and so nothing in here reaches for a global. Values are read when they are used, so a change
 * in the config screen takes effect on the next step.
 */
public interface SoundSettings extends AcousticVolumes {
    /** Everything on, at full volume. For tests and for running before a config exists. */
    SoundSettings DEFAULTS = new SoundSettings() {
        @Override
        public float wet() {
            return 1;
        }

        @Override
        public float foliage() {
            return 1;
        }

        @Override
        public boolean footwearEnabled() {
            return true;
        }
    };

    /** Whether boots add their own sound on top of the surface underfoot. */
    boolean footwearEnabled();
}
