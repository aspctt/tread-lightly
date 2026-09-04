// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.util;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import com.aspctt.treadlightly.TreadLightly;

/**
 * Reads config files out of every enabled pack rather than only the topmost one, so packs
 * layer instead of replacing each other. A file that fails to parse is logged against the pack
 * that supplied it and skipped, leaving the rest working.
 */
public interface ResourceUtils {
    /** @return true if at least one pack supplied this file and it read cleanly */
    static boolean forEach(ResourceLocation id, ResourceManager manager, Consumer<Reader> consumer) {
        int loaded = 0;

        for (Resource resource : manager.getResourceStack(id)) {
            try (Reader reader = resource.openAsReader()) {
                consumer.accept(reader);
                loaded++;
            } catch (Exception e) {
                TreadLightly.LOGGER.error("Could not read {} from pack {}", id, resource.sourcePackId(), e);
            }
        }

        return loaded > 0;
    }

    static <T> Stream<T> load(ResourceLocation id, ResourceManager manager, Function<JsonObject, T> reader) {
        return load(id, manager.getResourceStack(id), reader);
    }

    static <T> Stream<T> load(ResourceLocation id, List<Resource> resources, Function<JsonObject, T> reader) {
        return resources.stream().map(resource -> {
            try (Reader stream = resource.openAsReader()) {
                return reader.apply(JsonParser.parseReader(stream).getAsJsonObject());
            } catch (Exception e) {
                TreadLightly.LOGGER.error("Could not read {} from pack {}", id, resource.sourcePackId(), e);
                return (T) null;
            }
        }).filter(Objects::nonNull);
    }

    /**
     * Reads every file under a directory, keyed by whatever the name maps to.
     * <p>
     * Used for the per-entity block maps, where the file name is the entity type. Files whose
     * name maps to nothing are skipped, since a pack covering a mod the player has not
     * installed is normal rather than an error.
     */
    @SuppressWarnings("unchecked")
    static <T, K, V> Map<K, V> loadDir(FileToIdConverter finder, ResourceManager manager,
            Function<JsonObject, T> reader,
            Function<ResourceLocation, @Nullable K> keyMapper,
            Function<Stream<T>, @Nullable V> valueMapper) {
        return Map.ofEntries(finder.listMatchingResourceStacks(manager).entrySet().stream()
                .map(entry -> {
                    K key = keyMapper.apply(finder.fileToId(entry.getKey()));
                    if (key == null) {
                        return null;
                    }
                    V value = valueMapper.apply(load(entry.getKey(), entry.getValue(), reader));
                    return value == null ? null : Map.entry(key, value);
                })
                .filter(Objects::nonNull)
                .toArray(Map.Entry[]::new));
    }
}
