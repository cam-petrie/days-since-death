package com.dayssincedeath;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "Days Since Death",
        description = "Death history, corrections, backups, and survival time", tags = {"death", "streak", "timer", "statistics"})
public class DaysSinceDeathPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(DaysSinceDeathPlugin.class);
    static final String STATE_KEY = "recordV2";
    @Inject Client client;
    @Inject ConfigManager configManager;
    @Inject Gson gson;
    @Inject DaysSinceDeathConfig config;
    @Inject private ClientThread clientThread;
    @Inject private OverlayManager overlayManager;
    @Inject private DaysSinceDeathOverlay overlay;
    @Inject private ClientToolbar toolbar;
    Clock clock = Clock.systemUTC();
    private TrackerState state;
    private String loadedProfile;
    private String character = "";
    private String error;
    private boolean collecting;
    private boolean loggedIn;
    private boolean deathLatched;
    private long lastHeartbeat;
    private long nextSave;
    private volatile TrackerView currentView;
    private DaysSinceDeathPanel panel;
    private NavigationButton navigation;
    private Timer uiTimer;

    @Provides
    DaysSinceDeathConfig provideConfig(ConfigManager manager) { return manager.getConfig(DaysSinceDeathConfig.class); }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        publish();
        onEdt(() -> {
            panel = new DaysSinceDeathPanel(new DaysSinceDeathPanel.Actions()
            {
                public void undo(String profile) { edit(profile, () -> state.undoDeath()); }
                public void manualDate(String profile, long at) { edit(profile, () -> state.setManualDate(at, now())); }
                public void note(String profile, String id, String text) { edit(profile, () -> state.setNote(id, text)); }
                public void restore(String profile, TrackerState restored) { edit(profile, () -> restoreRecord(restored), true); }
                public void recover(String profile) { edit(profile, () -> {
                    String saved = configManager.getConfiguration(DaysSinceDeathConfig.GROUP, loadedProfile, "beforeRestore");
                    if (saved == null) { throw new IllegalStateException("No pre-restore recovery snapshot has been saved yet"); }
                    TrackerState recovered = gson.fromJson(saved, TrackerState.class);
                    if (recovered == null) { throw new IllegalStateException("Recovery snapshot is empty"); }
                    restoreRecord(recovered);
                }, true); }
                public void settings(ViewSettings options) { clientThread.invoke(() -> applyOptions(options)); }
            }, gson);
            BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setColor(new Color(220, 190, 115)); g.drawOval(3, 3, 25, 25);
            g.drawLine(16, 7, 16, 16); g.drawLine(16, 16, 23, 19); g.dispose();
            navigation = NavigationButton.builder().tooltip("Days Since Death")
                    .icon(icon).panel(panel).priority(8).build();
            toolbar.addNavigation(navigation);
            uiTimer = new Timer(1000, e -> panel.update(view())); uiTimer.start();
            panel.update(view());
        });
        clientThread.invoke(this::ensureProfile);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        pauseTracking();
        loadedProfile = null; state = null; publish();
        onEdt(() -> {
            if (uiTimer != null) { uiTimer.stop(); uiTimer = null; }
            if (navigation != null) { toolbar.removeNavigation(navigation); navigation = null; }
            panel = null;
        });
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        ensureProfile();
        heartbeat();
        Player player = client.getLocalPlayer();
        if (player != null && !player.isDead()) { deathLatched = false; }
        if (collecting && state != null && now() >= nextSave) { save(); nextSave = now() + 15_000; }
        publish();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN) { ensureProfile(); }
        else { pauseTracking(); }
        publish();
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        pauseTracking(); loadedProfile = null; state = null; error = null; deathLatched = false;
        publish();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (DaysSinceDeathConfig.GROUP.equals(event.getGroup()) && !STATE_KEY.equals(event.getKey()))
        { clientThread.invoke(this::publish); }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        Player player = client.getLocalPlayer();
        if (player == null || event.getActor() != player) { return; }
        ensureProfile();
        if (!collecting || state == null || deathLatched) { return; }
        heartbeat(); deathLatched = true;
        String reason = SafeDeathPolicy.ignoredReason(client.getWorldType(), config.ignoreLms(), config.ignoreArena());
        state.died(now(), location(player), reason);
        save(); publish();
    }

    private String location(Player player)
    {
        WorldPoint point = player.getWorldLocation();
        if (client.isInInstancedRegion() && player.getLocalLocation() != null)
        { point = WorldPoint.fromLocalInstance(client, player.getLocalLocation()); }
        return point == null ? "Location unavailable" : "Region " + point.getRegionID()
                + " · " + point.getX() + ", " + point.getY() + " · level " + point.getPlane();
    }

    void ensureProfile()
    {
        if (client.getGameState() != GameState.LOGGED_IN) { pauseTracking(); publish(); return; }
        String key = configManager.getRSProfileKey();
        if (key == null) { pauseTracking(); loadedProfile = null; state = null; publish(); return; }
        if (!Objects.equals(key, loadedProfile))
        {
            pauseTracking(); loadedProfile = key; state = null; deathLatched = false; error = null;
            String saved = configManager.getConfiguration(DaysSinceDeathConfig.GROUP, key, STATE_KEY);
            try
            {
                if (saved != null)
                {
                    state = gson.fromJson(saved, TrackerState.class);
                    if (state == null) { throw new IllegalArgumentException("Empty record"); }
                    state.validate(now());
                }
                else
                {
                    // Upgrade the published v1 record without overwriting the original value.
                    String original = configManager.getConfiguration(DaysSinceDeathConfig.GROUP, key, "record");
                    state = original == null ? TrackerState.start(now())
                            : TrackerState.migrate(gson.fromJson(original, DeathRecord.class), now());
                }
                state.validate(now());
            }
            catch (RuntimeException e)
            {
                state = null; error = "Saved record unreadable; left untouched.";
                log.warn("Could not load Days Since Death record", e);
            }
        }
        Player player = client.getLocalPlayer();
        character = player == null || player.getName() == null ? "Character" : player.getName();
        loggedIn = true;
        if (state != null && !collecting)
        {
            state.resumed(now()); collecting = true; lastHeartbeat = now(); nextSave = now() + 15_000; save();
        }
        publish();
    }

    private void heartbeat()
    {
        if (!collecting || state == null) { return; }
        long timestamp = now();
        long delta = timestamp - lastHeartbeat;
        if (lastHeartbeat > 0 && delta >= 0 && delta <= 5000) { state.addPlaytime(delta); }
        else if (delta > 5000) { state.resumed(timestamp); }
        state.lastSeen = Math.max(state.lastSeen, timestamp); lastHeartbeat = timestamp;
    }

    private void pauseTracking()
    {
        if (collecting && state != null) { heartbeat(); collecting = false; save(); }
        collecting = false; lastHeartbeat = 0;
        loggedIn = false;
    }

    private void save()
    {
        if (state != null && loadedProfile != null)
        { configManager.setConfiguration(DaysSinceDeathConfig.GROUP, loadedProfile, STATE_KEY, gson.toJson(state)); }
    }

    private void edit(String profile, Runnable mutation)
    {
        edit(profile, mutation, false);
    }

    private void edit(String profile, Runnable mutation, boolean allowMissing)
    {
        clientThread.invoke(() -> { applyEdit(profile, mutation, allowMissing); });
    }

    boolean applyEdit(String profile, Runnable mutation, boolean allowMissing)
    {
            TrackerState previous = state == null ? null : state.copy();
            try
            {
                if (profile == null || !Objects.equals(profile, loadedProfile)
                        || !Objects.equals(profile, configManager.getRSProfileKey()) || !loggedIn)
                { throw new IllegalStateException("Character changed. Reopen the action for the current character."); }
                if (state == null && !allowMissing) { throw new IllegalStateException("No readable record is loaded"); }
                heartbeat(); previous = state == null ? null : state.copy();
                mutation.run(); state.validate(now()); save(); publish(); notifyPanel("Record saved");
                return true;
            }
            catch (RuntimeException e) { state = previous; publish(); notifyPanel(e.getMessage()); return false; }
    }

    private void restoreRecord(TrackerState restored)
    {
        restored.validate(now());
        if (state != null) { configManager.setConfiguration(DaysSinceDeathConfig.GROUP, loadedProfile, "beforeRestore", gson.toJson(state)); }
        state = restored.copy(); state.undo = null; state.corrected = true;
        state.resumed(now()); lastHeartbeat = now(); collecting = true; error = null;
    }

    private void applyOptions(ViewSettings s)
    {
        String group = DaysSinceDeathConfig.GROUP;
        configManager.setConfiguration(group, "displayUnit", s.unit);
        configManager.setConfiguration(group, "timeBasis", s.basis);
        configManager.setConfiguration(group, "showOverlay", s.showOverlay);
        configManager.setConfiguration(group, "showLabel", s.showLabel);
        configManager.setConfiguration(group, "labelStyle", s.labelStyle);
        configManager.setConfiguration(group, "showSeconds", s.showSeconds);
        configManager.setConfiguration(group, "ignoreLms", s.ignoreLms);
        configManager.setConfiguration(group, "ignoreArena", s.ignoreArena);
        configManager.setConfiguration(group, "backgroundOpacity", s.opacity);
        configManager.setConfiguration(group, "textColor", s.colour);
        publish(); notifyPanel("Options applied");
    }

    private void notifyPanel(String text)
    {
        SwingUtilities.invokeLater(() -> { if (panel != null) { panel.message(text); } });
    }

    private void publish()
    {
        ViewSettings settings = config == null ? new ViewSettings() : ViewSettings.from(config);
        currentView = new TrackerView(loggedIn ? loadedProfile : null, character,
                collecting && state != null ? state.copy() : null, now(),
                error != null ? error : collecting ? "Loading character…" : "Log in to begin", settings);
    }

    long now() { return clock.millis(); }
    TrackerState visibleRecord() { return view().state; }
    TrackerView view()
    {
        TrackerView v = currentView;
        return v == null ? new TrackerView(null, "", null, now(), "Log in to begin", new ViewSettings()) : v;
    }

    private static void onEdt(Runnable action) throws Exception
    {
        if (SwingUtilities.isEventDispatchThread()) { action.run(); }
        else { SwingUtilities.invokeAndWait(action); }
    }
}
