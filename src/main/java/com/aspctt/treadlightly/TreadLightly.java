// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Tread Lightly is a client-only mod. Everything it does happens on the machine that
 * renders the world, so a dedicated server neither loads it nor needs it installed.
 */
@Mod(value = TreadLightly.MODID, dist = Dist.CLIENT)
public class TreadLightly {
    /**
     * The mod id, which doubles as the resource namespace sound packs address.
     */
    public static final String MODID = "treadlightly";

    public static final Logger LOGGER = LogUtils.getLogger();

    public TreadLightly(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
