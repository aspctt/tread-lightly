// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.SoundEngine;
import com.aspctt.treadlightly.sound.StepSoundSource;
import com.aspctt.treadlightly.sound.generator.StepSoundGenerator;

/**
 * Gives every living entity somewhere to keep its footstep generator.
 * <p>
 * A field rather than a data attachment: this is transient client state with nothing to
 * serialise, read for every tracked entity every tick, and it should not survive the entity.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin implements StepSoundSource {
    @Unique
    private final StepSoundSource treadlightly$source = new StepSoundSource.Container((LivingEntity) (Object) this);

    @Override
    public Optional<StepSoundGenerator> getStepGenerator(SoundEngine engine) {
        return treadlightly$source.getStepGenerator(engine);
    }

    @Override
    public boolean isStepBlocked() {
        return treadlightly$source.isStepBlocked();
    }
}
