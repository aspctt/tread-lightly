// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import net.minecraft.resources.ResourceLocation;

/**
 * A segment keyed by a plain identifier, for the maps whose keys need no state matching.
 * <p>
 * Entries are written {@code id} or {@code id@substrate}. Compare {@link StateLookup}, which
 * has to deal with block state properties, tags, and wildcards.
 *
 * @param <T> what is being looked up, reduced to an identifier by {@link #getId(Object)}
 */
abstract class AbstractSubstrateLookup<T> implements Lookup.DataSegment<T> {
    private final Map<String, Map<ResourceLocation, Optional<SoundsKey>>> substrates = new Object2ObjectLinkedOpenHashMap<>();

    protected AbstractSubstrateLookup(JsonObject json) {
        json.entrySet().forEach(entry -> {
            final String[] split = entry.getKey().trim().split("@");
            final String primitive = split[0];
            final String substrate = split.length > 1 ? split[1] : Substrates.DEFAULT;

            substrates
                .computeIfAbsent(substrate, s -> new Object2ObjectLinkedOpenHashMap<>())
                .put(ResourceLocation.parse(primitive), Optional.of(SoundsKey.of(entry.getValue().getAsString())));
        });
    }

    protected abstract ResourceLocation getId(T key);

    @Override
    public Optional<SoundsKey> getAssociation(@Nullable T key, String substrate) {
        if (key == null) {
            return Optional.empty();
        }
        final ResourceLocation id = getId(key);
        return getSubstrateMap(id, substrate).getOrDefault(id, Optional.empty());
    }

    /**
     * Picks the map to search for a given substrate, falling back through the break sound
     * variant to the unqualified entries.
     */
    protected Map<ResourceLocation, Optional<SoundsKey>> getSubstrateMap(ResourceLocation id, String substrate) {
        Map<ResourceLocation, Optional<SoundsKey>> primitives = substrates.get(substrate);
        if (primitives != null) {
            return primitives;
        }

        primitives = substrates.get("break_" + id.getPath());
        if (primitives != null) {
            return primitives;
        }

        return substrates.getOrDefault(Substrates.DEFAULT, Map.of());
    }

    @Override
    public Set<String> getSubstrates() {
        return substrates.keySet();
    }

    @Override
    public boolean contains(T key) {
        final ResourceLocation primitive = getId(key);

        for (var primitives : substrates.values()) {
            if (primitives.containsKey(primitive)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(T key, String substrate) {
        Map<ResourceLocation, Optional<SoundsKey>> primitives = substrates.get(substrate);
        return primitives != null && primitives.containsKey(getId(key));
    }

    @Override
    public boolean isEmpty() {
        return substrates.isEmpty();
    }
}
