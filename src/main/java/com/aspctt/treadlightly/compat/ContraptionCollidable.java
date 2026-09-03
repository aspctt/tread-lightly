// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An entity that is standing on blocks the world does not know about, such as a moving
 * contraption. Implemented by other mods; nothing here provides it.
 * <p>
 * Without this, walking around on a moving platform reads as walking on air and falls silent.
 */
public interface ContraptionCollidable {
    BlockState getCollidedStateAt(BlockPos pos);
}
