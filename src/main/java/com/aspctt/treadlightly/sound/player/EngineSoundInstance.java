// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.player;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A sound this mod is playing.
 * <p>
 * Marked so the suppressor can leave it alone. Some of our own sounds fall back to the block's
 * vanilla step sound, played in the player category at the player's position, which is exactly
 * what the suppressor looks for. Without something to tell them apart it would cancel us.
 */
public class EngineSoundInstance extends SimpleSoundInstance {
    public EngineSoundInstance(ResourceLocation id, SoundSource source, float volume, float pitch,
                               RandomSource random, double x, double y, double z) {
        super(id, source, volume, pitch, random, false, 0, Attenuation.LINEAR, x, y, z, false);
    }
}
