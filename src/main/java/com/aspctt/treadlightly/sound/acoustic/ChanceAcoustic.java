// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;

/**
 * Plays its acoustic only some of the time, so an occasional creak or scuff can sit on top of
 * an ordinary step without being there every time.
 *
 * @param probability percentage chance, so 100 always plays and 0 never does
 */
record ChanceAcoustic(
        Acoustic acoustic,
        float probability
) implements Acoustic {
    static final Serializer FACTORY = Serializer.ofJsObject((json, context) -> new ChanceAcoustic(
        Acoustic.read(context, json.get("acoustic")),
        json.get("probability").getAsFloat()
    ));

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        if (player.getRNG().nextFloat() * 100 <= probability) {
            acoustic.playSound(player, location, event, inputOptions);
        }
    }
}
