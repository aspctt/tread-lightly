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
 * An accessor rather than an access transformer so the field is checked against every target's
 * bytecode by tools/check-mixin-targets.py, along with the other mixin targets.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean treadlightly$isJumping();
}
