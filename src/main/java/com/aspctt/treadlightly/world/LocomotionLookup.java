// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.JsonElement;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.sound.generator.Locomotion;

/**
 * Which gait each entity type walks with, from the pack's locomotion map.
 * <p>
 * Anything unlisted is assumed to walk on two legs, since a modded humanoid is far more likely
 * than a modded fish.
 */
public class LocomotionLookup implements Index<Entity, Locomotion> {
    private final Map<ResourceLocation, Locomotion> values = new Object2ObjectOpenHashMap<>();

    /** The player's own choice, read live so changing it in the config takes effect at once. */
    private final Supplier<Locomotion> playerStance;

    public LocomotionLookup(Supplier<Locomotion> playerStance) {
        this.playerStance = playerStance;
    }

    @Override
    public Locomotion lookup(Entity key) {
        if (key instanceof Player player) {
            return Locomotion.forPlayer(player, playerStance.get());
        }
        return values.getOrDefault(EntityType.getKey(key.getType()), Locomotion.BIPED);
    }

    @Override
    public void add(String key, JsonElement value) {
        ResourceLocation id = ResourceLocation.parse(key);

        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            // Not fatal: packs routinely cover mods the player has not installed.
            TreadLightly.LOGGER.debug("Locomotion registered for unknown entity type {}", id);
        }

        values.put(id, Locomotion.byName(value.getAsString().toUpperCase(Locale.ROOT)));
    }

    @Override
    public boolean contains(ResourceLocation key) {
        return values.containsKey(key);
    }
}
