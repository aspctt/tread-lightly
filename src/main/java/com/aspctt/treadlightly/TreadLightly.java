// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import com.aspctt.treadlightly.client.ConfigScreens;
import com.aspctt.treadlightly.config.TreadLightlyConfig;
import com.aspctt.treadlightly.sound.SoundEngine;
import com.aspctt.treadlightly.sound.VanillaSoundSuppressor;

/**
 * Tread Lightly is a client-only mod. Everything it does happens on the machine that renders
 * the world, so a dedicated server neither loads it nor needs it installed.
 */
@Mod(value = TreadLightly.MODID, dist = Dist.CLIENT)
public class TreadLightly {
    /** The mod id, which doubles as the resource namespace sound packs address. */
    public static final String MODID = "treadlightly";

    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * The config screen library, which is optional. A compile-time constant, so naming it here
     * does not load anything that would need the library present.
     */
    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    /**
     * Reachable statically because mixins cannot be handed dependencies, and the entity mixins
     * need to ask whether this mod is about to make a sound. Nothing else should reach for it.
     */
    private static SoundEngine engine;

    private final TreadLightlyConfig config = new TreadLightlyConfig();

    public TreadLightly(IEventBus modEventBus, ModContainer modContainer) {
        config.bind(FMLPaths.CONFIGDIR.get().resolve(MODID + ".json"), this::onEnabledChanged);
        config.load();

        engine = new SoundEngine(config);

        modEventBus.addListener(RegisterClientReloadListenersEvent.class, event ->
                event.registerReloadListener((ResourceManagerReloadListener) this::onResourceReload));

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> onTick());
        NeoForge.EVENT_BUS.addListener(PlaySoundEvent.class, VanillaSoundSuppressor::onPlaySound);

        // Optional: without YetAnotherConfigLib the mod works and only the screen is missing.
        // Registering nothing leaves the Mods list without a config button, which is what
        // NeoForge shows for a mod that offers no screen.
        if (ModList.get().isLoaded(YACL_MOD_ID)) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> ConfigScreens.create(config, parent));
        }
    }

    public static SoundEngine engine() {
        return engine;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onResourceReload(ResourceManager manager) {
        engine.reload(manager);
    }

    private void onEnabledChanged() {
        engine.reload();
    }

    private void onTick() {
        var client = net.minecraft.client.Minecraft.getInstance();
        var camera = client.player;

        if (camera != null && !camera.isRemoved()) {
            engine.onFrame(camera);
        }
    }
}
