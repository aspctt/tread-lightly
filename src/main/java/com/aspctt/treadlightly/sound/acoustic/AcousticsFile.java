// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import java.io.Reader;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.aspctt.treadlightly.TreadLightly;
import com.aspctt.treadlightly.util.Range;

/**
 * Reads an acoustics library file and hands each named acoustic to a consumer.
 * <p>
 * The record itself is the parse context: the defaults an entry inherits when it does not say
 * otherwise, and the prefix its sound names hang off.
 *
 * @param defaultVolume volume window applied to entries that declare none
 * @param defaultPitch  pitch window applied to entries that declare none
 * @param soundRoot     prefix prepended to sound names that do not start with {@code @}
 */
public record AcousticsFile(
        Range defaultVolume,
        Range defaultPitch,
        String soundRoot
) {
    /**
     * The format version a pack must declare. Packs are refused rather than half-read, since a
     * file written for a different engine will parse into something quietly wrong.
     */
    private static final int ENGINE_VERSION = 2;

    @Nullable
    public static AcousticsFile read(Reader reader, BiConsumer<String, Acoustic> consumer, boolean ignoreVersion) {
        try {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            AcousticsFile context = read(json, ignoreVersion);
            json.getAsJsonObject("contents").entrySet().forEach(element ->
                    consumer.accept(element.getKey(), Acoustic.read(context, element.getValue(), "events")));
            return context;
        } catch (JsonParseException e) {
            TreadLightly.LOGGER.error("Error whilst loading acoustics", e);
        }
        return null;
    }

    private static AcousticsFile read(JsonObject json, boolean ignoreVersion) {
        expect("library".equals(json.get("type").getAsString()),
                () -> "Invalid type: Expected \"library\" got \"" + json.get("type").getAsString() + "\"");
        expect(ignoreVersion || json.get("engineversion").getAsInt() == ENGINE_VERSION,
                () -> "Unrecognised Engine version: " + ENGINE_VERSION + " expected, got " + json.get("engineversion").getAsInt());
        expect(json.has("contents"), () -> "Empty contents");

        String soundRoot = json.has("soundroot") ? json.get("soundroot").getAsString() : "";

        if (json.has("defaults")) {
            JsonObject defaults = json.getAsJsonObject("defaults");
            return new AcousticsFile(
                    Range.DEFAULT.read("volume", defaults),
                    Range.DEFAULT.read("pitch", defaults),
                    soundRoot
            );
        }

        return new AcousticsFile(Range.DEFAULT, Range.DEFAULT, soundRoot);
    }

    private static void expect(boolean condition, Supplier<String> message) {
        if (!condition) {
            throw new JsonParseException(message.get());
        }
    }

    /**
     * Resolves a sound name written in a pack. A leading {@code @} escapes the pack's own
     * sound root, which is how an entry reaches a sound belonging to somebody else.
     */
    public String getSoundName(String soundName) {
        if (soundName.isEmpty() || soundName.charAt(0) != '@') {
            return soundRoot + soundName;
        }

        return soundName.substring(1);
    }
}
