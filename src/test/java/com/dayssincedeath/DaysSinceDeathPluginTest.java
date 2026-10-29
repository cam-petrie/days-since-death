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
        plugin.clock = Clock.fixed(Instant.ofEpochMilli(START), ZoneOffset.UTC);
        local = mock(Player.class);
        when(plugin.client.getLocalPlayer()).thenReturn(local);
        when(plugin.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(local.getName()).thenReturn("Test character");
        when(plugin.configManager.getRSProfileKey()).thenAnswer(inv -> profile.get());
        when(plugin.configManager.getRSProfileConfiguration("dayssincedeath", "record"))
                .thenAnswer(inv -> storage.get(profile.get()));
        doAnswer(inv -> { storage.put(profile.get(), inv.getArgument(2)); return null; })
                .when(plugin.configManager).setRSProfileConfiguration(eq("dayssincedeath"), eq("record"), anyString());
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
}
