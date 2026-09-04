// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import com.aspctt.treadlightly.sound.StepSoundSource;

/**
 * Silences the game's own footstep for any entity this mod is about to give one to, so the two
 * are not heard on top of each other.
 * <p>
 * There is no NeoForge event covering this: the method is protected and called from movement,
 * so a mixin is the only way in.
 */
@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void treadlightly$cancelVanillaStep(BlockPos pos, BlockState state, CallbackInfo info) {
        if (this instanceof StepSoundSource source && source.isStepBlocked()) {
            info.cancel();
        }
    }
}
