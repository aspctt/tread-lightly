// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ArmorItem;

import net.neoforged.neoforge.common.NeoForgeMod;

import com.aspctt.treadlightly.config.Variator;
import com.aspctt.treadlightly.mixin.LivingEntityAccessor;
import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.util.Lerp;
import com.aspctt.treadlightly.util.PlayerUtil;
import com.aspctt.treadlightly.world.Association;
import com.aspctt.treadlightly.world.AssociationPool;
import com.aspctt.treadlightly.world.BiomeVarianceLookup.BiomeVariance;
import com.aspctt.treadlightly.world.Lookups;
import com.aspctt.treadlightly.world.Solver;
import com.aspctt.treadlightly.world.SoundsKey;
import com.aspctt.treadlightly.world.Substrates;

/**
 * Decides when an entity walking on the ground has taken a step, and plays it.
 * <p>
 * Steps come from distance travelled rather than from a timer, so an entity that speeds up
 * takes its steps closer together without anything having to track its gait. Everything else
 * here is the awkward cases: stairs and ladders need their own stride lengths, jumping and
 * landing are their own events, standing still still makes the occasional shuffle, and walking
 * through undergrowth rustles independently of the feet.
 */
public class TerrestrialStepSoundGenerator implements StepSoundGenerator {
    /** How fast the biome trim eases towards a new biome's values, per frame. */
    private static final float BIOME_EASE_RATE = 0.01F;

    /** Height change that counts as a stair rather than a fall. */
    private static final double STAIR_THRESHOLD = 0.4;

    /** Gap between checks for plants brushing the legs, in milliseconds. */
    private static final long BRUSH_INTERVAL = 100;

    protected final LivingEntity entity;
    protected final StepSoundContext context;
    private final Modifier<TerrestrialStepSoundGenerator> modifier;
    protected final MotionTracker motionTracker = new MotionTracker(this);
    protected final AssociationPool associations;

    private final RandomSource random = RandomSource.create();
    private final Lerp biomePitch = new Lerp();
    private final Lerp biomeVolume = new Lerp();

    /** Distance travelled at the last step, which the next step is measured from. */
    private float lastStepDistance;
    private double yPosition;

    private boolean isAirborne;
    private double lastFallDistance;

    private float lastReference;
    private boolean isImmobile;
    private long timeImmobile;
    private long immobilePlayback;
    private int immobileInterval;

    private boolean isRightFoot;

    /** Last frame's horizontal direction, for noticing that the entity turned on the spot. */
    private double xMovec;
    private double zMovec;
    private boolean wasTurning;

    private boolean stepThisFrame;
    private boolean isMessyFoliage;
    private long brushesTime;

    public TerrestrialStepSoundGenerator(LivingEntity entity, StepSoundContext context,
                                         Modifier<TerrestrialStepSoundGenerator> modifier) {
        this.entity = entity;
        this.context = context;
        this.modifier = modifier;
        this.associations = new AssociationPool(entity, context.lookups(), context.solver());
    }

    Variator variator() {
        return context.lookups().get().variator();
    }

    private Lookups lookups() {
        return context.lookups().get();
    }

    @Override
    public float getLocalPitch(float tickDelta) {
        return biomePitch.get(tickDelta);
    }

    @Override
    public float getLocalVolume(float tickDelta) {
        return biomeVolume.get(tickDelta);
    }

    @Override
    public MotionTracker getMotionTracker() {
        return motionTracker;
    }

    @Override
    public void generateFootsteps() {
        BiomeVariance variance = entity.level().getBiome(entity.blockPosition())
                .unwrapKey()
                .map(ResourceKey::location)
                .map(key -> lookups().biomes().lookup(key))
                .orElse(BiomeVariance.DEFAULT);

        biomePitch.update(variance.pitch(), BIOME_EASE_RATE);
        biomeVolume.update(variance.volume(), BIOME_EASE_RATE);

        motionTracker.simulateMotionData(entity);
        simulateFootsteps();
        simulateAirborne();
        simulateBrushes();
        simulateStationary();
        lastFallDistance = motionTracker.getFallDistance();
    }

    /** An entity standing still still shifts its weight about now and then. */
    private void simulateStationary() {
        if (isImmobile && (entity.onGround() || !entity.isUnderWater()) && playbackImmobile()) {
            Association association = associations.findAssociation(0d, isRightFoot);

            if (association.isResult() && !association.isSilent()) {
                playStep(association, State.STAND);
            }
        }
    }

    private boolean playbackImmobile() {
        long now = System.currentTimeMillis();
        if (now - immobilePlayback <= immobileInterval) {
            return false;
        }

        Variator variator = variator();
        immobilePlayback = now;
        immobileInterval = variator.IMMOBILE_INTERVAL_MIN
                + random.nextInt(Math.max(1, variator.IMMOBILE_INTERVAL_MAX - variator.IMMOBILE_INTERVAL_MIN));
        return true;
    }

