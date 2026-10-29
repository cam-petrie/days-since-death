package com.dayssincedeath;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(DaysSinceDeathConfig.GROUP)
public interface DaysSinceDeathConfig extends Config
{
    String GROUP = "dayssincedeath";

    @ConfigItem(keyName = "showOverlay", name = "Show counter", description = "Show the historical counter on the game view", position = 0)
    default boolean showOverlay() { return true; }

    @ConfigItem(keyName = "displayUnit", name = "Time format", description = "Automatic breakdown or total hours, days, weeks, or 365-day years", position = 1)
    default DisplayUnit displayUnit() { return DisplayUnit.AUTOMATIC; }
}
