// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.generator;

import java.util.function.Supplier;

import com.aspctt.treadlightly.sound.SoundSettings;
import com.aspctt.treadlightly.world.Lookups;
import com.aspctt.treadlightly.world.Solver;

/**
 * What a generator needs to do its job.
 *
 * @param lookups  supplied rather than held, since a resource reload replaces the whole set
 * @param settings the user's configuration
 * @param solver   shared by every entity, and stateless
 */
public record StepSoundContext(
        Supplier<Lookups> lookups,
        SoundSettings settings,
        Solver solver
) { }
