// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the solver decided a foot landed on: the block, where it was, and the acoustics for each
 * of the three layers that can sound at once.
 *
 * @param state     the block the foot actually met, which may not be the one at {@code pos}
 * @param pos       where that block is
 * @param source    the entity that stepped
 * @param forcePlay play even where the block reads as air, used when standing on an entity
 * @param dry       the step itself
 * @param wet       layered over the step when the block is wet
 * @param foliage   layered over the step when plants brush the legs
 */
public record Association(
        BlockState state,
        BlockPos pos,
        @Nullable LivingEntity source,
        boolean forcePlay,
        SoundsKey dry,
        SoundsKey wet,
        SoundsKey foliage
) {
    public static final Association NOT_EMITTER = new Association(
            Blocks.AIR.defaultBlockState(), BlockPos.ZERO, null, false,
            SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER);

    public static Association of(BlockState state, BlockPos pos, LivingEntity source, boolean forcePlay,
                                 SoundsKey dry, SoundsKey wet, SoundsKey foliage) {
        if (dry.isSilent() && wet.isSilent() && foliage.isSilent()) {
            return NOT_EMITTER;
        }
        // Immutable because the solver walks a mutable position while searching, and this
        // outlives that walk.
        return new Association(state, pos.immutable(), source, forcePlay, dry, wet, foliage);
    }

    /** True if any layer found an entry, even one that says stay silent. */
    public boolean isResult() {
        return dry.isResult() || wet.isResult() || foliage.isResult();
    }

    public boolean isSilent() {
        return this == NOT_EMITTER || (state.isAir() && !forcePlay);
    }

    /**
     * Whether two feet landed on the same thing. Used to play a two-footed sound once rather
     * than twice when both feet resolve identically.
     */
    public boolean dataEquals(Association other) {
        return Objects.equals(dry, other.dry);
    }
}
