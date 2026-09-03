// SPDX-FileCopyrightText: 2026 ASPCT
// SPDX-License-Identifier: LGPL-3.0-or-later

package com.aspctt.treadlightly.sound.acoustic;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.world.entity.LivingEntity;

import com.aspctt.treadlightly.sound.Options;
import com.aspctt.treadlightly.sound.State;
import com.aspctt.treadlightly.sound.player.SoundPlayer;

/**
 * Something a pack can name that knows how to make a noise.
 * <p>
 * Acoustics compose. A named acoustic is usually a set of per-state choices, each of which may
 * pick at random by weight, roll a chance, delay itself, or play several others at once, and
 * each of those may in turn be any of the same. That nesting is the whole expressive power of
 * the format, and it is all resolved once at load rather than per footstep.
 */
public interface Acoustic {
    /** Parsers by the {@code type} a pack writes. */
    Map<String, Serializer> FACTORIES = Map.of(
            "basic", VaryingAcoustic.FACTORY,
            "events", EventSelectorAcoustic.FACTORY,
            "simultaneous", SimultaneousAcoustic.FACTORY,
            "delayed", DelayedAcoustic.FACTORY,
            "probability", WeightedAcoustic.FACTORY,
            "chance", ChanceAcoustic.FACTORY
    );

    static Acoustic read(AcousticsFile context, JsonElement unsolved) throws JsonParseException {
        return read(context, unsolved, "basic");
    }

    static Acoustic read(AcousticsFile context, JsonElement json, String defaultUnassigned) throws JsonParseException {
        String type = getType(json, defaultUnassigned);
        return checked(
                checked(FACTORIES.get(type), () -> "Invalid type for acoustic `" + type + "`").create(json, context),
                () -> "Unresolved Json element: \r\n" + json);
    }

    /**
     * The type is usually written out, but the common shapes are inferred: a bare string is a
     * sound name, and an array is several acoustics at once.
     */
    private static String getType(JsonElement unsolved, String defaultUnassigned) {
        if (unsolved.isJsonObject()) {
            JsonObject json = unsolved.getAsJsonObject();
            return json.has("type") ? json.get("type").getAsString() : defaultUnassigned;
        }

        if (unsolved.isJsonArray()) {
            return "simultaneous";
        }

        if (unsolved.isJsonPrimitive() && unsolved.getAsJsonPrimitive().isString()) {
            return "basic";
        }

        return "";
    }

    private static <T> T checked(T value, Supplier<String> message) throws JsonParseException {
        if (value == null) {
            throw new JsonParseException(message.get());
        }
        return value;
    }

    void playSound(SoundPlayer player, LivingEntity location, State event, Options inputOptions);

    interface Serializer {
        Acoustic create(JsonElement json, AcousticsFile context);

        static Serializer ofJsObject(BiFunction<JsonObject, AcousticsFile, Acoustic> factory) {
            return (json, context) -> factory.apply(json.getAsJsonObject(), context);
        }
    }
}
