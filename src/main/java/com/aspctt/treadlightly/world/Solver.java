// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * Works out what an entity is actually standing on.
 * <p>
 * Harder than reading the block under its feet. A foot lands to one side of the entity's centre
 * depending on which foot it is and which way the entity faces; the block underfoot may be a
 * carpet lying on something else, or a fence the entity is perched on with air at foot level;
 * and the answer may be several sounds at once, a step plus wetness plus foliage.
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
