// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.LivingEntity;

/**
 * Exposes whether an entity is holding its jump input.
 * <p>
 * Needed to tell a deliberate jump from simply walking off a ledge, which want different
 * sounds. An access transformer would be the tidier route and is what NeoForge provides for
 * exactly this, but neither the MDK's DSL line nor NeoGradle 7.1's documented
 * {@code accessTransformers} block works: both quietly drop the joined Minecraft artifact from
 * the compile classpath, leaving every {@code net.minecraft} import unresolvable with nothing
 * naming the cause. Worth revisiting on a newer NeoGradle.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean treadlightly$isJumping();
}
