// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.config;

import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

/** Which entities get footsteps at all. */
public enum EntitySelector implements Predicate<Entity> {
    ALL(SoundSource.PLAYERS, SoundSource.HOSTILE, SoundSource.NEUTRAL) {
        @Override
        public boolean test(Entity entity) {
            return true;
        }
    },
    PLAYERS_AND_HOSTILES(SoundSource.PLAYERS, SoundSource.HOSTILE) {
        @Override
        public boolean test(Entity entity) {
            return entity instanceof Player || entity instanceof Monster;
        }
    },
    PLAYERS_ONLY(SoundSource.PLAYERS) {
        @Override
        public boolean test(Entity entity) {
            return entity instanceof Player;
        }
    };

    public static final EntitySelector[] VALUES = values();

    private final Set<SoundSource> affectedSources;

    EntitySelector(SoundSource... sources) {
        this.affectedSources = Set.of(sources);
    }

    /** The sound categories this selection covers, for suppressing the vanilla equivalents. */
    public Set<SoundSource> getAffectedSources() {
        return affectedSources;
    }
}
