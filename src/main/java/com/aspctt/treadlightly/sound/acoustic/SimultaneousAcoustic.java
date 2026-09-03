// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import java.util.List;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;

/**
 * Several acoustics at once, which is how a pack layers a scuff over a thud rather than needing
 * a single recording of both.
 */
record SimultaneousAcoustic(List<Acoustic> acoustics) implements Acoustic {
    static final Serializer FACTORY = (json, context) -> new SimultaneousAcoustic(
            (json.isJsonArray() ? json.getAsJsonArray() : json.getAsJsonObject().getAsJsonArray("array"))
            .asList()
            .stream()
            .map(element -> Acoustic.read(context, element))
            .toList()
    );

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        // Indexed rather than for-each: this runs for every layered footstep and an iterator
        // per step per entity is pure garbage.
        for (int i = 0; i < acoustics.size(); i++) {
            acoustics.get(i).playSound(player, location, event, inputOptions);
        }
    }
}
