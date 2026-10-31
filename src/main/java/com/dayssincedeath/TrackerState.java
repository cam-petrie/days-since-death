package com.dayssincedeath;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** V2 preview data. Mutations belong to the client thread; views receive copies. */
final class TrackerState
{
    static final int HISTORY_LIMIT = 100;
    int version = 2;
    long trackingStarted;
    long lastDeath;
    long bestCompleted;
    long deaths;
    long ignoredDeaths;
    long loggedMillis;
    long currentPlayMillis;
    long bestPlayMillis;
    long lastSeen;
    boolean manualDate;
    boolean corrected;
    boolean playtimePartial;
    List<Entry> history = new ArrayList<>();
    List<Gap> gaps = new ArrayList<>();
    UndoPoint undo;

    static final class Entry
    {
        String id;
        long at;
        String type;
        String location;
        String note = "";
        long streakMillis;
        long playMillis;

        Entry copy()
        {
            Entry e = new Entry();
            e.id = id; e.at = at; e.type = type; e.location = location;
            e.note = note; e.streakMillis = streakMillis; e.playMillis = playMillis;
            return e;
        }
    }

    static final class Gap
    {
        long from;
        long to;
        Gap copy() { Gap g = new Gap(); g.from = from; g.to = to; return g; }
    }

    static final class UndoPoint
    {
        String eventId;
        long lastDeath;
        long bestCompleted;
        long deaths;
        long currentPlayMillis;
        long bestPlayMillis;
        boolean manualDate;
        boolean playtimePartial;
    }

    static TrackerState start(long now)
    {
        TrackerState s = new TrackerState();
        s.trackingStarted = now;
        s.lastSeen = now;
        return s;
    }

    static TrackerState migrate(DeathRecord old, long now)
    {
        if (!old.valid()) { throw new IllegalArgumentException("Invalid original record"); }
        TrackerState s = start(old.trackingStarted);
        s.lastDeath = old.lastDeath;
        s.bestCompleted = old.bestCompleted;
        s.deaths = old.deaths;
        s.playtimePartial = true;
        // V1 never saved coverage or logged-in time; do not invent it.
        s.lastSeen = now;
        return s;
    }

    TrackerState copy()
    {
        TrackerState s = new TrackerState();
        s.version = version; s.trackingStarted = trackingStarted; s.lastDeath = lastDeath;
        s.bestCompleted = bestCompleted; s.deaths = deaths; s.ignoredDeaths = ignoredDeaths;
        s.loggedMillis = loggedMillis; s.currentPlayMillis = currentPlayMillis;
        s.bestPlayMillis = bestPlayMillis; s.lastSeen = lastSeen;
        s.manualDate = manualDate; s.corrected = corrected; s.playtimePartial = playtimePartial;
        history.forEach(e -> s.history.add(e.copy()));
        gaps.forEach(g -> s.gaps.add(g.copy()));
        if (undo != null)
        {
            s.undo = new UndoPoint();
            s.undo.eventId = undo.eventId; s.undo.lastDeath = undo.lastDeath;
            s.undo.bestCompleted = undo.bestCompleted; s.undo.deaths = undo.deaths;
            s.undo.currentPlayMillis = undo.currentPlayMillis; s.undo.bestPlayMillis = undo.bestPlayMillis;
            s.undo.manualDate = undo.manualDate; s.undo.playtimePartial = undo.playtimePartial;
        }
        return s;
    }

    long elapsed(long now) { return Math.max(0, now - (lastDeath == 0 ? trackingStarted : lastDeath)); }
    long best(long now) { return lastDeath == 0 ? bestCompleted : Math.max(bestCompleted, elapsed(now)); }
    long bestPlay() { return lastDeath == 0 ? bestPlayMillis : Math.max(bestPlayMillis, currentPlayMillis); }

    void addPlaytime(long delta)
    {
        if (delta < 0 || delta > 5000) { return; }
        loggedMillis += delta;
        currentPlayMillis += delta;
        if (undo != null) { undo.currentPlayMillis += delta; }
    }

    void resumed(long now)
    {
        if (lastSeen > 0 && now > lastSeen + 5000)
        {
            Gap g = new Gap(); g.from = lastSeen; g.to = now;
            gaps.add(0, g);
            if (gaps.size() > 20) { gaps.remove(gaps.size() - 1); }
        }
        lastSeen = Math.max(lastSeen, now);
    }

