// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import com.aspctt.treadlightly.TreadLightly;

/**
 * The block map: one pack's worth of block state to acoustic mappings.
 * <p>
 * An entry key may name a block outright, a block tag with {@code #}, or every block with
 * {@code *}. It may constrain block state properties in brackets, and may be qualified with a
 * substrate after a dot. So {@code minecraft:oak_slab[type=top].carpet=wood} is legal, and so
 * is {@code #minecraft:logs=wood}.
 * <p>
 * Lookups are cached per block state, because the same few blocks are queried every frame while
 * the set of entries never changes between reloads.
 */
public record StateLookup(Map<String, Bucket> substrates) implements Lookup.DataSegment<BlockState> {

    public StateLookup() {
        this(new Object2ObjectLinkedOpenHashMap<>());
    }

    public StateLookup(JsonObject json) {
        this(new Object2ObjectLinkedOpenHashMap<>());
        json.entrySet().forEach(entry -> {
            SoundsKey sound = SoundsKey.of(entry.getValue().getAsString());
            if (!sound.isResult()) {
                return;
            }

            Key key = Key.of(entry.getKey(), sound);

            substrates.computeIfAbsent(key.substrate(), Bucket.Substrate::new).add(key);
        });
    }

    @Override
    public Optional<SoundsKey> getAssociation(BlockState state, String substrate) {
        return substrates.getOrDefault(substrate, Bucket.EMPTY).get(state).value();
    }

    @Override
    public Set<String> getSubstrates() {
        return substrates.keySet();
    }

