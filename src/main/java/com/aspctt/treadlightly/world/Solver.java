// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * Works out what an entity is standing on. Not the same as reading the block under it: a foot
 * lands off centre, what it meets may be a carpet or a fence rather than the block at foot
 * level, and the answer can be several sounds at once.
 */
public interface Solver {
    /** Looks for plants brushing against an entity's legs rather than anything underfoot. */
    String MESSY_FOLIAGE_STRATEGY = "find_messy_foliage";

    /**
     * Resolves what one foot landed on.
     *
     * @param verticalOffsetAsMinus how far below the entity's feet to look, for jumps and falls
     * @param isRightFoot           which side of centre to place the foot
     * @return {@link Association#NOT_EMITTER} when nothing there should make a sound
     */
    Association findAssociation(AssociationPool associations, LivingEntity entity,
                                double verticalOffsetAsMinus, boolean isRightFoot);

    /** Resolves a specific position under a named strategy. */
    Association findAssociation(AssociationPool associations, LivingEntity entity,
                                BlockPos pos, String strategy);
}
