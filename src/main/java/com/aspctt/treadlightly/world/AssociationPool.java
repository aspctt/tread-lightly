// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.aspctt.treadlightly.api.DerivedBlock;

/**
 * Searches every loaded map for something that matches a position, block, and substrate.
 * <p>
 * Order matters. An entity standing there wins first, since you are on top of the boat and not
 * the water. Then the block map for the exact state, then for the block it derives from, and
 * only then the primitive map, which knows nothing but the vanilla sound type.
 */
public final class AssociationPool {
    /** How far past a block's own edges an entity still counts as being stood on. */
    private static final double GOLEM_REACH = 0.5;

    private final LivingEntity entity;
    private final Solver solver;

    /**
     * Read through a supplier rather than held directly, because a resource reload replaces the
     * whole set and a pool outlives its entity's reloads. Snapshotted once per pass in
     * {@link #reset(BlockPos)} so the hot path is not chasing it on every lookup.
     */
    private final Supplier<Lookups> lookupsSource;
    private Lookups lookups;

    private boolean wasGolem;
    private SoundsKey association = SoundsKey.UNASSIGNED;

    /**
     * Entities that could be stood on, found once for the whole pass.
     * <p>
     * The original ran a world entity query inside every single lookup, and one pass performs
     * up to five of those across three vertically adjacent positions. Neither of the filters
     * depends on which position is being asked about, so one query covering the pass's whole
     * range plus a cheap box test per lookup gives the same answer for a fifth of the work.
     */
    private List<Entity> nearby = List.of();
    private boolean nearbyLoaded;
    private int baseX;
    private int baseY;
    private int baseZ;

    /**
     * Results already solved this tick, by foot position.
     * <p>
     * Both feet routinely land on the same block, and the search is deterministic within a
     * tick, so it need only run once. This lives here rather than on the solver because a pool
     * belongs to one entity: a cache shared across entities would hand back an association
     * carrying the wrong source entity, and resolved against the wrong entity's block map.
     */
    private final Long2ObjectOpenHashMap<Association> solved = new Long2ObjectOpenHashMap<>();
    private long solvedTick = Long.MIN_VALUE;

    public AssociationPool(LivingEntity entity, Supplier<Lookups> lookups, Solver solver) {
        this.entity = entity;
        this.lookupsSource = lookups;
        this.lookups = lookups.get();
        this.solver = solver;
    }

    /**
     * Starts a new pass centred on a position. The solver walks one block up and down from
     * here looking for carpets and fences, so that is the range covered.
     */
    public void reset(BlockPos base) {
        lookups = lookupsSource.get();
        wasGolem = false;
        nearbyLoaded = false;
        nearby = List.of();
        baseX = base.getX();
        baseY = base.getY();
        baseZ = base.getZ();
    }

    /** @return what was already solved for this position this tick, or null */
    @Nullable
    Association getSolved(long footPos, long tick) {
        if (tick != solvedTick) {
            solvedTick = tick;
            solved.clear();
        }
        return solved.get(footPos);
    }

    void putSolved(long footPos, Association association) {
        solved.put(footPos, association);
    }

    /**
     * Whether anything matched so far in this pass came from the golem map. Used to leave the
     * wet layer off, since standing on a boat in the rain is not standing in the water.
     */
    public boolean wasLastMatchGolem() {
        return wasGolem;
    }

    public Association findAssociation(double verticalOffsetAsMinus, boolean isRightFoot) {
        return solver.findAssociation(this, entity, verticalOffsetAsMinus, isRightFoot);
    }

    public Association findAssociation(BlockPos pos, String strategy) {
        return solver.findAssociation(this, entity, pos, strategy);
    }

    /**
     * @return the acoustics for this position, or {@link SoundsKey#UNASSIGNED} if nothing matched
     */
    public SoundsKey get(BlockPos pos, BlockState state, String substrate) {
        @Nullable SoundsKey golem = getForGolem(pos, substrate);
        if (golem != null) {
            return golem;
        }

        // Checked before deriving a base state, since air is both common and never a match.
        if (state.isAir()) {
            return SoundsKey.UNASSIGNED;
        }

        BlockState baseState = DerivedBlock.getBaseOf(state);

        if (getForState(state, substrate)
            || (!baseState.isAir() && (
                    getForState(baseState, substrate)
                || (!Substrates.isDefault(substrate) && getForState(baseState, Substrates.DEFAULT))
                || getForPrimitive(baseState, substrate, pos)
            ))
            || getForPrimitive(state, substrate, pos)
        ) {
            return association;
        }

        return SoundsKey.UNASSIGNED;
    }

    /** @return the match, or null when no entity at this position has one */
    @Nullable
    private SoundsKey getForGolem(BlockPos pos, String substrate) {
        loadNearby(pos);

        if (nearby.isEmpty()) {
            return null;
        }

        double minX = pos.getX() - GOLEM_REACH;
        double maxX = pos.getX() + 1 + GOLEM_REACH;
        double minZ = pos.getZ() - GOLEM_REACH;
        double maxZ = pos.getZ() + 1 + GOLEM_REACH;

        for (int i = 0; i < nearby.size(); i++) {
            Entity golem = nearby.get(i);

            if (!golem.getBoundingBox().intersects(minX, pos.getY(), minZ, maxX, pos.getY() + 1, maxZ)) {
                continue;
            }

            if ((association = lookups.golems().getAssociation(golem.getType(), substrate)).isEmitter()) {
                wasGolem = true;
                return association;
            }
        }

        return null;
    }

    private void loadNearby(BlockPos pos) {
        if (nearbyLoaded && Math.abs(pos.getY() - baseY) <= 1) {
            return;
        }

        if (nearbyLoaded) {
            // Outside the range the pass declared, which the solver should never ask for.
            // Re-centre rather than quietly miss whatever is being stood on.
            baseY = pos.getY();
        }
        nearbyLoaded = true;

        // Neither filter depends on the position, which is what makes one query for the whole
        // vertical range equivalent to one query per position.
        double standingOn = entity.getY() + 0.2F;
        nearby = entity.level().getEntities(entity,
                new AABB(baseX - GOLEM_REACH, baseY - 1, baseZ - GOLEM_REACH,
                         baseX + 1 + GOLEM_REACH, baseY + 2, baseZ + 1 + GOLEM_REACH),
                e -> !e.canBeCollidedWith() || e.getBoundingBox().maxY < standingOn);
    }

    private boolean getForState(BlockState state, String substrate) {
        return (association = lookups.blocksFor(entity.getType()).getAssociation(state, substrate)).isResult();
    }

    /**
     * The last resort: what the block's vanilla sound type says. This is what makes modded
     * blocks work without anyone writing entries for them.
     */
    private boolean getForPrimitive(BlockState state, String substrate, BlockPos pos) {
        // Wetness and foliage layer over a step. Nothing about a vanilla sound type says
        // whether a block is wet, so asking would only produce nonsense.
        if (Substrates.isSupplementary(substrate)) {
            return false;
        }
        // The position-aware overload, so a block that varies its sound by location answers
        // for where it actually is.
        SoundType sounds = state.getSoundType(entity.level(), pos, entity);
        return (association = lookups.primitives()
                .getAssociation(sounds.getStepSound(), PrimitiveLookup.getSubstrate(sounds))).isResult();
    }
}
