package com.dayssincedeath;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class DaysSinceDeathLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(DaysSinceDeathPlugin.class);
        RuneLite.main(args);
    }
}
