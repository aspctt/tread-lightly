// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.State;

/**
 * Adjusts a generator's stride pattern for a body plan. The default does nothing, which is
 * right for anything walking on two legs.
 */
public class Modifier<T extends StepSoundGenerator> {
    /** Called after a step has played, for anything that needs to follow up with another. */
    protected void stepped(T generator, LivingEntity entity, State event) {
    }

    /** Adjusts how far the entity must travel before its next step. */
    protected float reevaluateDistance(State event, float distance) {
        return distance;
    }
}
