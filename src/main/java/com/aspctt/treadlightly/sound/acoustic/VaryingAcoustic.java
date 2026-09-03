// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import com.google.gson.JsonObject;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;
import com.aspctt.treadlightly.util.Range;

/**
 * One sound, played at a volume and pitch drawn from its windows. The leaf of the tree, and
 * what everything else eventually resolves to.
 */
record VaryingAcoustic(
        String soundName,
        Range volume,
        Range pitch
) implements Acoustic {
    static final Serializer FACTORY = (json, context) -> {
        if (json.isJsonPrimitive()) {
            return new VaryingAcoustic(
                context.getSoundName(json.getAsString()),
                context.defaultVolume(),
                context.defaultPitch()
            );
        }
        JsonObject object = json.getAsJsonObject();
        if (!object.has("name")) {
            return EmptyAcoustic.INSTANCE;
        }
        String name = object.get("name").getAsString();
        if (name.isEmpty()) {
            return EmptyAcoustic.INSTANCE;
        }
        return new VaryingAcoustic(
                context.getSoundName(name),
                context.defaultVolume().read("volume", object),
                context.defaultPitch().read("pitch", object)
        );
    };

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        if (soundName.isEmpty()) {
            // A deliberately empty sound, which is not the same as falling back to another.
            return;
        }

        // A gliding value fades across the window rather than picking from it, which is what
        // lets wing beats and heavy landings scale with how fast the entity was moving.
        final float finalVolume = (inputOptions.containsKey("gliding_volume")
                ? volume.on(inputOptions.get("gliding_volume"))
                : volume.random(player.getRNG())) * inputOptions.getOrDefault("volume_scale", 1F);

        final float finalPitch = inputOptions.containsKey("gliding_pitch")
                ? pitch.on(inputOptions.get("gliding_pitch"))
                : pitch.random(player.getRNG());

        player.playSound(location, soundName, finalVolume, finalPitch, inputOptions);
    }
}
