// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
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

    /** How far from a sound to look for the player it belongs to. */
    private static final double PLAYER_REACH = 0.5;

    private VanillaSoundSuppressor() {
    }

    public static void onPlaySound(PlaySoundEvent event) {
        @Nullable SoundInstance sound = event.getSound();

        // Ours, and some of ours are the block's own step sound played in the player category,
        // which is what the footstep test below looks for.
        if (sound == null || sound instanceof EngineSoundInstance) {
            return;
        }

        @Nullable SoundEngine engine = TreadLightly.engine();
        if (engine == null || !engine.replacesVanillaSounds()) {
            return;
        }

        if (sound.getSource() != SoundSource.PLAYERS && !REPLACED.contains(sound.getLocation())) {
            return;
        }

        @Nullable ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        // Only silence a sound this mod is actually standing in for. Without this the range
        // rules diverge: sound carries further than the distance we generate footsteps over, so
        // a distant player's steps would be cancelled with nothing put in their place.
        if (!isCoveredPlayerAt(engine, level, sound)) {
            return;
        }

        if (REPLACED.contains(sound.getLocation()) || isFootstepAt(level, sound)) {
            event.setSound(null);
        }
    }

    /** Whether a player this mod is generating footsteps for is standing where the sound is. */
    private static boolean isCoveredPlayerAt(SoundEngine engine, ClientLevel level, SoundInstance sound) {
        List<AbstractClientPlayer> players = level.players();

        for (int i = 0; i < players.size(); i++) {
            AbstractClientPlayer player = players.get(i);

            if (player.getBoundingBox().inflate(PLAYER_REACH)
                    .contains(sound.getX(), sound.getY(), sound.getZ())
                    && engine.isEnabledFor(player)
                    && engine.getGeneratorFor(player) != null) {
                return true;
            }
        }

        return false;
    }

    /** Whether this is the sound the game would have played for a step on the block underneath. */
    private static boolean isFootstepAt(ClientLevel level, SoundInstance sound) {
        BlockPos below = BlockPos.containing(sound.getX(), sound.getY() - 1, sound.getZ());
        @Nullable SoundEvent step = level.getBlockState(below).getSoundType(level, below, null).getStepSound();

        return step != null && sound.getLocation().equals(step.getLocation());
    }
}
