// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.config;

import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

import net.minecraft.util.Mth;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.sound.SoundSettings;
import com.aspctt.treadlightly.sound.generator.Locomotion;

/**
 * User settings, as a JSON file beside the game's other configuration.
 * <p>
 * Volumes are whole percentages because that is what the config screen shows and what a person
 * reading the file expects.
 */
public class TreadLightlyConfig implements SoundSettings {
    public int volume = 70;
    public int clientPlayerVolume = 100;
    public int otherPlayerVolume = 100;
    public int hostileEntitiesVolume = 100;
    public int passiveEntitiesVolume = 100;
    public int wetSoundsVolume = 50;
    public int foliageSoundsVolume = 100;

    /** Added on top as an entity works up to a run, so sprinting is louder than walking. */
    public int runningVolumeIncrease = 0;

    /** Ceiling on how many entities are given footsteps at once, for busy scenes. */
    public int maxSteppingEntities = 50;

    public boolean disabled = false;
    public boolean multiplayer = true;
    public boolean footwear = true;

    /** Suppress the game's own footsteps for everything covered by the entity selection. */
    public boolean exclusive = true;

    public Locomotion stance = Locomotion.NONE;
    public EntitySelector targetEntities = EntitySelector.ALL;

    private transient Path file;
    private transient Runnable onEnabledChanged = () -> { };

    public void bind(Path file, Runnable onEnabledChanged) {
        this.file = file;
        this.onEnabledChanged = onEnabledChanged;
    }

    public void load() {
        if (file != null && Files.isReadable(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                gson().fromJson(reader, TreadLightlyConfig.class);
            } catch (Exception e) {
                TreadLightly.LOGGER.error("Could not read config, using defaults", e);
            }
        }
        save();
    }

    public void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                gson().toJson(this, writer);
            }
        } catch (Exception e) {
            TreadLightly.LOGGER.error("Could not write config", e);
        }
    }

    /** Fills this instance rather than building a new one, so unmentioned keys keep defaults. */
    private Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(TreadLightlyConfig.class, (InstanceCreator<TreadLightlyConfig>) type -> this)
                .setPrettyPrinting()
                .create();
    }

    public boolean isEnabled() {
        return !disabled && getGlobalVolume() > 0;
    }

    public void setDisabled(boolean value) {
        if (disabled != value) {
            disabled = value;
            save();
            onEnabledChanged.run();
        }
    }

    public int getGlobalVolume() {
        return Mth.clamp(volume, 0, 100);
    }

    public int getRunningVolumeIncrease() {
        return Mth.clamp(runningVolumeIncrease, -100, 100);
    }

    public int getMaxSteppingEntities() {
        return Math.max(1, maxSteppingEntities);
    }

    public Locomotion getStance() {
        return stance == null ? Locomotion.NONE : stance;
    }

    public EntitySelector getEntitySelector() {
        return targetEntities == null ? EntitySelector.ALL : targetEntities;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean isEnabledInMultiplayer() {
        return multiplayer;
    }

    @Override
    public boolean footwearEnabled() {
        return footwear;
    }

    @Override
    public float wet() {
        return Mth.clamp(wetSoundsVolume, 0, 100) / 100F;
    }

    @Override
    public float foliage() {
        return Mth.clamp(foliageSoundsVolume, 0, 100) / 100F;
    }

    public float clientPlayer() {
        return Mth.clamp(clientPlayerVolume, 0, 100) / 100F;
    }

    public float otherPlayer() {
        return Mth.clamp(otherPlayerVolume, 0, 100) / 100F;
    }

    public float hostile() {
        return Mth.clamp(hostileEntitiesVolume, 0, 100) / 100F;
    }

    public float passive() {
        return Mth.clamp(passiveEntitiesVolume, 0, 100) / 100F;
    }
}
