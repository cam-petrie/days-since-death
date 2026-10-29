package com.dayssincedeath;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "Days Since Death", description = "Track time since your last observed death and your longest recorded streak", tags = {"death", "streak", "timer", "statistics"})
public class DaysSinceDeathPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(DaysSinceDeathPlugin.class);
    static final String STATE_KEY = "record";

    @Inject Client client;
    @Inject ConfigManager configManager;
    @Inject Gson gson;
    @Inject private ClientThread clientThread;
    @Inject private DaysSinceDeathConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private DaysSinceDeathOverlay overlay;
    @Inject private ClientToolbar toolbar;

    Clock clock = Clock.systemUTC();
    private volatile DeathRecord record;
    private volatile boolean loggedIn;
    private volatile String character = "";
    private volatile String error;
    private String loadedProfile;
    private boolean deathLatched;
    private NavigationButton navigation;
    private Timer uiTimer;
    private JLabel heading;
    private JLabel elapsed;
    private JLabel best;
    private JLabel last;
    private JLabel count;

    @Provides
    DaysSinceDeathConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(DaysSinceDeathConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        onEdt(() -> {
            PluginPanel panel = new PluginPanel() {};
            panel.setLayout(new BorderLayout(0, 16));
            panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            JPanel details = new JPanel(new GridLayout(0, 1, 0, 12));
            heading = new JLabel("Days Since Death");
            heading.setForeground(new Color(220, 190, 115));
            elapsed = new JLabel();
            elapsed.setFont(elapsed.getFont().deriveFont(Font.BOLD, 19f));
            best = new JLabel();
            last = new JLabel();
            count = new JLabel();
            details.add(heading);
            details.add(elapsed);
            details.add(best);
            details.add(last);
            details.add(count);
            panel.add(details, BorderLayout.NORTH);
            JLabel note = new JLabel("<html>Wall-clock time, including offline time.<br><br>Only deaths seen while this plugin runs are recorded. Safe minigame deaths also count.<br><br>Choose hours, days, weeks, or years in plugin settings.</html>");
            note.setForeground(Color.LIGHT_GRAY);
            panel.add(note, BorderLayout.CENTER);
            BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setColor(new Color(220, 190, 115));
            g.drawOval(3, 3, 25, 25);
            g.drawLine(16, 7, 16, 16);
            g.drawLine(16, 16, 23, 19);
            g.dispose();
            navigation = NavigationButton.builder().tooltip("Days Since Death").icon(icon).panel(panel).priority(8).build();
            toolbar.addNavigation(navigation);
            uiTimer = new Timer(1000, e -> refreshPanel());
            uiTimer.start();
            refreshPanel();
        });
        clientThread.invoke(this::ensureProfile);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        loggedIn = false;
        record = null;
        loadedProfile = null;
        deathLatched = false;
        error = null;
        onEdt(() -> {
            if (uiTimer != null) { uiTimer.stop(); uiTimer = null; }
            if (navigation != null) { toolbar.removeNavigation(navigation); navigation = null; }
        });
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        ensureProfile();
        Player player = client.getLocalPlayer();
        if (player != null && !player.isDead()) { deathLatched = false; }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        loggedIn = event.getGameState() == GameState.LOGGED_IN;
        if (loggedIn) { ensureProfile(); }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            record = null;
            loadedProfile = null;
            deathLatched = false;
            error = null;
        }
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        // Do not display or write the previous character's state during a switch.
        record = null;
        loadedProfile = null;
        deathLatched = false;
        error = null;
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        Player player = client.getLocalPlayer();
        if (player == null || event.getActor() != player) { return; }
        ensureProfile();
        if (!loggedIn || record == null || deathLatched) { return; }
        deathLatched = true;
        record = record.died(now());
        save();
    }

    void ensureProfile()
    {
        loggedIn = client.getGameState() == GameState.LOGGED_IN;
        if (!loggedIn) { return; }
        String key = configManager.getRSProfileKey();
        if (key == null)
        {
            record = null;
            loadedProfile = null;
            return;
        }
        Player player = client.getLocalPlayer();
        character = player == null ? "" : player.getName();
        if (Objects.equals(key, loadedProfile)) { return; }
        loadedProfile = key;
        record = null;
        deathLatched = false;
        error = null;
        String saved = configManager.getRSProfileConfiguration(DaysSinceDeathConfig.GROUP, STATE_KEY);
        if (saved == null)
        {
            record = DeathRecord.start(now());
            save();
            return;
        }
        try
        {
            DeathRecord restored = gson.fromJson(saved, DeathRecord.class);
            if (restored == null || !restored.valid()) { throw new IllegalArgumentException("Invalid death record"); }
            record = restored;
        }
        catch (RuntimeException e)
        {
            // Preserve the original saved value for recovery instead of resetting it.
            error = "Saved record unreadable; left untouched.";
            log.warn("Days Since Death could not read the saved record", e);
        }
    }

    private void save()
    {
        if (record != null && Objects.equals(loadedProfile, configManager.getRSProfileKey()))
        {
            configManager.setRSProfileConfiguration(DaysSinceDeathConfig.GROUP, STATE_KEY, gson.toJson(record));
        }
    }

    long now() { return clock.millis(); }

    DeathRecord visibleRecord() { return loggedIn ? record : null; }

    private void refreshPanel()
    {
        DeathRecord current = visibleRecord();
        heading.setText(current == null ? "Days Since Death" : character);
        elapsed.setText(current == null
                ? error != null ? "Record unavailable" : loggedIn ? "Loading character…" : "Log in to begin"
                : config.displayUnit().format(current.elapsed(now())));
        elapsed.setToolTipText(elapsed.getText());
        best.setText(current == null ? "" : current.lastDeath == 0 ? "No full streak recorded yet" : "Best: " + config.displayUnit().format(current.best(now())));
        best.setToolTipText(best.getText());
        if (current == null)
        {
            last.setText(error == null ? "Waiting for your character." : error);
            count.setText("");
            return;
        }
        long date = current.lastDeath == 0 ? current.trackingStarted : current.lastDeath;
        String stamp = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));
        last.setText("<html>" + (current.lastDeath == 0 ? "Tracking started" : "Last observed death") + "<br>" + stamp + "</html>");
        count.setText("Observed deaths: " + current.deaths);
    }

    private static void onEdt(Runnable action) throws Exception
    {
        if (SwingUtilities.isEventDispatchThread()) { action.run(); }
        else { SwingUtilities.invokeAndWait(action); }
    }
}
