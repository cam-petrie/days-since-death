package com.dayssincedeath;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeathRecordTest
{
    private static final long START = 1_700_000_000_000L;
    private static final long HOUR = 3_600_000;

    @Test
    public void firstUseDoesNotInventADeathOrPersonalBest()
    {
        DeathRecord record = DeathRecord.start(START);
        assertTrue(record.valid());
        assertEquals(0, record.lastDeath);
        assertEquals(0, record.deaths);
        assertEquals(2 * HOUR, record.elapsed(START + 2 * HOUR));
        assertEquals(0, record.best(START + 2 * HOUR));
    }

    @Test
    public void firstDeathStartsAStreakWithoutCountingPartialHistoryAsBest()
    {
        DeathRecord record = DeathRecord.start(START).died(START + 10 * HOUR);
        assertEquals(0, record.bestCompleted);
        assertEquals(1, record.deaths);
        assertEquals(HOUR, record.best(START + 11 * HOUR));
    }

    @Test
    public void laterDeathResetsCurrentAndKeepsLongestFullInterval()
    {
        DeathRecord record = DeathRecord.start(START).died(START + HOUR)
                .died(START + 5 * HOUR).died(START + 6 * HOUR);
        assertEquals(3, record.deaths);
        assertEquals(4 * HOUR, record.bestCompleted);
        assertEquals(0, record.elapsed(START + 6 * HOUR));
        assertEquals(4 * HOUR, record.best(START + 6 * HOUR));
        assertEquals(5 * HOUR, record.best(START + 11 * HOUR));
    }

    @Test
    public void backwardsClockNeverCreatesNegativeDuration()
    {
        DeathRecord record = DeathRecord.start(START).died(START + HOUR);
        assertEquals(0, record.elapsed(START));
        DeathRecord after = record.died(START);
        assertTrue(after.valid());
        assertEquals(START + HOUR, after.lastDeath);
        assertEquals(0, after.bestCompleted);
    }

    @Test
    public void inconsistentSavedRecordsAreRejected()
    {
        assertFalse(new DeathRecord(0, 0, 0, 0).valid());
        assertFalse(new DeathRecord(START, START - 1, 0, 1).valid());
        assertFalse(new DeathRecord(START, 0, 0, 1).valid());
        assertFalse(new DeathRecord(START, START, -1, 1).valid());
    }
}
