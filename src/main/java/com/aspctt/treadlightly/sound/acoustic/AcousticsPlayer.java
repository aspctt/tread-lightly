// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.sound.AcousticVolumes;
import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;
import com.aspctt.treadlightly.world.Association;
import com.aspctt.treadlightly.world.SoundsKey;

/** Holds the named acoustics a pack defined and plays them on request. */
public class AcousticsPlayer implements AcousticLibrary {
    /** Below this the layer is inaudible anyway, so skip the work entirely. */
    private static final float AUDIBLE_THRESHOLD = 0.1F;

    private final Map<String, Acoustic> acoustics = new Object2ObjectOpenHashMap<>();

    /**
     * Names already complained about. A block map pointing at an acoustic no pack defines
     * would otherwise log on every single step over that block, which buries everything else
     * in the log and costs real time in a hot path.
     */
    private final Set<String> reportedMissing = new ObjectOpenHashSet<>();

    private final SoundPlayer soundPlayer;
    private final AcousticVolumes volumes;

    /** Built once and read lazily, so layering a volume onto a step allocates nothing. */
    private final Options wetVolume;
    private final Options foliageVolume;

    public AcousticsPlayer(SoundPlayer soundPlayer, AcousticVolumes volumes) {
        this.soundPlayer = soundPlayer;
        this.volumes = volumes;
        this.wetVolume = Options.ofGetter("volume_percentage", volumes::wet);
        this.foliageVolume = Options.ofGetter("volume_percentage", volumes::foliage);
    }

    @Override
    public void addAcoustic(String name, Acoustic acoustic) {
        if (acoustics.put(name, acoustic) != null) {
            // Expected whenever packs are layered: the later pack wins, which is the point.
            TreadLightly.LOGGER.debug("Acoustic {} was replaced by a later pack", name);
        }
    }

    @Override
    @SuppressWarnings("deprecation") // BlockState.liquid(), see below.
    public void playStep(Association association, State event, Options options) {
        if (association.isSilent()) {
            return;
        }

        @Nullable LivingEntity source = association.source();
        if (source == null) {
            return;
        }

        if (association.dry().isResult()) {
            playAcoustic(source, association.dry(), event, options);
        } else if (!association.state().liquid()) {
            // liquid() is deprecated but has no replacement, and getFluidState().isEmpty() is
            // not one: that is true of a waterlogged slab, which you are still standing on and
            // which should still make its ordinary sound. This asks whether the block is
            // itself a liquid, which is the question worth asking here.
            playVanillaStep(association, source, options);
        }

        if (association.wet().isEmitter() && volumes.wet() > AUDIBLE_THRESHOLD) {
            playAcoustic(source, association.wet(), event, options.and(wetVolume));
        }

        if (association.foliage().isEmitter() && volumes.foliage() > AUDIBLE_THRESHOLD) {
            playAcoustic(source, association.foliage(), event, options.and(foliageVolume));
        }
    }

    /**
     * Falls back to the block's own vanilla step sound, quietly, for a block the packs have an
     * opinion about in some other layer but not this one.
     */
    private void playVanillaStep(Association association, LivingEntity source, Options options) {
        // NeoForge's position-aware overload, so blocks that vary their sound by where they are
        // are asked properly rather than answering for their default state.
        SoundType soundType = association.state().getSoundType(source.level(), association.pos(), source);

        // Snow lying on top of a block is what you actually hear, not the block beneath it.
        BlockState above = source.level().getBlockState(association.pos().above());
        if (above.is(Blocks.SNOW)) {
            soundType = above.getSoundType(source.level(), association.pos().above(), source);
        }

        soundPlayer.playSound(source,
                soundType.getStepSound().getLocation().toString(),
                soundType.getVolume() * 0.15F,
                soundType.getPitch(),
                options
        );
    }

    @Override
    public void playAcoustic(LivingEntity location, SoundsKey sounds, State event, Options inputOptions) {
        for (String name : sounds.names()) {
            @Nullable Acoustic acoustic = acoustics.get(name);

            if (acoustic == null) {
                if (reportedMissing.add(name)) {
                    TreadLightly.LOGGER.warn("Tried to play a missing acoustic: {}", name);
                }
            } else {
                acoustic.playSound(soundPlayer, location, event, inputOptions);
            }
        }
    }

    @Override
    public void think() {
        soundPlayer.think();
    }
}
