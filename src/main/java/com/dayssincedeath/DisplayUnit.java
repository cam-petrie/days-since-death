package com.dayssincedeath;

import java.util.Locale;

public enum DisplayUnit
{
    AUTOMATIC, HOURS, DAYS, WEEKS, YEARS;

    public String format(long millis)
    {
        long seconds = Math.max(0, millis / 1000);
        if (this != AUTOMATIC)
        {
            double divisor = this == HOURS ? 3600 : this == DAYS ? 86400
                    : this == WEEKS ? 604800 : 31536000;
            return String.format(Locale.US, "%.2f %s", seconds / divisor, name().toLowerCase(Locale.ROOT));
        }
        long days = seconds / 86400;
        long hours = seconds / 3600 % 24;
        long minutes = seconds / 60 % 60;
        if (days >= 365) { return days / 365 + "y " + days % 365 / 7 + "w " + days % 365 % 7 + "d"; }
        if (days >= 7) { return days / 7 + "w " + days % 7 + "d " + hours + "h"; }
        if (days > 0) { return days + "d " + hours + "h " + minutes + "m"; }
        if (hours > 0) { return hours + "h " + minutes + "m"; }
        return minutes + "m " + seconds % 60 + "s";
    }
}
