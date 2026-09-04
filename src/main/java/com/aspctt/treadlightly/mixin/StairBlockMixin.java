// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.aspctt.treadlightly.api.DerivedBlock;

/**
 * Stairs already know what they were cut from, so use that rather than guessing.
 * <p>
 * More reliable than the copied-properties route: a mod can build stairs from properties that
 * came from somewhere else entirely, but the state recorded here is the block itself.
 */
@Mixin(StairBlock.class)
abstract class StairBlockMixin implements DerivedBlock {
    @Shadow
    @Final
    protected BlockState baseState;

    @Override
    public BlockState getBaseBlockState() {
        return baseState;
    }
}
