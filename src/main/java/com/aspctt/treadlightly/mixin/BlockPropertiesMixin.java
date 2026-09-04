// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.aspctt.treadlightly.api.DerivedBlock;

/**
 * Remembers which block a set of properties was copied from.
 * <p>
 * Modded blocks are almost always declared by copying the properties of a vanilla one, so this
 * is the cheapest reliable signal of what a block is made of. Recording it here means oak-like
 * stairs from any mod can borrow oak's sound without anyone writing an entry for them.
 */
@Mixin(BlockBehaviour.Properties.class)
abstract class BlockPropertiesMixin implements DerivedBlock.Settings {
    @Unique
    @Nullable
    private Block treadlightly$baseBlock;

    @Override
    @Nullable
    public Block getBaseBlock() {
        return treadlightly$baseBlock;
    }

    @Override
    public void setBaseBlock(Block block) {
        this.treadlightly$baseBlock = block;
    }

    /**
     * The full copy delegates to this one, so injecting here covers both without needing to
     * know which a given mod reached for.
     */
    @SuppressWarnings("deprecation")
    @Inject(method = "ofLegacyCopy", at = @At("RETURN"))
    private static void treadlightly$recordSource(BlockBehaviour source,
                                                  CallbackInfoReturnable<BlockBehaviour.Properties> info) {
        if (source instanceof Block block) {
            ((DerivedBlock.Settings) info.getReturnValue()).setBaseBlock(block);
        }
    }
}
