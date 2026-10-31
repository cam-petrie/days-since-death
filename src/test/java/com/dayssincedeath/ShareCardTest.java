package com.dayssincedeath;

import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShareCardTest
{
    @Test
    public void anonymousCardDoesNotDependOnTheCharacterName()
    {
        long now = 1_700_000_000_000L;
        TrackerState state = PreviewLauncher.sample(now);
        TrackerView a = new TrackerView("a", "Private Name A", state, now, "", new ViewSettings());
        TrackerView b = new TrackerView("b", "Other Name B", state, now, "", new ViewSettings());
        BufferedImage anonymousA = ShareCard.render(a, false);
        BufferedImage anonymousB = ShareCard.render(b, false);
        assertArrayEquals(pixels(anonymousA), pixels(anonymousB));
        assertFalse(java.util.Arrays.equals(pixels(ShareCard.render(a, true)), pixels(ShareCard.render(b, true))));
    }

    private static int[] pixels(BufferedImage image)
    { return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()); }
}