    void died(long now, String location, String ignoreReason)
    {
        long at = Math.max(now, Math.max(trackingStarted, lastDeath));
        Entry e = new Entry();
        e.id = UUID.randomUUID().toString(); e.at = at;
        e.location = location; e.streakMillis = lastDeath == 0 ? 0 : elapsed(at);
        e.playMillis = lastDeath == 0 ? 0 : currentPlayMillis;
        if (ignoreReason != null)
        {
            e.type = "IGNORED";
            e.note = ignoreReason;
            ignoredDeaths++;
        }
        else
        {
            UndoPoint u = new UndoPoint();
            u.eventId = e.id; u.lastDeath = lastDeath; u.bestCompleted = bestCompleted;
            u.deaths = deaths; u.currentPlayMillis = currentPlayMillis;
            u.bestPlayMillis = bestPlayMillis; u.manualDate = manualDate; u.playtimePartial = playtimePartial;
            undo = u;
            e.type = "OBSERVED";
            if (lastDeath != 0)
            {
                bestCompleted = Math.max(bestCompleted, elapsed(at));
                bestPlayMillis = Math.max(bestPlayMillis, currentPlayMillis);
            }
            lastDeath = at; deaths++; currentPlayMillis = 0;
            manualDate = false; playtimePartial = false;
        }
        history.add(0, e);
        trimHistory();
    }

    void undoDeath()
    {
        if (undo == null) { throw new IllegalStateException("No counted death available to undo"); }
        Entry target = history.stream().filter(e -> e.id.equals(undo.eventId)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Undo history is unavailable"));
        target.type = "UNDONE";
        lastDeath = undo.lastDeath; bestCompleted = undo.bestCompleted; deaths = undo.deaths;
        currentPlayMillis = undo.currentPlayMillis; bestPlayMillis = undo.bestPlayMillis;
        manualDate = undo.manualDate; playtimePartial = undo.playtimePartial;
        undo = null; corrected = true;
    }

    void setManualDate(long at, long now)
    {
        if (at <= 0 || at > now) { throw new IllegalArgumentException("Use a past date, not a future date"); }
        lastDeath = at; trackingStarted = Math.min(trackingStarted, at);
        manualDate = true; corrected = true; currentPlayMillis = 0;
        playtimePartial = true; undo = null;
        Entry e = new Entry(); e.id = UUID.randomUUID().toString(); e.at = now;
        e.type = "MANUAL"; e.location = "User supplied date";
        e.note = "Last death set to " + java.time.Instant.ofEpochMilli(at);
        history.add(0, e); trimHistory();
    }

    void setNote(String id, String note)
    {
        if (note == null || note.length() > 500) { throw new IllegalArgumentException("Notes may contain at most 500 characters"); }
        history.stream().filter(e -> e.id.equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("History entry no longer exists")).note = note;
    }

    private void trimHistory()
    {
        while (history.size() > HISTORY_LIMIT) { history.remove(history.size() - 1); }
        if (undo != null && history.stream().noneMatch(e -> e.id.equals(undo.eventId))) { undo = null; }
    }

    void validate(long now)
    {
        if (version != 2 || trackingStarted <= 0 || trackingStarted > now + 300_000
                || lastDeath < 0 || lastDeath > now + 300_000 || (lastDeath > 0 && lastDeath < trackingStarted)
                || bestCompleted < 0 || deaths < 0 || ignoredDeaths < 0 || lastSeen < 0 || lastSeen > now + 300_000
                || loggedMillis < 0 || currentPlayMillis < 0 || currentPlayMillis > loggedMillis || bestPlayMillis < 0 || bestPlayMillis > loggedMillis
                || history == null || history.size() > HISTORY_LIMIT || gaps == null || gaps.size() > 20)
        { throw new IllegalArgumentException("Invalid or unsupported saved record"); }
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Entry e : history)
        {
            if (e == null || e.id == null || e.id.length() > 80 || !ids.add(e.id)
                    || e.at <= 0 || e.at > now + 300_000 || e.streakMillis < 0 || e.playMillis < 0
                    || !("OBSERVED".equals(e.type) || "IGNORED".equals(e.type) || "MANUAL".equals(e.type) || "UNDONE".equals(e.type))
                    || e.location == null || e.location.length() > 200 || e.note == null || e.note.length() > 500)
            { throw new IllegalArgumentException("Invalid death history"); }
        }
        for (Gap g : gaps)
        {
            if (g == null || g.from <= 0 || g.to < g.from || g.to > now + 300_000)
            { throw new IllegalArgumentException("Invalid coverage interval"); }
        }
        if (undo != null && (undo.lastDeath < 0 || undo.lastDeath > now + 300_000 || undo.deaths != deaths - 1
                || undo.bestCompleted < 0 || undo.currentPlayMillis < 0 || undo.currentPlayMillis > loggedMillis
                || undo.bestPlayMillis < 0 || history.stream().noneMatch(e -> e.id.equals(undo.eventId) && "OBSERVED".equals(e.type))))
        { throw new IllegalArgumentException("Invalid undo record"); }
    }
}
