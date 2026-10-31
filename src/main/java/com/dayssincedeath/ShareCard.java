package com.dayssincedeath;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class ShareCard
{
    static BufferedImage render(TrackerView view, boolean includeName)
    {
        if (view.state == null) { throw new IllegalArgumentException("No record to share"); }
        TrackerState s = view.state;
        BufferedImage image = new BufferedImage(1000, 560, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(24, 29, 31)); g.fillRect(0, 0, 1000, 560);
        g.setColor(new Color(213, 185, 119)); g.fillRect(48, 45, 5, 440);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        g.drawString("DAYS SINCE DEATH", 80, 88);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
        g.drawString(view.settings.counter(s, view.now), 80, 180);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 23));
        String basis = view.settings.basis == TimeBasis.LOGGED_IN ? "Observed logged-in survival time" : "Real-world time since last recorded death";
        g.drawString(basis, 80, 226);
        g.setColor(new Color(183, 194, 197));
        g.drawString(includeName ? "Character: " + view.character : "Character name hidden", 80, 284);
        String best = s.lastDeath == 0 ? "Not established" : view.settings.unit.format(
                view.settings.basis == TimeBasis.LOGGED_IN ? s.bestPlay() : s.best(view.now));
        g.drawString("Best recorded: " + best, 80, 329);
        g.drawString("Counted deaths: " + s.deaths + "  |  Excluded: " + s.ignoredDeaths, 80, 372);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        String source = s.lastDeath == 0 ? "No last death recorded" : s.manualDate ? "Last-death date manually supplied" : "Last-death date observed by the plugin";
        g.drawString(source + (s.corrected ? " · User-corrected record" : ""), 80, 428);
        g.drawString("Deaths outside tracking are unknown. + means partial playtime.", 80, 461);
        String date = Instant.ofEpochMilli(view.now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        g.drawString("Generated " + date + " · sheriff-cam · Local preview", 80, 504);
        g.dispose();
        return image;
    }
}
