package com.dayssincedeath;

import org.junit.Test;
import static org.junit.Assert.*;

public class DisplayUnitTest
{
    private static final long HOUR = 3_600_000;
    private static final long DAY = 24 * HOUR;

    @Test
    public void automaticFormatCrossesEachBoundary()
    {
        assertEquals("0m 0s", DisplayUnit.AUTOMATIC.format(-1));
        assertEquals("1m 1s", DisplayUnit.AUTOMATIC.format(61_000));
        assertEquals("1h 0m", DisplayUnit.AUTOMATIC.format(HOUR));
        assertEquals("1d 2h 0m", DisplayUnit.AUTOMATIC.format(DAY + 2 * HOUR));
        assertEquals("1w 1d 2h", DisplayUnit.AUTOMATIC.format(8 * DAY + 2 * HOUR));
        assertEquals("1y 1w 1d", DisplayUnit.AUTOMATIC.format(373 * DAY));
    }

    @Test
    public void selectedUnitsDisplayTotals()
    {
        assertEquals("36.00 hours", DisplayUnit.HOURS.format(DAY + 12 * HOUR));
        assertEquals("1.50 days", DisplayUnit.DAYS.format(DAY + 12 * HOUR));
        assertEquals("2.00 weeks", DisplayUnit.WEEKS.format(14 * DAY));
        assertEquals("2.00 years", DisplayUnit.YEARS.format(730 * DAY));
    }

    @Test
    public void secondsToggleWorksBeyondAnHourAndAcrossAllAutomaticUnits()
    {
        long value = 373 * DAY + 2 * HOUR + 3 * 60_000 + 9000;
        assertEquals("1y 1w 1d 2h 3m 9s", DisplayUnit.AUTOMATIC.format(value, true));
        assertEquals("1y 1w 1d", DisplayUnit.AUTOMATIC.format(value, false));
        assertEquals("1w 1d 2h 3m 9s", DisplayUnit.AUTOMATIC.format(8 * DAY + 2 * HOUR + 189_000, true));
        assertEquals("1d 2h 3m 9s", DisplayUnit.AUTOMATIC.format(DAY + 2 * HOUR + 189_000, true));
        assertEquals("2h 3m 9s", DisplayUnit.AUTOMATIC.format(2 * HOUR + 189_000, true));
        assertEquals("2h 3m", DisplayUnit.AUTOMATIC.format(2 * HOUR + 189_000, false));
        assertEquals("3m", DisplayUnit.AUTOMATIC.format(189_000, false));
    }
}
