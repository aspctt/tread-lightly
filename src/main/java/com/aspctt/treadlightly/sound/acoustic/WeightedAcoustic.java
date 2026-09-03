// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;

/**
 * Picks one of several acoustics at random, each as likely as its declared weight.
 * <p>
 * A pack writes this as a flat array alternating weight and acoustic, so
 * {@code [3, "stone1", 1, "stone2"]} plays the first three times as often as the second.
 */
record WeightedAcoustic(Entry[] entries) implements Acoustic {
    static final Serializer FACTORY = Serializer.ofJsObject((json, context) -> {
        Iterator<JsonElement> iter = json.getAsJsonArray(json.has("array") ? "array" : "entries").iterator();
        List<Integer> weights = new ArrayList<>();
        List<Acoustic> acoustics = new ArrayList<>();

        while (iter.hasNext()) {
            int weight = iter.next().getAsInt();

            if (!iter.hasNext()) {
                throw new JsonParseException("Probability has odd number of children!");
            }
            if (weight < 0) {
                throw new JsonParseException("A probability weight can't be negative");
            }

            weights.add(weight);
            acoustics.add(Acoustic.read(context, iter.next()));
        }

        return build(weights, acoustics);
    });

    /**
     * Builds the running totals a draw is compared against.
     * <p>
     * The original stored each entry's own share of the total here rather than the running
     * sum, and then looked for the first entry whose share was at least as large as the draw.
     * With two equal weights that gives both entries a threshold of 0.5, so any draw above 0.5
     * matched nothing at all and the footstep fell silent. Half of all steps over such a block
     * made no sound.
     */
    private static Acoustic build(List<Integer> weights, List<Acoustic> acoustics) {
        int total = 0;
        for (int weight : weights) {
            total += weight;
        }

        if (total <= 0) {
            // Nothing can ever be drawn, so say so once at load rather than failing silently
            // on every step.
            return EmptyAcoustic.INSTANCE;
        }

        Entry[] entries = new Entry[acoustics.size()];
        int running = 0;
        for (int i = 0; i < entries.length; i++) {
            running += weights.get(i);
            entries[i] = new Entry((float) running / total, acoustics.get(i));
        }
        return new WeightedAcoustic(entries);
    }

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        final float rand = player.getRNG().nextFloat();

        for (Entry entry : entries) {
            // nextFloat is below 1 and the final threshold is exactly 1, so this always hits.
            if (rand < entry.threshold()) {
                entry.acoustic().playSound(player, location, event, inputOptions);
                return;
            }
        }
    }

    /** One choice, holding the running total of every weight up to and including its own. */
    record Entry(float threshold, Acoustic acoustic) { }
}
