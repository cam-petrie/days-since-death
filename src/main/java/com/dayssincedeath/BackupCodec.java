package com.dayssincedeath;

import com.google.gson.Gson;

final class BackupCodec
{
    static final int MAX_CHARS = 1_000_000;
    static final class Backup
    {
        String format;
        int version;
        String character;
        long exportedAt;
        TrackerState state;
    }

    static String encode(Gson gson, TrackerState state, String character, long now)
    {
        Backup b = new Backup(); b.format = "days-since-death"; b.version = 2;
        b.character = character; b.exportedAt = now; b.state = state.copy();
        return gson.toJson(b);
    }

    static Backup decode(Gson gson, String json, long now)
    {
        if (json == null || json.length() > MAX_CHARS) { throw new IllegalArgumentException("Backup is too large"); }
        com.google.gson.JsonObject root = gson.fromJson(json, com.google.gson.JsonObject.class);
        if (root == null || !root.has("state") || !root.get("state").isJsonObject()
                || !root.getAsJsonObject("state").has("version"))
        { throw new IllegalArgumentException("Backup is missing its record schema"); }
        Backup b = gson.fromJson(json, Backup.class);
        if (b == null || !"days-since-death".equals(b.format) || b.version != 2
                || b.character == null || b.character.length() > 80 || b.exportedAt <= 0 || b.state == null)
        { throw new IllegalArgumentException("Not a Days Since Death v2 backup"); }
        b.state.validate(now);
        return b;
    }
}
