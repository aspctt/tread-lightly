// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.client;

import java.util.List;
import java.util.Map;

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
        Minecraft client = Minecraft.getInstance();

        if (!client.getDebugOverlay().showDebugScreen()) {
            return;
        }

        @Nullable SoundEngine engine = TreadLightly.engine();
        @Nullable Player player = client.player;

        if (engine == null || player == null) {
            return;
        }

        List<String> lines = event.getRight();
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

    private static String join(Map<String, SoundsKey> mapped) {
        StringBuilder out = new StringBuilder();

        mapped.forEach((substrate, key) -> {
            if (!out.isEmpty()) {
                out.append(", ");
            }
            out.append(substrate.isEmpty() ? "" : substrate + "=").append(key.raw());
        });

        return out.toString();
    }
}
