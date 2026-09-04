// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.player;

import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.generator.StepSoundGenerator;
import com.aspctt.treadlightly.util.PlayerUtil;

/** Hands a resolved sound straight to the game's sound manager. */
public final class ImmediateSoundPlayer implements SoundPlayer {
    /**
     * Namespace used for sounds played for anybody but the listener.
     * <p>
     * Stereo recordings do not position well in 3D, so a pack ships mono variants for other
     * people's footsteps and keeps the wider ones for your own.
     */
    private static final String MONO_NAMESPACE = TreadLightly.MODID + "mono";

    /**
     * Beyond this squared distance the sound is scheduled a little later, so that a distant
     * footstep arrives roughly when the sound of it would have.
     */
    private static final double TRAVEL_TIME_DISTANCE = 100;

    private final Random random = new Random();
    private final PlaybackSource source;

    public ImmediateSoundPlayer(PlaybackSource source) {
        this.source = source;
    }

    @Override
    public Random getRNG() {
        return random;
    }

    @Override
    public void playSound(LivingEntity location, String soundName, float volume, float pitch, Options options) {
        volume *= options.getOrDefault("volume_percentage", 1F);
        pitch *= options.getOrDefault("pitch_percentage", 1F);

        Minecraft client = Minecraft.getInstance();

        volume *= source.getVolumeFor(location);

        // A larger entity sounds deeper, the same way its strides are longer.
        pitch /= ((PlayerUtil.getScale(location) - 1) * 0.6F) + 1;

        @Nullable StepSoundGenerator generator = source.getGeneratorFor(location);
        if (generator != null) {
            float tickDelta = client.getTimer().getGameTimeDeltaPartialTick(false);
            volume *= generator.getLocalVolume(tickDelta);
            pitch *= generator.getLocalPitch(tickDelta);
        }

        ResourceLocation id = resolve(client.getSoundManager(), soundName, location);

        SoundInstance sound = new SimpleSoundInstance(
                id,
                location.getSoundSource(),
                volume, pitch,
                SoundInstance.createUnseededRandom(),
                false, 0,
                SoundInstance.Attenuation.LINEAR,
                location.getX(), location.getY(), location.getZ(),
                false);

        double distance = client.gameRenderer.getMainCamera().getPosition().distanceToSqr(location.position());

        if (distance > TRAVEL_TIME_DISTANCE) {
            client.getSoundManager().playDelayed(sound, (int) Math.floor(Math.sqrt(distance) / 2));
        } else {
            client.getSoundManager().play(sound);
        }
    }

    /**
     * Turns a pack's sound name into an id.
     * <p>
     * A name carrying its own namespace is taken as written, which is how a pack points at
     * sounds that belong to something else. Otherwise it lands in this mod's namespace, or the
     * mono one when the sound is for somebody other than the listener.
     */
    private static ResourceLocation resolve(SoundManager sounds, String name, LivingEntity location) {
        if (name.indexOf(':') >= 0) {
            return ResourceLocation.parse(name);
        }

        if (!PlayerUtil.isClientPlayer(location)) {
            ResourceLocation mono = ResourceLocation.fromNamespaceAndPath(MONO_NAMESPACE, name);
            // Falling back rather than going silent, since a pack is under no obligation to
            // ship mono variants at all.
            if (sounds.getSoundEvent(mono) != null) {
                return mono;
            }
        }

        return TreadLightly.id(name);
    }
}
