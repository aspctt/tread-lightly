// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.State;

/**
 * Turns an even two-legged gait into a four-legged one.
 * <p>
 * A horse does not place its feet at even intervals. Hooves fall in pairs with an uneven gap
 * between them, which is what makes a canter sound like a canter rather than a fast walk. Every
 * third step is doubled up, and the spacing between them is varied so the pattern does not
 * become a metronome.
 */
public class QuadrupedModifier extends Modifier<TerrestrialStepSoundGenerator> {
    private static final float WALK_OVERALL_MULTIPLIER = 1.85F / 2;
    private static final float WALK_SPACING = 0.2F;

    /** Which of the three positions in the gait cycle the next step falls on. */
    private int hoof;

    private float nextWalkDistanceMultiplier = 0.05F;

    private final RandomSource random = RandomSource.create();

    @Override
    protected void stepped(TerrestrialStepSoundGenerator generator, LivingEntity entity, State event) {
        if (hoof == 0 || hoof == 2) {
            nextWalkDistanceMultiplier = random.nextFloat();
        }

        hoof = (hoof + 1) % 3;

        if (event == State.WALK) {
            // The second hoof of the pair, following close behind the first.
            generator.produceStep(event);
        }
    }

    @Override
    protected float reevaluateDistance(State event, float distance) {
        if (event == State.WALK) {
            float spacing = nextWalkDistanceMultiplier;
            spacing *= spacing;
            spacing *= WALK_SPACING;

            // The cycle only runs 0 to 2, so the original's extra test for 3 was dead.
            return hoof == 1
                    ? distance * spacing * WALK_OVERALL_MULTIPLIER
                    : distance * (1 - spacing) * WALK_OVERALL_MULTIPLIER;
        }

        if (event == State.RUN) {
            // A gallop bunches three beats together and then leaves a gap.
            return hoof == 0 ? distance * 0.8F : distance * 0.3F;
        }

        return distance;
    }
}
