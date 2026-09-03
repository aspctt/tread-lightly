// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Set;

/**
 * The stages of a block map lookup.
 * <p>
 * A block map entry may be qualified with a substrate, written {@code block.substrate=sounds},
 * which says the entry only applies when the solver is asking that particular question. The
 * string values are the pack format and cannot change without breaking every existing pack.
 */
public interface Substrates {
    /** An unqualified entry: what the block sounds like when stepped on directly. */
    String DEFAULT = "";
    /** A thin block resting on top of another, which the foot lands on instead. */
    String CARPET = "carpet";
    /** Layered over the dry sound when the block is wet, waterlogged, or rained on. */
    String WET = "wet";
    /** A block below the one underfoot, reached when standing on a fence or wall. */
    String FENCE = "bigger";
    /** Layered over the step when plants brush against the legs. */
    String FOLIAGE = "foliage";
    /** Marks ground that foliage is allowed to rustle against. */
    String MESSY = "messy";

    /**
     * Substrates that only ever layer on top of a real step sound. They are never resolved
     * through the primitive map, because a block's vanilla sound type says nothing about
     * whether it is wet or has plants on it.
     */
    Set<String> SUPPLEMENTARY_SUBSTRATES = Set.of(WET, FOLIAGE, MESSY);

    static boolean isDefault(String substrate) {
        return DEFAULT.equals(substrate);
    }

    static boolean isSupplementary(String substrate) {
        return SUPPLEMENTARY_SUBSTRATES.contains(substrate);
    }
}
