// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.client;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import com.aspctt.treadlightly.api.DerivedBlock;
import com.aspctt.treadlightly.world.Lookup;
import com.aspctt.treadlightly.world.Lookups;
import com.aspctt.treadlightly.world.PrimitiveLookup;
import com.aspctt.treadlightly.world.SoundsKey;

/**
 * Writes out what every block in the game resolves to, for someone writing a pack.
 * <p>
 * The useful mode is the short one, which lists only blocks nothing has an opinion about. That
 * is the pack author's to-do list: everything in it currently falls back to the block's vanilla
 * sound type. The full report is for checking what an existing entry actually does.
 */
public final class BlockReport {
    private BlockReport() {
    }

    /**
     * Collects the report. Must run on the client thread: the block map memoises what it
     * resolves as it goes, in maps that are not safe to touch from two threads at once, and
     * the game is resolving footsteps against those same maps every tick.
     *
     * @param full true to list every block, false to list only those with nothing mapped
     */
    public static JsonObject gather(Lookups lookups, boolean full) {
        JsonObject root = new JsonObject();
        JsonObject blocks = new JsonObject();
        int unmapped = 0;

        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            JsonObject associations = associations(lookups, state);

            if (!full && !associations.isEmpty()) {
                continue;
            }
            if (associations.isEmpty()) {
                unmapped++;
            }

            blocks.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(block)), describe(lookups, state, associations));
        }

        root.addProperty("blocks_in_game", BuiltInRegistries.BLOCK.size());
        root.addProperty("blocks_with_nothing_mapped", unmapped);
        root.addProperty("listing", full ? "every block" : "only blocks with nothing mapped");
        root.add("blocks", blocks);

        return root;
    }

    /** Writes a gathered report. Safe to call off the client thread, and slow enough to want to. */
    public static Path write(JsonObject report, boolean full, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = nextFreeName(directory, full ? "report-full" : "report");

        try (Writer writer = Files.newBufferedWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(report, writer);
        }

        return file;
    }

    /** What the block maps to, per substrate. Empty means nothing has an opinion about it. */
    private static JsonObject associations(Lookups lookups, BlockState state) {
        JsonObject result = new JsonObject();
        Map<String, SoundsKey> found = lookups.globalBlocks().getAssociations(state);

        for (Map.Entry<String, SoundsKey> entry : found.entrySet()) {
            result.addProperty(entry.getKey().isEmpty() ? "default" : entry.getKey(), entry.getValue().raw());
        }

        return result;
    }

    // The position-aware overload is the current one, but a report walks the whole block
    // registry with no world to place anything in, so the default state's own sound type is
    // the only answer available and the right one for a listing.
    @SuppressWarnings("deprecation")
    private static JsonObject describe(Lookups lookups, BlockState state, JsonObject associations) {
        JsonObject entry = new JsonObject();
        SoundType sounds = state.getSoundType();

        entry.addProperty("sound_type", String.valueOf(sounds.getStepSound().getLocation()));

        // What it would fall back to with nothing mapped, which is the answer for everything
        // in the short report.
        Lookup<SoundEvent> primitives = lookups.primitives();
        SoundsKey fallback = primitives.getAssociation(sounds.getStepSound(), PrimitiveLookup.getSubstrate(sounds));
        entry.addProperty("falls_back_to", fallback.isResult() ? fallback.raw() : "nothing");

        BlockState base = DerivedBlock.getBaseOf(state);
        if (!base.isAir()) {
            entry.addProperty("built_from", String.valueOf(BuiltInRegistries.BLOCK.getKey(base.getBlock())));
        }

        JsonArray tags = new JsonArray();
        state.getTags().map(TagKey::location).map(String::valueOf).sorted().forEach(tags::add);
        if (!tags.isEmpty()) {
            entry.add("tags", tags);
        }

        if (!associations.isEmpty()) {
            entry.add("mapped_to", associations);
        }

        return entry;
    }

    /** Never overwrites an earlier report, so two runs can be compared. */
    private static Path nextFreeName(Path directory, String base) {
        Path file = directory.resolve(base + ".json");

        for (int i = 1; Files.exists(file); i++) {
            file = directory.resolve(base + "_" + i + ".json");
        }

        return file;
    }
}
