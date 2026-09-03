// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.config;

import java.io.Reader;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.annotations.SerializedName;

/**
 * Tuning values the step generator works from, overridable by a pack.
 * <p>
 * These decide how far an entity walks between steps, how long it must stand still before it
 * counts as stopped, and how fast it must move to be running. A pack shipping unusually long or
 * short sounds can retune them rather than fighting the defaults.
 * <p>
 * Public mutable fields because Gson populates them by name, and anything a pack does not
 * mention keeps the value here.
 */
public class Variator {
    /** How long an entity must be still before it is treated as stopped, in milliseconds. */
    public int IMMOBILE_DURATION = 200;

    /**
     * Shortest gap between the idle shuffles of a standing entity, in milliseconds.
     * <p>
     * The original misspelled this and its partner below with one M. Packs were written against
     * that spelling, so both are accepted and the correct one is what gets written.
     */
    @SerializedName(value = "IMMOBILE_INTERVAL_MIN", alternate = "IMOBILE_INTERVAL_MIN")
    public int IMMOBILE_INTERVAL_MIN = 500;

    /** Longest gap between the idle shuffles of a standing entity, in milliseconds. */
    @SerializedName(value = "IMMOBILE_INTERVAL_MAX", alternate = "IMOBILE_INTERVAL_MAX")
    public int IMMOBILE_INTERVAL_MAX = 3000;

    /** Whether leaving and hitting the ground make their own sounds. */
    public boolean EVENT_ON_JUMP = true;

    /** How far an entity must fall for the landing to count as heavy. */
    public float LAND_HARD_DISTANCE_MIN = 0.9F;

    /** Below this speed a jump is a standing one, taken off both feet. */
    public float SPEED_TO_JUMP_AS_MULTIFOOT = 0.005F;

    /** Above this speed an entity is running rather than walking. */
    public float SPEED_TO_RUN = 0.022F;

    /**
     * Speed at which footsteps begin getting louder as an entity works up to a run.
     * <p>
     * The original declared 0.001 here and then overwrote this field and the one below with
     * hardcoded values on every single frame, so no pack could ever change either. These
     * defaults are the values that were actually in use.
     */
    public float RUNNING_RAMPUP_BEGIN = 0.011F;

    /** Speed at which footsteps stop getting louder, the entity now fully running. */
    public float RUNNING_RAMPUP_END = 0.022F;

    /** How far a normal entity walks between steps. */
    public float DISTANCE_HUMAN = 0.95F;

    /** How far between steps on stairs, shorter because the strides are. */
    public float DISTANCE_STAIR = 0.95F * 0.65F;

    /** How far between rungs on a ladder. */
    public float DISTANCE_LADDER = 0.5F;

    /** Whether turning on the spot makes a scuffing sound. */
    public boolean PLAY_WANDER = true;

    /**
     * Reads overrides from a pack into this instance, leaving anything unmentioned alone.
     * <p>
     * The instance creator is what makes Gson fill this object rather than build and discard a
     * new one, which is how several packs can each contribute a few values.
     */
    public void load(Reader reader) {
        new GsonBuilder()
                .registerTypeAdapter(Variator.class, (InstanceCreator<Variator>) type -> this)
                .create()
                .fromJson(reader, Variator.class);
    }
}
