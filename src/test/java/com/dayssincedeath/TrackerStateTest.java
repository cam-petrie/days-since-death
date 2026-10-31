package com.dayssincedeath;

import com.google.gson.Gson;
import java.util.EnumSet;
import net.runelite.api.WorldType;
import org.junit.Test;
import static org.junit.Assert.*;

public class TrackerStateTest
{
    private static final long START = 1_700_000_000_000L;

    @Test
    public void ignoredDeathsLeaveTheStreakAndUndoAlone()
    {
        TrackerState s = TrackerState.start(START);
        s.died(START + 1000, "Region 1", null);
        String undoId = s.undo.eventId;
        s.died(START + 2000, "LMS", "Excluded LMS");
        assertEquals(1, s.deaths); assertEquals(1, s.ignoredDeaths);
        assertEquals(START + 1000, s.lastDeath); assertEquals(undoId, s.undo.eventId);
        assertEquals("IGNORED", s.history.get(0).type);
        s.validate(START + 2000);
    }

    @Test
    public void undoRestoresStreakAndRetainsPlaytimeSinceTheDeath()
    {
        TrackerState s = TrackerState.start(START);
        s.died(START, "A", null); s.addPlaytime(3000);
        s.died(START + 5000, "B", null); s.addPlaytime(2000);
        s.undoDeath();
        assertEquals(START, s.lastDeath); assertEquals(1, s.deaths);
        assertEquals(5000, s.currentPlayMillis); assertEquals(5000, s.loggedMillis);
        assertEquals("UNDONE", s.history.get(0).type); assertTrue(s.corrected); assertNull(s.undo);
        s.validate(START + 10_000);
    }

    @Test
    public void manualDateIsLabelledAndDoesNotInventPlaytime()
    {
        TrackerState s = TrackerState.start(START);
        s.setManualDate(START - 86_400_000, START);
        assertTrue(s.manualDate); assertTrue(s.corrected); assertTrue(s.playtimePartial);
        assertEquals(0, s.currentPlayMillis); assertEquals(0, s.deaths);
        assertEquals("MANUAL", s.history.get(0).type); s.validate(START);
    }

    @Test(expected = IllegalArgumentException.class)
    public void futureManualDateIsRejected()
    { TrackerState.start(START).setManualDate(START + 1, START); }

    @Test
    public void migrationPreservesOriginalRecordAndMarksPlaytimePartial()
    {
        DeathRecord original = DeathRecord.start(START).died(START + 1000).died(START + 5000);
        TrackerState s = TrackerState.migrate(original, START + 10_000);
        assertEquals(original.lastDeath, s.lastDeath); assertEquals(original.bestCompleted, s.bestCompleted);
        assertEquals(2, s.deaths); assertEquals(0, s.loggedMillis); assertTrue(s.playtimePartial);
        assertEquals(START + 10_000, s.lastSeen); s.validate(START + 10_000);
    }

    @Test
    public void coverageGapsAreBoundedAndNeverCountAsPlaytime()
    {
        TrackerState s = TrackerState.start(START);
        s.addPlaytime(600); s.addPlaytime(100_000); s.addPlaytime(-1);
        for (int i = 1; i <= 30; i++) { s.resumed(START + i * 60_000); }
        assertEquals(600, s.loggedMillis); assertEquals(20, s.gaps.size());
        s.validate(START + 30 * 60_000);
    }

    @Test
    public void onlySupportedSafeWorldsAreExcludedWhenEnabled()
    {
        assertNull(SafeDeathPolicy.ignoredReason(EnumSet.of(WorldType.LAST_MAN_STANDING), false, false));
        assertNotNull(SafeDeathPolicy.ignoredReason(EnumSet.of(WorldType.LAST_MAN_STANDING), true, false));
        assertNotNull(SafeDeathPolicy.ignoredReason(EnumSet.of(WorldType.PVP_ARENA), false, true));
        assertNull(SafeDeathPolicy.ignoredReason(EnumSet.of(WorldType.PVP), true, true));
        assertNull(SafeDeathPolicy.ignoredReason(null, true, true));
    }

    @Test
    public void backupRoundTripRetainsHistoryNotesAndUndo()
    {
        Gson gson = new Gson(); TrackerState s = TrackerState.start(START);
        s.died(START + 1000, "Example region", null);
        s.setNote(s.history.get(0).id, "Practice run");
        String json = BackupCodec.encode(gson, s, "Example", START + 2000);
        BackupCodec.Backup b = BackupCodec.decode(gson, json, START + 2000);
        assertEquals("Example", b.character); assertEquals("Practice run", b.state.history.get(0).note);
        b.state.undoDeath(); assertEquals(0, b.state.lastDeath);
        assertEquals(START + 1000, s.lastDeath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedBackupSchemaIsRejected()
    { BackupCodec.decode(new Gson(), "{\"format\":\"days-since-death\",\"version\":999}", START); }

    @Test(expected = IllegalArgumentException.class)
    public void corruptedImportedHistoryIsRejected()
    {
        TrackerState s = TrackerState.start(START); s.died(START, "A", null);
        s.history.get(0).type = "INVALID";
        BackupCodec.decode(new Gson(), BackupCodec.encode(new Gson(), s, "A", START), START);
    }

    @Test
    public void snapshotsAreIndependentAndHistoryIsBounded()
    {
        TrackerState s = TrackerState.start(START);
        for (int i = 0; i < 110; i++) { s.died(START + i, "A", null); }
        TrackerState snapshot = s.copy(); snapshot.setNote(snapshot.history.get(0).id, "Edited");
        assertEquals("", s.history.get(0).note); assertEquals(100, s.history.size());
        s.validate(START + 110);
    }

    @Test
    public void compactCounterHonoursBasisSecondsAndUnknownState()
    {
        TrackerState s = TrackerState.start(START); ViewSettings options = new ViewSettings();
        assertEquals("Unknown", options.counter(s, START));
        s.died(START, "A", null); s.addPlaytime(3000);
        options.basis = TimeBasis.LOGGED_IN;
        assertEquals("0m 3s", options.counter(s, START + 60_000));
        s.playtimePartial = true; options.showSeconds = false;
        assertEquals("0m+", options.counter(s, START + 60_000));
    }
}