    @Override
    public boolean contains(BlockState state) {
        for (Bucket substrate : substrates.values()) {
            if (substrate.contains(state)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(BlockState state, String substrate) {
        return substrates.getOrDefault(substrate, Bucket.EMPTY).contains(state);
    }

    @Override
    public boolean isEmpty() {
        return substrates.isEmpty();
    }

    /** A container of entries that can answer what a block state resolves to. */
    interface Bucket {
        Bucket EMPTY = state -> Key.NULL;

        default void add(Key key) {}

        Key get(BlockState state);

        default boolean contains(BlockState state) {
            return false;
        }

        /**
         * All entries for one substrate, split by how they are addressed. Blocks and tags are
         * indexed by identifier; wildcards have to be scanned.
         */
        record Substrate(
                KeyList wildcards,
                Map<ResourceLocation, Bucket> blocks,
                Map<ResourceLocation, Bucket> tags) implements Bucket {

            Substrate(String substrate) {
                this(new KeyList(), new Object2ObjectLinkedOpenHashMap<>(), new Object2ObjectLinkedOpenHashMap<>());
            }

            @Override
            public void add(Key key) {
                if (key.isWildcard()) {
                    wildcards.add(key);
                } else {
                    (key.isTag() ? tags : blocks).computeIfAbsent(key.identifier(), Tile::new).add(key);
                }
            }

            @Override
            public Key get(BlockState state) {
                final Key association = getTile(state).get(state);

                return association == Key.NULL
                        ? wildcards.findMatch(state)
                        : association;
            }

            @Override
            public boolean contains(BlockState state) {
                return getTile(state).contains(state) || wildcards.findMatch(state) != Key.NULL;
            }

            /**
             * Resolves which entries apply to a block, memoising the answer into the block
             * index. Tag membership is the expensive part and only ever resolved once per
             * block per reload.
             */
            private Bucket getTile(BlockState state) {
                return blocks.computeIfAbsent(BuiltInRegistries.BLOCK.getKey(state.getBlock()), id -> {
                    for (ResourceLocation tag : tags.keySet()) {
                        if (state.is(TagKey.create(Registries.BLOCK, tag))) {
                            return tags.get(tag);
                        }
                    }

                    return Bucket.EMPTY;
                });
            }
        }

        /** The entries for a single block or tag, with the per-state answer cached. */
        record Tile(Map<BlockState, Key> cache, KeyList keys) implements Bucket {
            Tile(ResourceLocation id) {
                this(new Object2ObjectLinkedOpenHashMap<>(), new KeyList());
            }

            @Override
            public void add(Key key) {
                keys.add(key);
            }

            @Override
            public Key get(BlockState state) {
                return cache.computeIfAbsent(state, keys::findMatch);
            }

            @Override
            public boolean contains(BlockState state) {
                return get(state) != Key.NULL;
            }
        }
    }

    /**
     * Entries in match order. Those constraining block state properties are tried first, so a
     * specific {@code oak_slab[type=top]} beats a bare {@code oak_slab}.
     */
    record KeyList(Set<Key> priorityKeys, Set<Key> keys) {

        KeyList() {
            this(new ObjectLinkedOpenHashSet<>(), new ObjectLinkedOpenHashSet<>());
        }

        void add(Key key) {
            Set<Key> target = key.empty() ? keys : priorityKeys;
            // Removed first so a later pack's entry replaces rather than losing to the original
            // insertion position.
            target.remove(key);
            target.add(key);
        }

        Key findMatch(BlockState state) {
            for (Key i : priorityKeys) {
                if (i.matches(state)) {
                    return i;
                }
            }
            for (Key i : keys) {
                if (i.matches(state)) {
                    return i;
                }
            }
            return Key.NULL;
        }
    }

    /** One parsed block map entry. */
    record Key(
            ResourceLocation identifier,
            String substrate,
            Set<Attribute> properties,
            Optional<SoundsKey> value,
            boolean empty,
            boolean isTag,
            boolean isWildcard
    ) {
        static final Key NULL = new Key(ResourceLocation.withDefaultNamespace("air"), "",
                ObjectSets.emptySet(), Optional.empty(), true, false, false);

        static Key of(String key, SoundsKey value) {
            final boolean isTag = key.indexOf('#') == 0;

            if (isTag) {
                key = key.replaceFirst("#", "");
            }

            final String id = key.split("[\\.\\[]")[0];
            final boolean isWildcard = id.indexOf('*') == 0;
            ResourceLocation identifier = NULL.identifier();

            if (!isWildcard) {
                if (id.indexOf('^') > -1) {
                    identifier = ResourceLocation.parse(id.split("\\^")[0]);
                    TreadLightly.LOGGER.warn("Metadata entry for {}={} was ignored", key, value.raw());
                } else {
                    identifier = ResourceLocation.parse(id);
                }

                if (!isTag && !BuiltInRegistries.BLOCK.containsKey(identifier)) {
                    TreadLightly.LOGGER.warn("Sound registered for unknown block id {}", identifier);
                }
            }

            key = key.replace(id, "");
            final String substrate = key.replaceFirst("\\[[^\\]]+\\]", "");
            String finalSubstrate = "";

            if (substrate.indexOf('.') > -1) {
                finalSubstrate = substrate.split("\\.")[1];
                key = key.replace(substrate, "");
            }

            final Set<Attribute> properties = ObjectArrayList.of(
                         key.replace("[", "")
                            .replace("]", "")
                            .split(","))
                    .stream()
                    .filter(line -> line.indexOf('=') > -1)
                    .map(Attribute::new)
                    .collect(ObjectOpenHashSet.toSet());

            return new Key(identifier, finalSubstrate, properties, Optional.of(value),
                    properties.isEmpty(), isTag, isWildcard);
        }

        /**
         * Whether this entry's property constraints hold for a state.
         * <p>
         * A constraint naming a property the state does not have is ignored rather than
         * failing the match. That is how the original behaved, and packs in the wild carry
         * such entries, so tightening it would silence blocks that currently sound correct.
         */
        boolean matches(BlockState state) {
            if (empty) {
                return true;
            }

            Map<Property<?>, Comparable<?>> entries = state.getValues();

            for (Attribute property : properties) {
                for (Property<?> key : entries.keySet()) {
                    if (key.getName().equals(property.name())) {
                        if (!Objects.toString(entries.get(key)).equalsIgnoreCase(property.value())) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return (isTag ? "#" : "")
                    + identifier
                    + "[" + properties.stream().map(Attribute::toString).collect(Collectors.joining()) + "]"
                    + "." + substrate
                    + "=" + value;
        }

        /**
         * Identity is the entry's address, deliberately excluding {@link #value()}, so that a
         * later pack mapping the same block and properties replaces the earlier entry instead
         * of sitting alongside it.
         */
        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof Key other
                    && isTag == other.isTag
                    && isWildcard == other.isWildcard
                    && empty == other.empty
                    && Objects.equals(identifier, other.identifier)
                    && Objects.equals(substrate, other.substrate)
                    && Objects.equals(properties, other.properties));
        }

        @Override
        public int hashCode() {
            return Objects.hash(empty, identifier, isTag, isWildcard, properties, substrate);
        }

        /** One {@code name=value} constraint from an entry's bracketed section. */
        record Attribute(String name, String value) {
            Attribute(String property) {
                this(property.split("="));
            }

            Attribute(String[] split) {
                this(split[0], split[1]);
            }

            @Override
            public String toString() {
                return name + "=" + value;
            }
        }
    }
}
