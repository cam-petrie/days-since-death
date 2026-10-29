package com.dayssincedeath;

/** Immutable record persisted as a single character-scoped configuration value. */
final class DeathRecord
{
    final int version;
    final long trackingStarted;
    final long lastDeath;
    final long bestCompleted;
    final long deaths;

    DeathRecord(long trackingStarted, long lastDeath, long bestCompleted, long deaths)
    {
        this.version = 1;
        this.trackingStarted = trackingStarted;
        this.lastDeath = lastDeath;
        this.bestCompleted = bestCompleted;
        this.deaths = deaths;
    }

    static DeathRecord start(long now) { return new DeathRecord(now, 0, 0, 0); }

    boolean valid()
    {
        return version == 1 && trackingStarted > 0 && lastDeath >= 0
                && (lastDeath == 0 || lastDeath >= trackingStarted)
                && bestCompleted >= 0 && deaths >= 0
                && (lastDeath == 0 ? deaths == 0 && bestCompleted == 0 : deaths > 0);
    }

    long elapsed(long now) { return Math.max(0, now - (lastDeath > 0 ? lastDeath : trackingStarted)); }

    long best(long now) { return lastDeath == 0 ? 0 : Math.max(bestCompleted, elapsed(now)); }

    DeathRecord died(long now)
    {
        // A backwards system-clock adjustment must not create a negative streak.
        long timestamp = Math.max(now, Math.max(trackingStarted, lastDeath));
        long completed = lastDeath == 0 ? bestCompleted : Math.max(bestCompleted, timestamp - lastDeath);
        return new DeathRecord(trackingStarted, timestamp, completed, deaths + 1);
    }
}
