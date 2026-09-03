// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/**
 * The primitive map: the fallback that gives a block a sound when nothing in the block map
 * names it, by going on the vanilla sound type it already has.
 * <p>
 * This is what lets modded blocks sound right without anyone writing an entry for them. A
 * modded stone sounds like stone because it declares vanilla's stone sound type.
 */
public class PrimitiveLookup extends AbstractSubstrateLookup<SoundEvent> {
    /**
     * Substrate strings by sound type.
     * <p>
     * The substrate is derived from the sound type's volume and pitch, which is how two
     * different materials sharing one step sound stay distinguishable. Formatting it costs far
     * more than the lookup it feeds, and this runs for every footstep over an unmapped block,
     * so each sound type is formatted once and remembered. Concurrent because resource reload
     * populates lookups off the client thread.
     */
    private static final Map<SoundType, String> SUBSTRATE_CACHE = new ConcurrentHashMap<>();

    public PrimitiveLookup(JsonObject json) {
        super(json);
    }

    @Override
    protected ResourceLocation getId(SoundEvent key) {
        return key.getLocation();
    }

    public static String getSubstrate(SoundType type) {
        return SUBSTRATE_CACHE.computeIfAbsent(type,
                t -> String.format(Locale.ENGLISH, "%.2f_%.2f", t.getVolume(), t.getPitch()));
    }
}
