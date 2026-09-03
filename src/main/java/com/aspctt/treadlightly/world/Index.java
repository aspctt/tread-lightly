// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import net.minecraft.resources.ResourceLocation;

/**
 * A flat mapping loaded from config, where every key resolves to exactly one value and there
 * is always a default. Unlike {@link Lookup} there are no substrates and no layering: the last
 * pack to name a key wins outright.
 *
 * @param <K> what is looked up
 * @param <V> what it resolves to
 */
public interface Index<K, V> extends Loadable {
    V lookup(K key);

    boolean contains(ResourceLocation key);
}
