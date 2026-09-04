// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.LivingEntity;

/**
 * Exposes whether an entity is holding its jump input, to tell a deliberate jump from walking
 * off a ledge.
 * <p>
 * An access transformer is the tidier route, but on NeoGradle 7.1.38 both the MDK's DSL line
 * and the documented {@code accessTransformers} block drop the joined Minecraft artifact from
 * the compile classpath, with nothing naming the cause. Worth retrying on a newer NeoGradle.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean treadlightly$isJumping();
}