    /** @return true when the entity has just started moving again after standing long enough */
    private boolean updateImmobileState(float reference) {
        float diff = lastReference - reference;
        lastReference = reference;

        if (!isImmobile && diff == 0F) {
            timeImmobile = System.currentTimeMillis();
            isImmobile = true;
        } else if (isImmobile && diff != 0F) {
            isImmobile = false;
            return System.currentTimeMillis() - timeImmobile > variator().IMMOBILE_DURATION;
        }

        return false;
    }

    private void simulateFootsteps() {
        final float distance = motionTracker.getDistanceTraveled();
        stepThisFrame = false;

        // The counter resets when an entity is removed and re-added, so does this.
        if (lastStepDistance > distance) {
            lastStepDistance = 0;
        }

        double movX = motionTracker.getMotionX();
        double movZ = motionTracker.getMotionZ();

        // A negative dot product against last frame's direction means the entity reversed,
        // which is a scuff rather than a step.
        boolean turning = (movX * xMovec + movZ * zMovec) < 0.001F;
        if (wasTurning != turning) {
            wasTurning = turning;

            if (turning && variator().PLAY_WANDER && !hasStoppingConditions()) {
                playStep(associations.findAssociation(0, isRightFoot), State.WANDER);
            }
        }
        xMovec = movX;
        zMovec = movZ;

        float travelled = distance - lastStepDistance;
        if (updateImmobileState(distance) && !entity.onClimbable()) {
            travelled = 0;
            lastStepDistance = distance;
        }

        if (entity.onGround() || entity.isUnderWater() || entity.onClimbable()) {
            simulateStep(distance, travelled);
        }

        if (entity.onGround()) {
            // Only sampled on the ground, otherwise descending stairs is measured mid-air
            // between two steps and reads as a fall.
            yPosition = entity.getY();
        }
    }

    private void simulateStep(float distance, float travelled) {
        Variator variator = variator();
        @Nullable State event = null;
        float strideLength;

        if (entity.onClimbable() && !entity.onGround()) {
            strideLength = variator.DISTANCE_LADDER;
        } else if (!entity.isUnderWater() && Math.abs(yPosition - entity.getY()) > STAIR_THRESHOLD) {
            if (yPosition < entity.getY()) {
                strideLength = variator.DISTANCE_STAIR;
                event = motionTracker.pickState(entity, State.UP, State.UP_RUN);
            } else if (!entity.isShiftKeyDown()) {
                // Negative, so going down stairs always steps rather than waiting for distance.
                strideLength = -1F;
                event = motionTracker.pickState(entity, State.DOWN, State.DOWN_RUN);
            } else {
                strideLength = variator.DISTANCE_HUMAN;
            }
        } else {
            strideLength = variator.DISTANCE_HUMAN;
        }

        if (event == null) {
            event = motionTracker.pickState(entity, State.WALK, State.RUN);
        }

        // A galloping horse covers ground far faster than its legs suggest.
        if (entity instanceof AbstractHorse && motionTracker.getHorizontalSpeed() > 0.1) {
            strideLength *= 3;
        }

        strideLength = modifier.reevaluateDistance(event, strideLength);

        // A larger entity takes proportionally longer strides.
        strideLength *= ((PlayerUtil.getScale(entity) - 1) * 0.6F) + 1;

        if (travelled > strideLength) {
            produceStep(event);
            modifier.stepped(this, entity, event);
            lastStepDistance = distance;
        }
    }

    public final void produceStep(@Nullable State event) {
        produceStep(event, 0d);
    }

    public final void produceStep(@Nullable State event, double verticalOffsetAsMinus) {
        if (event == null) {
            event = motionTracker.pickState(entity, State.WALK, State.RUN);
        }

        if (hasStoppingConditions()) {
            float volume = Math.min(1, (float) entity.getDeltaMovement().length() * 0.35F);
            // NeoForge's fluid type rather than the deprecated tag check, so a modded fluid
            // that behaves like lava is treated like it.
            boolean submerged = entity.isUnderWater() || entity.isEyeInFluidType(NeoForgeMod.LAVA_TYPE.value());

            lookups().acoustics().playAcoustic(entity,
                    entity.isInWater() ? SoundsKey.SWIM_WATER : SoundsKey.SWIM_LAVA,
                    submerged ? State.SWIM : event,
                    Options.singular("gliding_volume", volume)
                        // Other people's splashing is much quieter than your own.
                        .and(Options.singular("volume_scale", PlayerUtil.isClientPlayer(entity) ? 1 : 0.125F))
            );
            playStep(associations.findAssociation(entity.blockPosition().below(), Solver.MESSY_FOLIAGE_STRATEGY), event);
        } else {
            if (!entity.isDiscrete() || event.isExtraLoud()) {
                playStep(associations.findAssociation(verticalOffsetAsMinus, isRightFoot), event);
            }
            isRightFoot = !isRightFoot;
        }

        stepThisFrame = true;
    }

