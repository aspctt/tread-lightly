// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.stream.Stream;

/**
 * The acoustic names a block map entry resolves to.
 * <p>
 * A single entry may name several acoustics, comma separated, all of which play together.
 * Three sentinel values carry meaning beyond the names themselves, and let the solver tell
 * "no entry exists here" apart from "an entry exists and says stay silent". They are interned
 * by {@link #of(String)}, so identity comparison against them is safe and is what
 * {@link #isResult()} and {@link #isSilent()} rely on.
 *
 * @param raw   the entry exactly as the pack wrote it, and the basis of equality
 * @param names the individual acoustic names, held as an array rather than a list so that
 *              playing a sound allocates no iterator on a path that runs for every footstep
 *              of every nearby entity. Callers must not modify it.
 */
public record SoundsKey(String raw, String[] names) {
    /** No entry in the block map. The solver keeps looking. */
    public static final SoundsKey UNASSIGNED = new SoundsKey("UNASSIGNED", new String[0]);
    /** An entry exists and declares the block silent. The solver stops looking. */
    static final SoundsKey NON_EMITTER = new SoundsKey("NOT_EMITTER", new String[0]);
    /** Ground that foliage above it is allowed to rustle against. */
    static final SoundsKey MESSY_GROUND = new SoundsKey("MESSY_GROUND", new String[0]);

    public static final SoundsKey SWIM_WATER = of("_SWIM_WATER");
    public static final SoundsKey SWIM_LAVA = of("_SWIM_LAVA");
    public static final SoundsKey WATERFINE = of("waterfine");
    public static final SoundsKey LAVAFINE = of("lavafine");

    public static SoundsKey of(String names) {
        if (MESSY_GROUND.raw.equals(names)) {
            return MESSY_GROUND;
        }
        if (UNASSIGNED.raw.equals(names)) {
            return UNASSIGNED;
        }
        if (NON_EMITTER.raw.equals(names)) {
            return NON_EMITTER;
        }
        return new SoundsKey(names);
    }

    SoundsKey(String names) {
        this(names, Stream.of(names.split(","))
                .filter(s -> !s.isEmpty())
                .distinct()
                .toArray(String[]::new));
    }

    /** True if any entry was found at all, sentinel or otherwise. */
    public boolean isResult() {
        return this != UNASSIGNED;
    }

    /** True if an entry was found and it declares silence. */
    public boolean isSilent() {
        return this == NON_EMITTER;
    }

    public boolean isEmitter() {
        return !isSilent();
    }

    /**
     * Equality is on {@link #raw()} alone. {@code names} is derived from it, and a record's
     * generated equality would compare the array by reference, which silently makes two keys
     * built from the same string unequal. The solver compares the keys resolved for each foot
     * to decide whether a two-footed sound plays once or twice, so that would be a real bug.
     */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof SoundsKey other && raw.equals(other.raw));
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }
}
