// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import net.minecraft.resources.ResourceLocation;

/**
 * A flat mapping with one value per key and always a default. Unlike {@link Lookup} there are
 * no substrates and no layering: the last pack to name a key wins outright.
 */
public interface Index<K, V> extends Loadable {
    V lookup(K key);

    boolean contains(ResourceLocation key);
}
