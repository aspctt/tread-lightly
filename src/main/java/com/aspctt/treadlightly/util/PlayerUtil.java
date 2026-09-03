// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface PlayerUtil {
    /**
     * Whether this is the player at the keyboard, as opposed to somebody else's player entity.
     * <p>
     * The distinction runs through the whole engine: the local player has real velocity and
     * distance counters to work from, while everyone else has to be inferred from position
     * changes, and the two are mixed differently and played at different volumes.
     */
    static boolean isClientPlayer(Entity entity) {
        Player client = Minecraft.getInstance().player;

        return entity instanceof Player
                && !(entity instanceof RemotePlayer)
                && client != null
                && (client == entity || client.getUUID().equals(entity.getUUID()));
    }

    /**
     * How large an entity is against the normal size for its type, so a resized entity takes
     * proportionally longer strides and sounds deeper. Clamped because a scale of zero would
     * divide through the rest of the engine.
     */
    static float getScale(LivingEntity entity) {
        return Mth.clamp(entity.getBbWidth() / entity.getType().getDimensions().width(), 0.01F, 200F);
    }
}
