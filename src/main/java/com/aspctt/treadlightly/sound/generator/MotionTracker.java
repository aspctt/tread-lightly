// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.aspctt.treadlightly.config.Variator;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.util.PlayerUtil;

/**
 * Works out how fast an entity is moving and how far it has walked.
 * <p>
 * Only the player at the keyboard reports any of this honestly. Everyone else arrives as a
 * stream of positions, so their speed is the difference between frames and their distance
 * travelled has to be accumulated here. Getting this wrong is what makes other players' steps
 * sound wrong in multiplayer, so the two paths are kept deliberately separate.
 */
public class MotionTracker {
    /** Vanilla's per-tick gravity, added back so a grounded entity reads as vertically still. */
    private static final double GRAVITY_PER_TICK = 0.0784000015258789D;

    private double lastX;
    private double lastY;
    private double lastZ;

    private double motionX;
    private double motionY;
    private double motionZ;

    private float distanceTraveled;
    private double fallDistance;

    private final TerrestrialStepSoundGenerator generator;

    public MotionTracker(TerrestrialStepSoundGenerator generator) {
        this.generator = generator;
    }

    public double getMotionX() {
        return motionX;
    }

    public double getMotionY() {
        return motionY;
    }

    public double getMotionZ() {
        return motionZ;
    }

    /** Squared, because every caller only compares it against a threshold. */
    public double getHorizontalSpeed() {
        return motionX * motionX + motionZ * motionZ;
    }

    public boolean isStationary() {
        return motionX == 0 && motionZ == 0;
    }

    public float getDistanceTraveled() {
        return distanceTraveled;
    }

    public double getFallDistance() {
        return fallDistance;
    }

    /** Fills in what the client does not receive for entities other than its own player. */
    public void simulateMotionData(LivingEntity entity) {
        if (PlayerUtil.isClientPlayer(entity)) {
            motionX = entity.getDeltaMovement().x;
            motionY = entity.getDeltaMovement().y;
            motionZ = entity.getDeltaMovement().z;
            distanceTraveled = entity.walkDist;
            fallDistance = entity.fallDistance;
        } else {
            // No velocity is sent for anyone else, so it is inferred from where they were.
            motionX = entity.getX() - lastX;
            lastX = entity.getX();

            motionY = entity.getY() - lastY;
            if (entity.onGround()) {
                // Standing on the ground still accrues downward velocity that is cancelled on
                // the next tick. Adding it back leaves a still entity reading as still.
                motionY += GRAVITY_PER_TICK;
            }
            lastY = entity.getY();

            motionZ = entity.getZ() - lastZ;
            lastZ = entity.getZ();
        }

        if (entity instanceof RemotePlayer other) {
            accumulateSimulatedDistance(other, other.getAbilities().flying);
        }

        if (!(entity instanceof Player)) {
            distanceTraveled += (float) Math.sqrt(getHorizontalSpeed()) * 0.6F;
        }
    }

    /**
     * Chooses between a walking and a running variant of the same event.
     * <p>
     * Other players have no usable speed, so their sprint flag is the only thing to go on.
     */
    public State pickState(LivingEntity entity, State walk, State run) {
        if (entity instanceof Player && !PlayerUtil.isClientPlayer(entity)) {
            return entity.isSprinting() ? run : walk;
        }
        return getHorizontalSpeed() > generator.variator().SPEED_TO_RUN ? run : walk;
    }

    /** How far into a run the entity is, from 0 to 1, used to lift the volume as it speeds up. */
    public float getSpeedScalingRatio(LivingEntity entity) {
        Variator variator = generator.variator();
        double relativeSpeed = getHorizontalSpeed() + (motionY * motionY) - variator.RUNNING_RAMPUP_BEGIN;
        double maxSpeed = variator.RUNNING_RAMPUP_END - variator.RUNNING_RAMPUP_BEGIN;
        return (float) Mth.clamp(relativeSpeed / maxSpeed, 0, 1);
    }

    private void accumulateSimulatedDistance(LivingEntity entity, boolean flying) {
        if (motionX != 0 || motionZ != 0) {
            distanceTraveled += Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ) * 0.8;
        } else {
            distanceTraveled += Math.sqrt(motionX * motionX + motionZ * motionZ) * 0.8;
        }

        if (entity.onGround() || entity.isPassenger() || flying || motionY > 0) {
            fallDistance = 0;
        } else if (motionY < 0) {
            fallDistance -= motionY;
        }
    }
}
