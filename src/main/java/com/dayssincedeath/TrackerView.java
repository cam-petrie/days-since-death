package com.dayssincedeath;

final class TrackerView
{
    final String profile;
    final String character;
    final TrackerState state;
    final long now;
    final String message;
    final ViewSettings settings;

    TrackerView(String profile, String character, TrackerState state, long now, String message, ViewSettings settings)
    {
        this.profile = profile; this.character = character; this.state = state;
        this.now = now; this.message = message; this.settings = settings;
    }
}
