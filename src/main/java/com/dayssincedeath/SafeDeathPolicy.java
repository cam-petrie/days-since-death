package com.dayssincedeath;

import java.util.Set;
import net.runelite.api.WorldType;

final class SafeDeathPolicy
{
    static String ignoredReason(Set<WorldType> types, boolean ignoreLms, boolean ignoreArena)
    {
        if (types == null) { return null; }
        if (ignoreLms && types.contains(WorldType.LAST_MAN_STANDING)) { return "Excluded: Last Man Standing world"; }
        if (ignoreArena && types.contains(WorldType.PVP_ARENA)) { return "Excluded: PvP Arena world"; }
        return null;
    }
}
