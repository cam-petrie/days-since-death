package com.dayssincedeath;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class CounterRendererTest
{
    @Test
    public void hidingLabelLeavesOnlyTextAndNormalPadding()
    {
        Graphics2D g = new BufferedImage(400, 40, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            ViewSettings settings = new ViewSettings(); String counter = "8d 2h 9m 5s";
            int full = CounterRenderer.size(g.getFontMetrics(), settings, counter).width;
            settings.showLabel = false;
            for (LabelStyle style : LabelStyle.values())
            {
                settings.labelStyle = style;
                Dimension size = CounterRenderer.render(g, settings, counter);
                assertEquals(g.getFontMetrics().stringWidth(counter) + 12, size.width);
                assertTrue(size.width < full);
            }
        }
        finally { g.dispose(); }
    }

    @Test
    public void compactStylesUseLessSpaceThanFullLabel()
    {
        Graphics2D g = new BufferedImage(400, 40, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            ViewSettings s = new ViewSettings(); String counter = "3h 5m";
            int full = CounterRenderer.size(g.getFontMetrics(), s, counter).width;
            for (LabelStyle style : new LabelStyle[]{LabelStyle.LD, LabelStyle.CLOCK, LabelStyle.CLOCK_LD})
            {
                s.labelStyle = style;
                assertTrue(CounterRenderer.render(g, s, counter).width < full);
            }
        }
        finally { g.dispose(); }
    }
}
