package com.dayssincedeath;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.api.Client;

final class DaysSinceDeathOverlay extends Overlay
{
    private final DaysSinceDeathPlugin plugin;
    private final DaysSinceDeathConfig config;
    private final Client client;
    private final TooltipManager tooltips;

    @Inject
    DaysSinceDeathOverlay(DaysSinceDeathPlugin plugin, DaysSinceDeathConfig config, Client client, TooltipManager tooltips)
    {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        this.tooltips = tooltips;
        setPosition(OverlayPosition.TOP_LEFT);
        setResizable(false);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        TrackerView view = plugin.view();
        TrackerState record = view.state;
        if (!config.showOverlay() || record == null) { return null; }
        ViewSettings settings = ViewSettings.from(config);
        String counter = settings.counter(record, plugin.now());
        Dimension dimensions = CounterRenderer.render(graphics, settings, counter);
        net.runelite.api.Point mouse = client.getMouseCanvasPosition();
        if (mouse != null && getBounds().contains(mouse.getX(), mouse.getY()))
        {
            String stamp = record.lastDeath == 0 ? "Unknown" : DaysSinceDeathPanel.stamp(record.lastDeath);
            tooltips.add(new Tooltip("Last Death: " + stamp + "<br>Source: "
                    + (record.manualDate ? "manually supplied" : record.lastDeath == 0 ? "not established" : "observed")
                    + "<br>Best: " + (record.lastDeath == 0 ? "Unknown" : DisplayUnit.AUTOMATIC.format(record.best(plugin.now())))
                    + "<br>Logged-in streak: " + DisplayUnit.AUTOMATIC.format(record.currentPlayMillis)
                    + (record.playtimePartial ? " (partial)" : "")
                    + "<br>Deaths outside tracking are unknown."));
        }
        return dimensions;
    }
}
