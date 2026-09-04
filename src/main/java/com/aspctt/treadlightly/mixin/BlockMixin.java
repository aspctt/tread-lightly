// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.api.DerivedBlock;
import com.aspctt.treadlightly.sound.SoundEngine;

/**
 * Lets any block say what it was built from, so it can borrow that block's sound.
 * <p>
 * First choice is the block whose properties this one was copied from. Failing that, a guess
 * from the name, which catches modded leaves and similar where nothing was copied. Air means
 * no idea, and the lookup falls through to the block's own vanilla sound type.
 */
@Mixin(Block.class)
abstract class BlockMixin extends BlockBehaviour implements DerivedBlock {
    /** Never runs. Present so the compiler accepts extending the class being mixed into. */
    BlockMixin() {
        super(null);
    }

    @Override
    public BlockState getBaseBlockState() {
        @Nullable Block base = ((DerivedBlock.Settings) this.properties).getBaseBlock();

        if (base == null) {
            @Nullable SoundEngine engine = TreadLightly.engine();
            if (engine != null) {
                base = engine.getLookups().heuristics().getMostSimilar((Block) (Object) this);
            }
        }

        return (base == null ? Blocks.AIR : base).defaultBlockState();
    }
}
