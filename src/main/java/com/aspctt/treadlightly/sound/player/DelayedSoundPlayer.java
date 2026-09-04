// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.player;

import java.util.List;
import java.util.Random;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.util.MathUtil;

/**
 * Holds a sound back for the delay its acoustic asked for, then passes it on.
 * <p>
 * This is what puts the scuff of a heel just after the weight of the step rather than on top of
 * it. Anything without a delay goes straight through.
 * <p>
 * The queue is scanned rather than guarded by a next-due timestamp. It is almost always empty,
 * so scanning costs nothing, while a stale guard timestamp would be able to strand every
 * pending sound behind it for the rest of the session.
 */
public class DelayedSoundPlayer implements SoundPlayer {
    /**
     * A sound more than this fraction of its own delay window late is dropped instead of played.
     * <p>
     * A late footstep is worse than a missing one: it arrives after the foot has visibly moved
     * on, which reads as a second entity walking nearby.
     */
    private static final float LATENESS_TOLERANCE = 1.5F;

    private final List<PendingSound> pending = new ObjectArrayList<>();
    private final SoundPlayer immediate;

    public DelayedSoundPlayer(SoundPlayer immediate) {
        this.immediate = immediate;
    }

    @Override
    public Random getRNG() {
        return immediate.getRNG();
    }

    @Override
    public void playSound(LivingEntity location, String soundName, float volume, float pitch, Options options) {
        if (!options.containsKey("delay_min") || !options.containsKey("delay_max")) {
            immediate.playSound(location, soundName, volume, pitch, options);
            return;
        }

        long min = (long) options.get("delay_min");
        long max = (long) options.get("delay_max");

        pending.add(new PendingSound(location, soundName, volume, pitch, options,
                System.currentTimeMillis() + MathUtil.randAB(getRNG(), min, max),
                // "skippable" turns the lateness check off rather than on. Counter-intuitive,
                // but it is the pack format and packs are written against it.
                options.containsKey("skippable") ? -1L : max));
    }

    @Override
    public void think() {
        if (pending.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        // Indexed and backwards so a removal does not shuffle the elements still to check.
        for (int i = pending.size() - 1; i >= 0; i--) {
            PendingSound sound = pending.get(i);

            if (now < sound.timeToPlay()) {
                continue;
            }

            pending.remove(i);

            if (sound.window() < 0 || now - sound.timeToPlay() <= sound.window() / LATENESS_TOLERANCE) {
                immediate.playSound(sound.location(), sound.soundName(), sound.volume(), sound.pitch(), sound.options());
            }
        }
    }

    /**
     * @param timeToPlay when this is due, as a wall clock time
     * @param window     the delay window it was drawn from, or negative to never drop it
     */
    private record PendingSound(
            LivingEntity location,
            String soundName,
            float volume,
            float pitch,
            Options options,
            long timeToPlay,
            long window
    ) { }
}
