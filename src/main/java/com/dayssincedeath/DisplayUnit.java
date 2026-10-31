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

    public String format(long millis, boolean showSeconds)
    {
        long seconds = Math.max(0, millis / 1000);
        if (this != AUTOMATIC) { return format(millis); }
        if (!showSeconds) { return seconds < 3600 ? seconds / 60 + "m" : format(millis); }
        long days = seconds / 86400;
        StringBuilder value = new StringBuilder();
        if (days >= 365) { value.append(days / 365).append("y "); days %= 365; }
        if (days >= 7 || value.length() > 0) { value.append(days / 7).append("w "); days %= 7; }
        if (days > 0 || value.length() > 0) { value.append(days).append("d "); }
        if (seconds >= 3600) { value.append(seconds / 3600 % 24).append("h "); }
        value.append(seconds / 60 % 60).append("m ").append(seconds % 60).append("s");
        return value.toString();
    }

}
