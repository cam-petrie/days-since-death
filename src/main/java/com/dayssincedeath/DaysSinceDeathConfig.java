package com.dayssincedeath;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import java.awt.Color;

@ConfigGroup(DaysSinceDeathConfig.GROUP)
public interface DaysSinceDeathConfig extends Config
{
    String GROUP = "dayssincedeath";

    @ConfigItem(keyName = "showOverlay", name = "Show counter", description = "Show the historical counter on the game view", position = 0)
    default boolean showOverlay() { return true; }

    @ConfigItem(keyName = "displayUnit", name = "Time format", description = "Automatic breakdown or total hours, days, weeks, or 365-day years", position = 1)
    default DisplayUnit displayUnit() { return DisplayUnit.AUTOMATIC; }

    @ConfigItem(keyName = "timeBasis", name = "Counter basis", description = "Real-world elapsed time or observed logged-in time", position = 2)
    default TimeBasis timeBasis() { return TimeBasis.REAL_WORLD; }

    @ConfigItem(keyName = "showLabel", name = "Show Last Death label", description = "Hide the label for a smaller counter", position = 3)
    default boolean showLabel() { return true; }

    @ConfigItem(keyName = "labelStyle", name = "Label style", description = "Full label, LD, clock icon, or clock icon plus LD", position = 4)
    default LabelStyle labelStyle() { return LabelStyle.FULL; }

    @ConfigItem(keyName = "showSeconds", name = "Show seconds", description = "Include seconds at every duration in Automatic format; total-unit formats stay numeric", position = 5)
    default boolean showSeconds() { return true; }

    @ConfigItem(keyName = "textColor", name = "Counter colour", description = "Text colour of this plugin's counter", position = 6)
    default Color textColor() { return Color.WHITE; }

    @Range(min = 0, max = 255)
    @ConfigItem(keyName = "backgroundOpacity", name = "Background opacity", description = "0 is transparent; 255 is opaque", position = 7)
    default int backgroundOpacity() { return 160; }

    @ConfigItem(keyName = "ignoreLms", name = "Exclude LMS deaths", description = "Ignore deaths on dedicated Last Man Standing worlds", position = 8)
    default boolean ignoreLms() { return false; }

    @ConfigItem(keyName = "ignoreArena", name = "Exclude PvP Arena deaths", description = "Ignore deaths on dedicated PvP Arena worlds", position = 9)
    default boolean ignoreArena() { return false; }
}
