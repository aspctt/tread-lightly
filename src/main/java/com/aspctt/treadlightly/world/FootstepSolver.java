// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.aspctt.treadlightly.compat.ContraptionCollidable;
import com.aspctt.treadlightly.sound.AcousticVolumes;
import com.aspctt.treadlightly.util.PlayerUtil;

/**
 * Works out what an entity is standing on.
 * <p>
 * The search runs outwards. It starts under the foot, allowing for which foot it is and which
 * way the entity faces, and looks for a carpet on top, the block itself, then a fence below.
 * Failing all of that it tries the surrounding columns, because a player walking along the edge
 * of a block is often technically over air. Only when everything fails does it fall back to
 * treating a liquid as the surface.
 */
public class FootstepSolver implements Solver {
    /** Feet sit slightly inside the block, so a trapdoor underfoot is still found. */
    private static final double TRAP_DOOR_OFFSET = 0.1;

    /** How far to either side of centre a foot lands, before scaling. */
    private static final float FOOT_OFFSET = 0.2F;

    /** Vertical speed below which a step is treated as a bounce rather than a footfall. */
    private static final double BOUNCE_THRESHOLD = 0.004;

    private final AcousticVolumes volumes;

    /** Supplied rather than held, since a resource reload replaces the whole set. */
    private final Supplier<Lookups> lookups;

    public FootstepSolver(Supplier<Lookups> lookups, AcousticVolumes volumes) {
        this.lookups = lookups;
        this.volumes = volumes;
    }

    /**
     * Reads a block, letting an entity standing on a moving contraption see the contraption's
     * blocks, and letting a block that presents itself as another one be heard as that one.
     */
    private BlockState getBlockStateAt(LivingEntity entity, BlockPos pos) {
        Level level = entity.level();
        BlockState state = level.getBlockState(pos);

        if (state.isAir() && entity instanceof ContraptionCollidable collidable) {
            state = collidable.getCollidedStateAt(pos);
        }

        return state.getAppearance(level, pos, Direction.UP, state, pos);
    }

    private AABB getCollider(LivingEntity entity) {
        AABB collider = entity.getBoundingBox();

        // Dropped to the bottom of the block so a carpet resting on a fence is still detected.
        collider = collider.move(0, -(collider.minY - Math.floor(collider.minY)), 0);
        collider = collider.inflate(0.1);

        if (entity.isSprinting()) {
            // A sprinting entity covers more ground between steps, so reach further for
            // something to have stepped on.
            collider = collider.inflate(0.3, 0.5, 0.3);
        }
        return collider;
    }

