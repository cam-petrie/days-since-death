package com.dayssincedeath;

import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DaysSinceDeathPluginTest
{
    private DaysSinceDeathPlugin plugin;
    private Player local;
    private final Map<String, String> storage = new HashMap<>();
    private final AtomicReference<String> profile = new AtomicReference<>("character-a");
    private static final long START = 1_700_000_000_000L;

    @Before
    public void setUp()
    {
        plugin = new DaysSinceDeathPlugin();
        plugin.client = mock(Client.class);
        plugin.configManager = mock(ConfigManager.class);
        plugin.gson = new Gson();
        plugin.config = new DaysSinceDeathConfig() {};
        plugin.clock = Clock.fixed(Instant.ofEpochMilli(START), ZoneOffset.UTC);
        local = mock(Player.class);
        when(plugin.client.getLocalPlayer()).thenReturn(local);
        when(plugin.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(local.getName()).thenReturn("Test character");
        when(plugin.configManager.getRSProfileKey()).thenAnswer(inv -> profile.get());
        when(plugin.configManager.getConfiguration(eq(DaysSinceDeathConfig.GROUP), anyString(), eq(DaysSinceDeathPlugin.STATE_KEY)))
                .thenAnswer(inv -> storage.get(inv.getArgument(1)));
        doAnswer(inv -> { storage.put(inv.getArgument(1), inv.getArgument(3)); return null; })
                .when(plugin.configManager).setConfiguration(eq(DaysSinceDeathConfig.GROUP), anyString(), eq(DaysSinceDeathPlugin.STATE_KEY), anyString());
    }

    @Test
    public void onlyOwnDeathCountsAndDuplicateEventsAreSuppressed()
    {
        plugin.ensureProfile();
        plugin.onActorDeath(new ActorDeath(mock(NPC.class)));
        plugin.onActorDeath(new ActorDeath(mock(Player.class)));
        assertEquals(0, plugin.visibleRecord().deaths);
        when(local.isDead()).thenReturn(true);
        plugin.onActorDeath(new ActorDeath(local));
        plugin.onActorDeath(new ActorDeath(local));
        plugin.onGameTick(new GameTick());
        plugin.onActorDeath(new ActorDeath(local));
        assertEquals(1, plugin.visibleRecord().deaths);
        when(local.isDead()).thenReturn(false);
        plugin.onGameTick(new GameTick());
        plugin.clock = Clock.fixed(Instant.ofEpochMilli(START + 60_000), ZoneOffset.UTC);
        plugin.onActorDeath(new ActorDeath(local));
        assertEquals(2, plugin.visibleRecord().deaths);
        assertEquals(60_000, plugin.visibleRecord().bestCompleted);
    }

    @Test
    public void charactersHaveSeparatePersistentRecords()
    {
        plugin.ensureProfile();
        plugin.onActorDeath(new ActorDeath(local));
        String savedA = storage.get("character-a");
        profile.set("character-b");
        plugin.ensureProfile();
        assertEquals(0, plugin.visibleRecord().deaths);
        profile.set("character-a");
        plugin.ensureProfile();
        assertEquals(1, plugin.visibleRecord().deaths);
        assertEquals(savedA, storage.get("character-a"));
    }

    @Test
    public void corruptSavedDataIsPreserved()
    {
        storage.put("character-a", "{broken");
        plugin.ensureProfile();
        plugin.onActorDeath(new ActorDeath(local));
        assertNull(plugin.visibleRecord());
        assertEquals("{broken", storage.get("character-a"));
    }

    @Test
    public void missingCharacterAndLoggedOutEventsNeverWrite()
    {
        profile.set(null);
        plugin.onActorDeath(new ActorDeath(local));
        assertTrue(storage.isEmpty());
        profile.set("character-a");
        when(plugin.client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        plugin.onActorDeath(new ActorDeath(local));
        assertTrue(storage.isEmpty());
    }

    @Test
    public void versionTwoMigratesVersionOneWithoutOverwritingOriginalRecord()
    {
        DeathRecord original = DeathRecord.start(START).died(START);
        String originalJson = plugin.gson.toJson(original);
        when(plugin.configManager.getConfiguration("dayssincedeath", "character-a", "record")).thenReturn(originalJson);
        plugin.ensureProfile();
        assertEquals(1, plugin.visibleRecord().deaths);
        assertTrue(plugin.visibleRecord().playtimePartial);
        plugin.onActorDeath(new ActorDeath(local));
        verify(plugin.configManager, never()).setConfiguration(eq("dayssincedeath"), anyString(), eq("record"), anyString());
        assertEquals(originalJson, plugin.configManager.getConfiguration("dayssincedeath", "character-a", "record"));
    }

    @Test
    public void existingProductionVersionTwoTakesPrecedenceOverOldAndExperimentalData()
    {
        TrackerState production = TrackerState.start(START);
        production.died(START, "Production", null);
        production.died(START, "Production", null);
        storage.put("character-a", plugin.gson.toJson(production));
        when(plugin.configManager.getConfiguration("dayssincedeath", "character-a", "record"))
                .thenReturn(plugin.gson.toJson(DeathRecord.start(START)));
        when(plugin.configManager.getConfiguration("dayssincedeathlocal", "character-a", DaysSinceDeathPlugin.STATE_KEY))
                .thenReturn("{untrusted experimental data}");
        plugin.ensureProfile();
        assertEquals(2, plugin.visibleRecord().deaths);
        assertEquals("Production", plugin.visibleRecord().history.get(0).location);
        verify(plugin.configManager, never()).getConfiguration("dayssincedeathlocal", "character-a", DaysSinceDeathPlugin.STATE_KEY);
        verify(plugin.configManager, never()).getConfiguration("dayssincedeath", "character-a", "record");
    }

    @Test
    public void corruptedVersionTwoDoesNotSilentlyRevertToOlderVersionOne()
    {
        storage.put("character-a", "{invalid}");
        when(plugin.configManager.getConfiguration("dayssincedeath", "character-a", "record"))
                .thenReturn(plugin.gson.toJson(DeathRecord.start(START)));
        plugin.ensureProfile();
        assertNull(plugin.visibleRecord());
        assertEquals("{invalid}", storage.get("character-a"));
        verify(plugin.configManager, never()).getConfiguration("dayssincedeath", "character-a", "record");
    }

    @Test
    public void invalidLegacyRecordIsPreservedWithoutCreatingVersionTwo()
    {
        String original = plugin.gson.toJson(DeathRecord.start(START + 86_400_000));
        when(plugin.configManager.getConfiguration("dayssincedeath", "character-a", "record")).thenReturn(original);
        plugin.ensureProfile();
        assertNull(plugin.visibleRecord());
        assertTrue(storage.isEmpty());
        verify(plugin.configManager, never()).setConfiguration(eq("dayssincedeath"), anyString(), eq("record"), anyString());
    }

    @Test
    public void loggedInTimeExcludesLogoutAndResumesWithCoverageGap()
    {
        plugin.ensureProfile();
        plugin.clock = Clock.fixed(Instant.ofEpochMilli(START + 600), ZoneOffset.UTC);
        plugin.onGameTick(new GameTick());
        assertEquals(600, plugin.visibleRecord().loggedMillis);
        when(plugin.client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        plugin.ensureProfile();
        plugin.clock = Clock.fixed(Instant.ofEpochMilli(START + 3_600_000), ZoneOffset.UTC);
        when(plugin.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        plugin.ensureProfile();
        assertEquals(600, plugin.visibleRecord().loggedMillis);
        assertEquals(1, plugin.visibleRecord().gaps.size());
    }

    @Test
    public void delayedActionCannotModifyAnotherCharacter()
    {
        plugin.ensureProfile();
        profile.set("character-b"); plugin.ensureProfile();
        java.util.concurrent.atomic.AtomicBoolean ran = new java.util.concurrent.atomic.AtomicBoolean();
        assertFalse(plugin.applyEdit("character-a", () -> ran.set(true), false));
        assertFalse(ran.get()); assertEquals(0, plugin.visibleRecord().deaths);
    }

    @Test
    public void lmsExclusionRecordsHistoryWithoutResettingCurrentStreak()
    {
        plugin.config = new DaysSinceDeathConfig() { @Override public boolean ignoreLms() { return true; } };
        when(plugin.client.getWorldType()).thenReturn(java.util.EnumSet.of(net.runelite.api.WorldType.LAST_MAN_STANDING));
        plugin.ensureProfile(); plugin.onActorDeath(new ActorDeath(local));
        assertEquals(0, plugin.visibleRecord().deaths);
        assertEquals(1, plugin.visibleRecord().ignoredDeaths);
        assertEquals("IGNORED", plugin.visibleRecord().history.get(0).type);
    }
}
