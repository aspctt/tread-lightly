// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.api.DerivedBlock;
import com.aspctt.treadlightly.sound.SoundEngine;
import com.aspctt.treadlightly.world.Lookups;
import com.aspctt.treadlightly.world.PrimitiveLookup;
import com.aspctt.treadlightly.world.SoundsKey;

/**
 * Adds a few lines to the F3 screen saying what the block underfoot and the block being looked
 * at resolve to. Answers "why does this sound wrong" without writing a whole report.
 */
public final class DebugReadout {
    private DebugReadout() {
    }

    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
            appendTo(event.getRight());
        }
    }

    /** Separate from the event so the content can be exercised without a render pass. */
    public static void appendTo(List<String> lines) {
        Minecraft client = Minecraft.getInstance();
        @Nullable SoundEngine engine = TreadLightly.engine();
        @Nullable Player player = client.player;

        if (engine == null || player == null) {
            return;
        }

        lines.add("");

        if (!engine.getLookups().hasData()) {
            lines.add("[Tread Lightly] no pack loaded");
            return;
        }

        lines.add("[Tread Lightly]");

        BlockPos underfoot = player.blockPosition().below();
        lines.add("underfoot: " + describe(engine.getLookups(), player, underfoot));

        if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            lines.add("looking at: " + describe(engine.getLookups(), player, hit.getBlockPos()));
        }
    }

    /** What the block at this position resolves to, and how it got there. */
    private static String describe(Lookups lookups, Player player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        Map<String, SoundsKey> mapped = lookups.globalBlocks().getAssociations(state);

        if (!mapped.isEmpty()) {
            return join(mapped);
        }

        BlockState base = DerivedBlock.getBaseOf(state);
        Map<String, SoundsKey> viaBase = base.isAir() ? Map.of() : lookups.globalBlocks().getAssociations(base);
        if (!viaBase.isEmpty()) {
            return "via " + BuiltInRegistries.BLOCK.getKey(base.getBlock()) + ": " + join(viaBase);
        }

        // The position-aware overload, so a block that varies its sound by location reports
        // what it will actually sound like here.
        SoundType sounds = state.getSoundType(player.level(), pos, player);
        SoundsKey fallback = lookups.primitives()
                .getAssociation(sounds.getStepSound(), PrimitiveLookup.getSubstrate(sounds));

        return fallback.isResult()
                ? "sound type " + sounds.getStepSound().getLocation() + " -> " + fallback.raw()
                : "nothing mapped, using the game's own sound";
    }

    /**
     * Entries are separated with a pipe, not a comma: a single entry can already name several
     * acoustics comma separated, and mixing the two makes the line impossible to read. The
     * unqualified entry comes first, the rest in a fixed order, so the same block always reads
     * the same way.
     */
    private static String join(Map<String, SoundsKey> mapped) {
        return mapped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().isEmpty()
                        ? entry.getValue().raw()
                        : entry.getKey() + "=" + entry.getValue().raw())
                .collect(Collectors.joining(" | "));
    }
}
