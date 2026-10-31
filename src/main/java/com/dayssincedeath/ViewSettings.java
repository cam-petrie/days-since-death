package com.dayssincedeath;

import java.awt.Color;

final class ViewSettings
{
    DisplayUnit unit = DisplayUnit.AUTOMATIC;
    TimeBasis basis = TimeBasis.REAL_WORLD;
    boolean showOverlay = true;
    boolean showLabel = true;
    boolean showSeconds = true;
    boolean ignoreLms;
    boolean ignoreArena;
    Color colour = Color.WHITE;
    int opacity = 160;

    static ViewSettings from(DaysSinceDeathConfig c)
    {
        ViewSettings s = new ViewSettings();
        s.unit = c.displayUnit(); s.basis = c.timeBasis(); s.showOverlay = c.showOverlay();
        s.showLabel = c.showLabel(); s.showSeconds = c.showSeconds();
        s.ignoreLms = c.ignoreLms(); s.ignoreArena = c.ignoreArena();
        s.colour = c.textColor(); s.opacity = c.backgroundOpacity();
        return s;
    }

    String counter(TrackerState state, long now)
    {
        if (state.lastDeath == 0) { return "Unknown"; }
        long value = basis == TimeBasis.LOGGED_IN ? state.currentPlayMillis : state.elapsed(now);
        return unit.format(value, showSeconds) + (basis == TimeBasis.LOGGED_IN && state.playtimePartial ? "+" : "");
    }
}
