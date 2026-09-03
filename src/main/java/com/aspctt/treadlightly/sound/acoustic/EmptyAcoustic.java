// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;

/** An acoustic that deliberately makes no sound, for a pack silencing one state of many. */
record EmptyAcoustic() implements Acoustic {
    static final Acoustic INSTANCE = new EmptyAcoustic();

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
    }
}
