// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.client;

import java.nio.file.Path;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.google.gson.JsonObject;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.sound.SoundEngine;
import com.aspctt.treadlightly.world.Lookups;

/**
 * {@code /treadlightly report} writes out what every block resolves to.
 * <p>
 * A client command, so it works on any server and needs no permissions: it only reads what this
 * client has loaded.
 */
public final class ReportCommand {
    private ReportCommand() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal(TreadLightly.MODID)
                        .then(Commands.literal("report")
                                .executes(context -> run(context.getSource(), false))
                                .then(Commands.literal("full")
                                        .executes(context -> run(context.getSource(), true)))));
    }

    private static int run(CommandSourceStack source, boolean full) {
        SoundEngine engine = TreadLightly.engine();
        Lookups lookups = engine.getLookups();

        if (!lookups.hasData()) {
            source.sendFailure(Component.translatable("commands.treadlightly.report.no_pack"));
            return 0;
        }

        Path directory = FMLPaths.GAMEDIR.get().resolve(TreadLightly.MODID);

        // Gathered here, on the client thread, because it reads the same block map the game is
        // resolving footsteps against and that map memoises as it goes.
        JsonObject report = BlockReport.gather(lookups, full);
        Minecraft client = Minecraft.getInstance();

        // Writing is the slow part and touches nothing shared, so it goes to the IO pool. The
        // reply has to come back to the client thread before it can be put on screen.
        Util.ioPool().execute(() -> {
            try {
                Path file = BlockReport.write(report, full, directory);
                client.execute(() -> source.sendSuccess(() -> Component.translatable(
                        "commands.treadlightly.report.done",
                        Component.literal(file.getFileName().toString())
                                .withStyle(style -> style
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.toString()))
                                        .withUnderlined(true)),
                        report.get("blocks_with_nothing_mapped").getAsInt(),
                        report.get("blocks_in_game").getAsInt()
                ).withStyle(ChatFormatting.GREEN), false));
            } catch (Exception e) {
                TreadLightly.LOGGER.error("Could not write the block report", e);
                client.execute(() -> source.sendFailure(
                        Component.translatable("commands.treadlightly.report.failed", e.getMessage())));
            }
        });

        return 1;
    }
}
