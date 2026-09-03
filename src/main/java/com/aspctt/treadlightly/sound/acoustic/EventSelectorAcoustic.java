// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;

/**
 * Picks a different acoustic depending on what the entity was doing.
 * <p>
 * A state the pack did not define falls back along the chain {@link State} declares, so a pack
 * defining only {@code walk} and {@code run} still covers stairs, ladders and landings.
 */
record EventSelectorAcoustic(Map<State, Acoustic> pairs) implements Acoustic {
    static final Serializer FACTORY = Serializer.ofJsObject((json, context) -> {
        // An EnumMap rather than a hash map: keyed by an enum, read on every footstep, and
        // indexed by ordinal rather than hashed.
        Map<State, Acoustic> pairs = new EnumMap<>(State.class);
        for (State state : State.values()) {
            if (json.has(state.getName())) {
                pairs.put(state, Acoustic.read(context, json.get(state.getName())));
            }
        }
        return new EventSelectorAcoustic(pairs);
    });

    @Override
    public void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions) {
        // One lookup rather than containsKey followed by get.
        @Nullable Acoustic acoustic = pairs.get(event);

        if (acoustic != null) {
            acoustic.playSound(player, location, event, inputOptions);
        } else if (event.canTransition()) {
            playSound(player, location, event.getTransitionDestination(), inputOptions);
        }
    }
}
