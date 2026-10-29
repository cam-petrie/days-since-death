package com.dayssincedeath;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

final class DaysSinceDeathOverlay extends OverlayPanel
{
    private final DaysSinceDeathPlugin plugin;
    private final DaysSinceDeathConfig config;

    @Inject
    DaysSinceDeathOverlay(DaysSinceDeathPlugin plugin, DaysSinceDeathConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        DeathRecord record = plugin.visibleRecord();
        if (!config.showOverlay() || record == null) { return null; }
        String counter = record.lastDeath == 0 ? "Unknown" : config.displayUnit().format(record.elapsed(plugin.now()));
        int width = graphics.getFontMetrics().stringWidth("Last Death")
                + graphics.getFontMetrics().stringWidth(counter) + 20;
        panelComponent.setPreferredSize(new Dimension(Math.max(130, width), 0));
        panelComponent.getChildren().add(LineComponent.builder().left("Last Death").right(counter).build());
        return super.render(graphics);
    }
}
