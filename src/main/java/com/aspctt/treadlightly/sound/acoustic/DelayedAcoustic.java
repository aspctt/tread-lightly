// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;
import com.aspctt.treadlightly.util.Period;

/**
 * Holds its acoustic back for a while, which is what puts the scuff of a heel slightly after
 * the weight of the step rather than on top of it.
 * <p>
 * The delay is not applied here. It rides along in the options and is honoured by the player,
 * because only the player knows what time it is and what else is already queued.
 */
record DelayedAcoustic(
        Acoustic acoustic,
        Period delay
) implements Acoustic {
    static final Serializer FACTORY = Serializer.ofJsObject((json, context) -> new DelayedAcoustic(
        // A delayed entry may either name a sound directly or wrap another acoustic.
        json.has("name") ? VaryingAcoustic.FACTORY.create(json, context) : Acoustic.read(context, json.get("acoustic")),
        Period.fromJson(json, "delay")
    ));

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        acoustic.playSound(player, location, event, inputOptions.and(delay));
    }
}
