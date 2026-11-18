// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.compat;

import java.util.UUID;

//? if <1.21.11
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
//? if <1.21.11 {
import net.neoforged.fml.ModList;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
//?}

/**
 * Sable sub-levels, which are the moving ships of Create Aeronautics and the rest of the
 * Simulated project.
 * <p>
 * A sub-level keeps its blocks in a plot far from where it is drawn, so the world under someone
 * standing on deck is air. Points are moved into that plot instead. There the level holds the
 * real blocks, and someone standing still on a moving ship is standing still. Sable only exists
 * for 1.21.1, so on every other version nothing is ever on one.
 */
public final class SubLevels {
    //? if <1.21.11 {
    /**
     * Sable bundles Companion and installs its own implementation behind it. 1.5.0 is the first
     * Companion with the call used here, and Sable 1.0 bundled 1.4, so with that the ships stay
     * silent rather than crash.
     */
    private static final boolean IS_AVAILABLE = ModList.get().getModContainerById("sablecompanion")
            .map(container -> container.getModInfo().getVersion().compareTo(new DefaultArtifactVersion("1.5.0")) >= 0)
            .orElse(false);
    //?}

    private SubLevels() {
    }

    /** The sub-level the entity is standing on, or null when it is on the world itself. */
    public static @Nullable UUID frameOf(Entity entity) {
        //? if <1.21.11 {
        return IS_AVAILABLE ? Sable.frameOf(entity) : null;
        //?} else
        //return null;
    }

    /** The point as seen from the sub-level the entity is standing on. Off one it is unchanged. */
    public static Vec3 toFrame(Entity entity, Vec3 point) {
        //? if <1.21.11 {
        return IS_AVAILABLE ? Sable.toFrame(entity, point) : point;
        //?} else
        //return point;
    }

    //? if <1.21.11 {
    /** Kept apart so Companion's classes are only loaded once it is known to be installed. */
    private static final class Sable {
        static @Nullable UUID frameOf(Entity entity) {
            @Nullable SubLevelAccess subLevel = SableCompanion.INSTANCE.getTrackingSubLevel(entity);
            return subLevel == null ? null : subLevel.getUniqueId();
        }

        static Vec3 toFrame(Entity entity, Vec3 point) {
            @Nullable SubLevelAccess subLevel = SableCompanion.INSTANCE.getTrackingSubLevel(entity);
            return subLevel == null ? point : subLevel.logicalPose().transformPositionInverse(point);
        }
    }
    //?}
}
