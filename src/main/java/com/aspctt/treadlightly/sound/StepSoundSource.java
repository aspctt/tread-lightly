// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound;

import java.util.Optional;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.TreadLightly;

import com.aspctt.treadlightly.sound.generator.Locomotion;
import com.aspctt.treadlightly.sound.generator.StepSoundGenerator;

/**
 * An entity that can be given footsteps. Implemented on {@link LivingEntity} by mixin.
 * <p>
 * A field on the entity rather than a data attachment: this is transient client-side state with
 * nothing to serialise, and it is read for every tracked entity every frame.
 */
public interface StepSoundSource {
    Optional<StepSoundGenerator> getStepGenerator(SoundEngine engine);

    /** Whether the game's own footstep for this entity should be suppressed. */
    boolean isStepBlocked();

    /** Holds one entity's generator, rebuilt if its gait changes. */
    final class Container implements StepSoundSource {
        private final LivingEntity entity;

        private Locomotion locomotion;
        private Optional<StepSoundGenerator> generator;

        public Container(LivingEntity entity) {
            this.entity = entity;
        }

        @Override
        public Optional<StepSoundGenerator> getStepGenerator(SoundEngine engine) {
            Locomotion current = engine.getLookups().locomotions().lookup(entity);

            // Rebuilt when the gait changes, which happens when a pack is reloaded or the
            // player picks a different stance.
            if (generator == null || current != locomotion) {
                locomotion = current;
                generator = current.supplyGenerator(entity, engine.getStepContext());
            }
            return generator;
        }

        @Override
        public boolean isStepBlocked() {
            return TreadLightly.engine().isStepBlocked(entity, this);
        }
    }
}