    /** In water or lava, where the surface underfoot is not what is heard. */
    private boolean hasStoppingConditions() {
        return entity.isInWater() || entity.isInLava();
    }

    private void simulateAirborne() {
        if ((entity.onGround() || entity.onClimbable()) == isAirborne) {
            isAirborne = !isAirborne;
            simulateJumpingLanding();
        }
    }

    /** Whether the entity meant to leave the ground, as opposed to walking off an edge. */
    private boolean isJumping() {
        return ((LivingEntityAccessor) entity).treadlightly$isJumping();
    }

    /** Remote players sit a block higher than they report, so look one lower for their feet. */
    private double getOffsetMinus() {
        return entity instanceof RemotePlayer ? 1 : 0;
    }

    private void simulateJumpingLanding() {
        if (hasStoppingConditions()) {
            return;
        }

        if (isAirborne && isJumping()) {
            simulateJumping();
        } else if (!isAirborne) {
            simulateLanding();
        }
    }

    private void simulateJumping() {
        Variator variator = variator();
        if (!variator.EVENT_ON_JUMP) {
            return;
        }

        if (motionTracker.getHorizontalSpeed() < variator.SPEED_TO_JUMP_AS_MULTIFOOT) {
            // A standing jump leaves the ground on both feet.
            playMultifoot(getOffsetMinus() + 0.4d, State.WANDER);
        } else {
            // A running jump goes off one, and the same foot lands first, so it is not toggled.
            playSinglefoot(getOffsetMinus() + 0.4d, State.JUMP);
        }
    }

    private void simulateLanding() {
        if (lastFallDistance <= 0) {
            return;
        }

        if (lastFallDistance > variator().LAND_HARD_DISTANCE_MIN) {
            // Anything landing from height comes down on both feet.
            playMultifoot(getOffsetMinus(), State.LAND);
        } else if (!stepThisFrame && !entity.isShiftKeyDown()) {
            playSinglefoot(getOffsetMinus(), motionTracker.pickState(entity, State.CLIMB, State.CLIMB_RUN));
            isRightFoot = !isRightFoot;
        }
    }

    /**
     * Plants brushing against the legs, which happens on its own schedule rather than with the
     * feet, so that wading through a field rustles continuously instead of once per step.
     */
    private void simulateBrushes() {
        if (brushesTime > System.currentTimeMillis()) {
            return;
        }
        brushesTime = System.currentTimeMillis() + BRUSH_INTERVAL;

        if (motionTracker.isStationary()
                || entity.isShiftKeyDown()
                || !entity.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
            return;
        }

        Association association = associations.findAssociation(BlockPos.containing(
            entity.getX(),
            entity.getRootVehicle().getY() - 0.1D - (entity.onGround() ? 0 : 0.25D),
            entity.getZ()
        ), Solver.MESSY_FOLIAGE_STRATEGY);

        if (!association.isSilent()) {
            // Only on entering the undergrowth, not for every check while inside it.
            if (!isMessyFoliage) {
                isMessyFoliage = true;
                playStep(association, State.WALK);
            }
        } else {
            isMessyFoliage = false;
        }
    }

    protected void playStep(Association association, State eventType) {
        if (context.settings().footwearEnabled()
                && entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ArmorItem boots) {
            SoundsKey bootSound = lookups().primitives()
                    .getAssociation(boots.getEquipSound().value(), Substrates.DEFAULT);

            if (bootSound.isEmitter()) {
                // The surface is still heard, but under the boot rather than instead of it.
                lookups().acoustics().playStep(association, eventType, Options.singular("volume_percentage", 0.5F));
                lookups().acoustics().playAcoustic(entity, bootSound, eventType, Options.EMPTY);
                return;
            }
        }

        lookups().acoustics().playStep(association, eventType, Options.EMPTY);
    }

    private void playSinglefoot(double verticalOffsetAsMinus, State eventType) {
        Association association = associations.findAssociation(verticalOffsetAsMinus, isRightFoot);

        if (!association.isResult()) {
            // Nothing directly underfoot, so try a block lower before giving up.
            association = associations.findAssociation(verticalOffsetAsMinus + 1, isRightFoot);
        }

        playStep(association, eventType);
    }

    private void playMultifoot(double verticalOffsetAsMinus, State eventType) {
        Association leftFoot = associations.findAssociation(verticalOffsetAsMinus, false);
        Association rightFoot = associations.findAssociation(verticalOffsetAsMinus, true);

        if (leftFoot.isResult() && leftFoot.dataEquals(rightFoot)) {
            // Both feet on the same thing is one sound, not two on top of each other.
            if (isRightFoot) {
                leftFoot = Association.NOT_EMITTER;
            } else {
                rightFoot = Association.NOT_EMITTER;
            }
        }

        playStep(leftFoot, eventType);
        playStep(rightFoot, eventType);
    }
}
