// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.player;

import java.util.Random;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;

/**
 * Where a resolved acoustic actually goes. Implementations either hand it straight to the sound
 * manager or hold it back for a declared delay.
 */
public interface SoundPlayer {
    void playSound(LivingEntity location, String soundName, float volume, float pitch, Options options);

    /**
     * The generator shared by every acoustic. Sound selection is cosmetic and never has to
     * agree across clients, so this deliberately is not the world's seeded random.
     */
    Random getRNG();

    /** Called once a frame so a player holding sounds back can release the due ones. */
    default void think() { }
}
