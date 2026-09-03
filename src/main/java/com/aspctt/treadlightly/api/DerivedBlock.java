// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that was built from another one, and can reasonably borrow its sound.
 * <p>
 * Oak stairs are oak planks, and a modded block copying vanilla's properties usually wants
 * vanilla's sound. Resolving that means a pack does not need an entry for every stair, slab and
 * wall of every wood in every mod. {@link Block} is made to implement this by a mixin, and
 * stairs additionally expose the block they were cut from.
 */
public interface DerivedBlock {
    /**
     * The block this one was built from, or air when there is nothing sensible.
     * <p>
     * Falls back to air rather than failing when the mixin has not been applied, so the engine
     * degrades to plain block map lookups instead of refusing to make any sound at all.
     */
    static BlockState getBaseOf(BlockState state) {
        return state.getBlock() instanceof DerivedBlock derived
                ? derived.getBaseBlockState()
                : Blocks.AIR.defaultBlockState();
    }

    BlockState getBaseBlockState();

    /** Implemented on block properties, to remember what a copied set of properties came from. */
    interface Settings {
        @Nullable
        Block getBaseBlock();

        void setBaseBlock(Block block);
    }
}
