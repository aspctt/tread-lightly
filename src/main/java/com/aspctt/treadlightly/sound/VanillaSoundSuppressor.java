// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.sound.player.EngineSoundInstance;

/**
 * Silences the game's own player sounds that this mod replaces.
 * <p>
 * The entity mixin only covers steps the client works out for itself. On a server the sounds a
 * player makes arrive as messages instead, so without this you hear both: ours and the game's,
 * a fraction apart. Swimming, splashing and landing arrive the same way.
 */
public final class VanillaSoundSuppressor {
    /** Player sounds this mod produces itself, wherever they come from. */
    private static final Set<ResourceLocation> REPLACED = Set.of(
            SoundEvents.PLAYER_SWIM.getLocation(),
            SoundEvents.PLAYER_SPLASH.getLocation(),
            SoundEvents.PLAYER_SPLASH_HIGH_SPEED.getLocation(),
            SoundEvents.PLAYER_BIG_FALL.getLocation(),
            SoundEvents.PLAYER_SMALL_FALL.getLocation());

    private VanillaSoundSuppressor() {
    }

    public static void onPlaySound(PlaySoundEvent event) {
        @Nullable SoundInstance sound = event.getSound();

        // Ours, and some of ours are the block's own step sound played in the player category,
        // which is what the test below looks for.
        if (sound == null || sound instanceof EngineSoundInstance) {
            return;
        }

        @Nullable SoundEngine engine = TreadLightly.engine();
        if (engine == null || !engine.replacesVanillaSounds()) {
            return;
        }

        if (isReplaced(sound)) {
            event.setSound(null);
        }
    }

    private static boolean isReplaced(SoundInstance sound) {
        ResourceLocation id = sound.getLocation();

        if (REPLACED.contains(id)) {
            return true;
        }

        // Anything else only counts if it is a player's own footstep, which is the block's step
        // sound played in the player category at the spot they are standing.
        if (sound.getSource() != SoundSource.PLAYERS) {
            return false;
        }

        @Nullable ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        BlockPos below = BlockPos.containing(sound.getX(), sound.getY() - 1, sound.getZ());
        @Nullable SoundEvent step = level.getBlockState(below).getSoundType(level, below, null).getStepSound();

        return step != null && id.equals(step.getLocation());
    }
}
