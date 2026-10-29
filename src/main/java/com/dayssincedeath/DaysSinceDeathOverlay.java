package com.dayssincedeath;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

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
        panelComponent.setPreferredSize(new Dimension(230, 0));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(record.lastDeath == 0 ? "Tracking since first use" : "Since last observed death")
                .color(new Color(220, 190, 115)).build());
        long now = plugin.now();
        panelComponent.getChildren().add(LineComponent.builder().left("Elapsed")
                .right(config.displayUnit().format(record.elapsed(now))).build());
        if (record.lastDeath > 0)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Best recorded")
                    .right(config.displayUnit().format(record.best(now))).build());
        }
        return super.render(graphics);
    }
}
