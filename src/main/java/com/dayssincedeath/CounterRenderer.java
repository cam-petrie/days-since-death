package com.dayssincedeath;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/** The game overlay and sidebar preview share the same content-sized renderer. */
final class CounterRenderer
{
    static String label(ViewSettings settings)
    {
        if (!settings.showLabel) { return ""; }
        return settings.labelStyle == LabelStyle.FULL ? "Last Death"
                : settings.labelStyle == LabelStyle.CLOCK ? "" : "LD";
    }

    static boolean icon(ViewSettings settings)
    {
        return settings.showLabel && (settings.labelStyle == LabelStyle.CLOCK || settings.labelStyle == LabelStyle.CLOCK_LD);
    }

    static Dimension size(FontMetrics metrics, ViewSettings settings, String counter)
    {
        String label = label(settings);
        int prefix = icon(settings) ? 12 : 0;
        if (!label.isEmpty()) { prefix += (prefix > 0 ? 4 : 0) + metrics.stringWidth(label); }
        return new Dimension(12 + prefix + (prefix > 0 ? 7 : 0) + metrics.stringWidth(counter), Math.max(12, metrics.getHeight()) + 8);
    }

    static Dimension render(Graphics2D graphics, ViewSettings settings, String counter)
    {
        FontMetrics metrics = graphics.getFontMetrics();
        Dimension size = size(metrics, settings, counter);
        graphics.setColor(new Color(25, 25, 25, Math.max(0, Math.min(255, settings.opacity))));
        graphics.fillRect(0, 0, size.width, size.height);
        graphics.setColor(settings.colour);
        int x = 6;
        int baseline = (size.height - metrics.getHeight()) / 2 + metrics.getAscent();
        if (icon(settings))
        {
            int y = (size.height - 12) / 2;
            graphics.drawOval(x, y, 11, 11);
            graphics.drawLine(x + 6, y + 2, x + 6, y + 6);
            graphics.drawLine(x + 6, y + 6, x + 9, y + 8);
            x += 12 + (label(settings).isEmpty() ? 0 : 4);
        }
        String label = label(settings);
        if (!label.isEmpty()) { graphics.drawString(label, x, baseline); x += metrics.stringWidth(label); }
        if (icon(settings) || !label.isEmpty()) { x += 7; }
        graphics.drawString(counter, x, baseline);
        return size;
    }
}
