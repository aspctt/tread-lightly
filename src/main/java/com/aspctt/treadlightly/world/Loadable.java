// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.world;

import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A config file that is a flat object of key to value, read additively so that several packs
 * can each contribute entries.
 */
public interface Loadable {
    Gson GSON = new Gson();

    /** Registers one entry. Called once per key in the file. */
    void add(String key, JsonElement json);

    /** Reads entries from the given reader, adding to whatever is already loaded. */
    default void load(Reader reader) {
        JsonObject json = GSON.fromJson(reader, JsonObject.class);

        json.entrySet().forEach(entry -> add(entry.getKey(), entry.getValue()));
    }
}
