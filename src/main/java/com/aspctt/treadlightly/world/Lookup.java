// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * A stack of block map segments, queried as one.
 * <p>
 * Each enabled resource pack contributes a segment. Later packs win, so the segments are
 * searched in reverse pack order and the first one holding an entry for a key decides the
 * answer. That is what lets a pack override a single block without restating the rest.
 *
 * @param <T> what is being looked up: a block state, a sound event, an entity type
 */
public final class Lookup<T> {
    private DataSegment<T> data = UnionDataSegment.empty();

    /** Loads segments layered on top of a parent lookup, used for per-entity block maps. */
    public boolean load(Stream<? extends DataSegment<T>> data, Lookup<T> parent) {
        return load(Stream.of(parent.data, UnionDataSegment.of(data)));
    }

    public boolean load(Stream<? extends DataSegment<T>> data) {
        this.data = UnionDataSegment.of(data);
        return !this.data.isEmpty();
    }

    /**
     * Resolves a key and substrate to the acoustics that should play.
     *
     * @return {@link SoundsKey#UNASSIGNED} when no segment holds an entry, or
     *         {@code NON_EMITTER} when one does and declares silence
     */
    public SoundsKey getAssociation(T key, String substrate) {
        return data.getAssociation(key, substrate).orElse(SoundsKey.UNASSIGNED);
    }

    /** Every substrate any loaded segment holds entries for. */
    public Set<String> getSubstrates() {
        return data.getSubstrates();
    }

    /**
     * Every substrate that resolves for this key, with what it resolves to. For the debug
     * readout rather than the sound path, so it allocates freely.
     */
    public Map<String, SoundsKey> getAssociations(T key) {
        final Object2ObjectOpenHashMap<String, SoundsKey> result = new Object2ObjectOpenHashMap<>();

        for (String substrate : getSubstrates()) {
            SoundsKey association = getAssociation(key, substrate);

            if (association.isResult()) {
                result.put(substrate, association);
            }
        }

        return Object2ObjectMaps.unmodifiable(result);
    }

    public boolean contains(T key) {
        return data.contains(key);
    }

    public boolean contains(T key, String substrate) {
        return data.contains(key, substrate);
    }

    /**
     * One pack's worth of entries. Implementations return an empty {@link Optional} to mean
     * "not mine, ask the next segment", which is distinct from returning a key that says
     * silence. The returned optionals are stored rather than built per call, so resolving a
     * footstep allocates nothing here.
     */
    public interface DataSegment<T> {
        Optional<SoundsKey> getAssociation(T key, String substrate);

        Set<String> getSubstrates();

        boolean contains(T key);

        boolean contains(T key, String substrate);

        boolean isEmpty();
    }

    record UnionDataSegment<T>(List<? extends DataSegment<T>> entries, Set<String> substrates) implements DataSegment<T> {
        private static final UnionDataSegment<?> EMPTY = new UnionDataSegment<>(List.of(), Set.of());

        @SuppressWarnings("unchecked")
        static <T> DataSegment<T> empty() {
            return (UnionDataSegment<T>) EMPTY;
        }

        static <T> DataSegment<T> of(Stream<? extends DataSegment<T>> entries) {
            // Reversed so the last pack loaded is consulted first. Copied rather than left as
            // a reversed view, so indexing in getAssociation does not go through a translation
            // on every lookup.
            var data = List.copyOf(entries.filter(i -> !i.isEmpty()).toList().reversed());
            if (data.isEmpty()) {
                return empty();
            }
            // Collapse a single segment rather than paying for a wrapper on every lookup.
            if (data.size() == 1) {
                return data.getFirst();
            }
            var substrates = data.stream()
                    .flatMap(i -> i.getSubstrates().stream())
                    .collect(Collectors.toUnmodifiableSet());

            return new UnionDataSegment<>(data, substrates);
        }

        @Override
        public Optional<SoundsKey> getAssociation(T key, String substrate) {
            for (int i = 0; i < entries.size(); i++) {
                Optional<SoundsKey> found = entries.get(i).getAssociation(key, substrate);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }

        @Override
        public Set<String> getSubstrates() {
            return substrates;
        }

        @Override
        public boolean contains(T key) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).contains(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(T key, String substrate) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).contains(key, substrate)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return entries.isEmpty();
        }
    }
}
