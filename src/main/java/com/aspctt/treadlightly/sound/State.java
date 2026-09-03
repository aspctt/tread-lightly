// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * What an entity was doing when a sound was asked for.
 * <p>
 * A pack does not have to define every state. Where one is missing, playback falls back along
 * the chain each state declares, so a pack that only defines {@code walk} and {@code run} still
 * makes a noise going up stairs. The names are the pack format and cannot change.
 */
public enum State {
    /** Standing still. */
    STAND(null),
    /** Walking. */
    WALK(null),
    /** Turning on the spot. */
    WANDER(null),
    /** Swimming. */
    SWIM(null),
    /** Running. */
    RUN(WALK),
    /** Leaving the ground. */
    JUMP(WANDER),
    /** Landing after a fall, or jumping on the spot. */
    LAND(RUN),
    /** Climbing a ladder. */
    CLIMB(WALK),
    /** Climbing a ladder at a run. */
    CLIMB_RUN(RUN),
    /** Going down stairs. */
    DOWN(WALK),
    /** Going down stairs at a run. */
    DOWN_RUN(RUN),
    /** Going up stairs. */
    UP(WALK),
    /** Going up stairs at a run. */
    UP_RUN(RUN);

    private final State destination;
    private final String jsonName;

    State(@Nullable State fallback) {
        this.destination = fallback == null ? this : fallback;
        this.jsonName = name().toLowerCase(Locale.ROOT);
    }

    /** The name a pack writes for this state. */
    public String getName() {
        return jsonName;
    }

    public boolean canTransition() {
        return destination != this;
    }

    public State getTransitionDestination() {
        return destination;
    }

    /** States loud enough to be heard even when the entity is sneaking. */
    public boolean isExtraLoud() {
        return this == RUN || this == JUMP || destination == RUN;
    }
}
