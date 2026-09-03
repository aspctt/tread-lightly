// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Optional;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * Sounds for standing on an entity rather than a block: boats, minecarts, iron golems, and
 * anything else broad and solid enough to walk about on.
 */
public class GolemLookup extends AbstractSubstrateLookup<EntityType<?>> {
    public GolemLookup(JsonObject json) {
        super(json);
    }

    @Override
    public Optional<SoundsKey> getAssociation(EntityType<?> key, String substrate) {
        return getSubstrateMap(getId(key), substrate).getOrDefault(getId(key), Optional.empty());
    }

    @Override
    protected ResourceLocation getId(EntityType<?> key) {
        return EntityType.getKey(key);
    }
}