    private boolean checkCollision(Level level, BlockState state, BlockPos pos, AABB collider) {
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            shape = state.getShape(level, pos);
        }
        // An empty shape counts as touching: plants and snow layers have no collision but are
        // very much underfoot.
        return shape.isEmpty() || shape.bounds().move(pos).intersects(collider);
    }

    @Override
    public Association findAssociation(AssociationPool associations, LivingEntity entity, BlockPos pos, String strategy) {
        if (!MESSY_FOLIAGE_STRATEGY.equals(strategy)) {
            return Association.NOT_EMITTER;
        }

        BlockPos above = pos.above();
        BlockState state = getBlockStateAt(entity, above);

        Lookup<BlockState> lookup = lookups.get().blocksFor(entity.getType());
        SoundsKey foliage = lookup.getAssociation(state, Substrates.FOLIAGE);

        // The block's own sound is discarded here. This is only about plants brushing the legs,
        // and only over ground the pack marked as messy enough for them to rustle against.
        if (foliage.isEmitter() && lookup.getAssociation(state, Substrates.MESSY) == SoundsKey.MESSY_GROUND) {
            return Association.of(state, above, entity, false, SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER, foliage);
        }

        return Association.NOT_EMITTER;
    }

    @Override
    public Association findAssociation(AssociationPool associations, LivingEntity entity,
                                       double verticalOffsetAsMinus, boolean isRightFoot) {
        double rot = Math.toRadians(Mth.wrapDegrees(entity.getYRot()));
        Vec3 position = entity.position();

        float feetDistanceToCenter = FOOT_OFFSET * (isRightFoot ? -1 : 1) * PlayerUtil.getScale(entity);

        BlockPos footPos = BlockPos.containing(
            position.x + Math.cos(rot) * feetDistanceToCenter,
            entity.getBoundingBox().min(Axis.Y) - TRAP_DOOR_OFFSET - verticalOffsetAsMinus,
            position.z + Math.sin(rot) * feetDistanceToCenter
        );

        // Remote players have no reliable velocity, so this test would reject all their steps.
        if (!(entity instanceof RemotePlayer)) {
            Vec3 velocity = entity.getDeltaMovement();

            if (velocity.lengthSqr() != 0 && Math.abs(velocity.y) < BOUNCE_THRESHOLD) {
                return Association.NOT_EMITTER;
            }
        }

        // Cached on the pool, which belongs to this one entity. One solver serves them all,
        // so caching here would mix entities up.
        long key = footPos.asLong();
        @Nullable Association cached = associations.getSolved(key, entity.level().getGameTime());
        if (cached != null) {
            return cached;
        }

        AABB collider = getCollider(entity);
        BlockPos.MutableBlockPos mutableFootPos = footPos.mutable();

        // A very large entity's feet land well outside its centre block, so search the ground
        // it actually covers rather than one column.
        if (feetDistanceToCenter > 1) {
            for (BlockPos underfootPos : BlockPos.withinManhattan(footPos, (int) feetDistanceToCenter, 2, (int) feetDistanceToCenter)) {
                mutableFootPos.set(underfootPos);
                Association found = findAssociation(associations, entity, collider, underfootPos, mutableFootPos);
                if (found.isResult()) {
                    associations.putSolved(key, found);
                    return found;
                }
            }
        }

        Association found = findAssociation(associations, entity, collider, footPos, mutableFootPos);
        associations.putSolved(key, found);
        return found;
    }

    /**
     * Tries the foot's own column, then the surrounding ones, then gives up and lets a liquid
     * answer for itself.
     */
    @SuppressWarnings("deprecation") // BlockState.liquid(), which has no replacement.
    private Association findAssociation(AssociationPool associations, LivingEntity entity, AABB collider,
                                        BlockPos originalFootPos, BlockPos.MutableBlockPos pos) {
        Association association = findAssociation(associations, entity, pos, collider);

        // A liquid is not accepted yet. Something solid nearby is a better answer than the
        // water you are wading through.
        if (association.isResult() && !association.state().liquid()) {
            return association;
        }

        final double radius = 0.4;
        int[] xValues = {
                Mth.floor(collider.min(Axis.X) - radius),
                pos.getX(),
                Mth.floor(collider.max(Axis.X) + radius)
        };
        int[] zValues = {
                Mth.floor(collider.min(Axis.Z) - radius),
                pos.getZ(),
                Mth.floor(collider.max(Axis.Z) + radius)
        };

        for (int x : xValues) {
            for (int z : zValues) {
                if (x == originalFootPos.getX() && z == originalFootPos.getZ()) {
                    continue;
                }
                pos.set(x, originalFootPos.getY(), z);

                association = findAssociation(associations, entity, pos, collider);
                if (association.isResult() && !association.state().liquid()) {
                    return association;
                }
            }
        }

        pos.set(originalFootPos);
        BlockState state = getBlockStateAt(entity, pos);

        if (state.liquid()) {
            SoundsKey sounds = state.getFluidState().is(FluidTags.LAVA) ? SoundsKey.LAVAFINE : SoundsKey.WATERFINE;
            return Association.of(state, pos.below(), entity, false, sounds, SoundsKey.NON_EMITTER, SoundsKey.NON_EMITTER);
        }

        return association;
    }

    /**
     * Resolves one column: a carpet on top, else the block itself, else a fence below, plus the
     * wet and foliage layers that go over whichever of those answered.
     */
    private Association findAssociation(AssociationPool associations, LivingEntity entity,
                                        BlockPos.MutableBlockPos pos, AABB collider) {
        associations.reset(pos);

        Level level = entity.level();
        BlockState target = getBlockStateAt(entity, pos);

        pos.move(Direction.UP);
        final boolean hasRain = level.isRainingAt(pos);
        BlockState carpet = getBlockStateAt(entity, pos);
        VoxelShape shape = carpet.getShape(level, pos);
        boolean isValidCarpet = !shape.isEmpty() && shape.max(Axis.Y) < 0.3F;

        SoundsKey association = SoundsKey.UNASSIGNED;
        SoundsKey foliage = SoundsKey.UNASSIGNED;
        SoundsKey wet = SoundsKey.UNASSIGNED;

        if (isValidCarpet && (association = associations.get(pos, carpet, Substrates.CARPET)).isEmitter()
                && !association.isSilent()) {
            // The carpet is what was stepped on, so the frame of reference moves up with it.
            target = carpet;
        } else {
            // A carpet mapped to silence does not stop the search: solving continues into the
            // surface actually being walked on.
            pos.move(Direction.DOWN);
            association = associations.get(pos, target, Substrates.DEFAULT);

            if (!association.isEmitter() || !association.isResult()) {
                pos.move(Direction.DOWN);
                BlockState fence = getBlockStateAt(entity, pos);

                if (checkCollision(level, fence, pos, collider)
                        && (association = associations.get(pos, fence, Substrates.FENCE)).isResult()) {
                    carpet = target;
                    target = fence;
                } else {
                    pos.move(Direction.UP);
                }
            }

            // Bare feet, or moving fast enough to disturb the plants either way.
            if (volumes.foliage() > 0
                    && (entity.getItemBySlot(EquipmentSlot.FEET).isEmpty() || entity.isSprinting())
                    && association.isEmitter()
                    && carpet.getCollisionShape(level, pos).isEmpty()) {
                // Foliage over a block mapped to silence stays silent, and this is skipped
                // entirely when the carpet itself already answered.
                pos.move(Direction.UP);
                foliage = associations.get(pos, carpet, Substrates.FOLIAGE);
                pos.move(Direction.DOWN);
            }
        }

        // Something too small to actually be standing on does not count.
        if (association.isResult() && !checkCollision(level, target, pos, collider)) {
            association = SoundsKey.UNASSIGNED;
        }

        // Wet only when open to the sky in rain, or genuinely in or on water. Not when the
        // match came from an entity, since the deck of a boat is dry.
        if (association.isEmitter() && (hasRain
                || (!associations.wasLastMatchGolem() && (
                   (target.getFluidState().is(FluidTags.WATER) && !target.isFaceSturdy(level, pos, Direction.UP))
                || (carpet.getFluidState().is(FluidTags.WATER) && !carpet.isFaceSturdy(level, pos, Direction.UP))
        )))) {
            wet = associations.get(pos, target, Substrates.WET);
        }

        return Association.of(target, pos, entity,
                associations.wasLastMatchGolem() && entity.onGround(), association, wet, foliage);
    }
}
