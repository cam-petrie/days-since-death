# Days Since Death

A RuneLite plugin that records your own observed deaths and shows how long it
has been since the last one. Think of a workplace's “days without an incident”
sign, with separate records for each RuneScape character.

## Features

- An optional compact counter and a clock-icon sidebar with details.
- Automatic duration breakdown or total hours, days, weeks, or years.
- Longest recorded streak, including your current streak if it is the longest.
- Last observed death date and total observed deaths.
- Character-scoped storage using RuneLite's RuneScape profile configuration.
- A clear first-use state: “Tracking since first use” until a death is observed.
- No extra combat indicators, sounds, notifications, game actions, or external
  services. This plugin does not load the old Bandos FAHHHH plugin.

## What the counter means

This is **wall-clock time**, including time logged out. Years mean 365 days;
they are duration units rather than calendar anniversaries. The timer is shown
only when a character is logged in and its record has loaded.

The plugin cannot recover deaths that happened before installation, while it
was disabled, or while playing on another client/device. “Observed” is part of
the label for that reason. A successful character switch loads that character's
own record, not the previous character's.

All local-player `ActorDeath` events count, including safe deaths in minigames.
There is no unsafe-death filter in version 1. Duplicate events during the same
death are suppressed until the player is alive again.

Before the first observed death, the elapsed counter starts at first use, but
that partial interval is not treated as a complete personal-best streak. After
the first observed death, both current and completed intervals contribute to
the longest recorded streak. Corrupt saved data is preserved for recovery,
rather than silently overwritten.

## Build and run

Requires Java 11 or later; Java 17 or 21 works for development. The wrapper
downloads Gradle and dependencies on first use.

```sh
./gradlew test jar
./gradlew run
```

The `run` task launches a RuneLite development client with Days Since Death
loaded. macOS Java module access is configured automatically. Your existing
RuneLite profiles are available because the normal RuneLite configuration
directory is used. Enable **Days Since Death** in plugin settings. Choose
**Time format** and toggle **Show counter** there; the sidebar remains available
when the on-screen counter is hidden.

For a Jagex Account, use the development client's **Jagex Accounts** sidebar,
choose **Add Jagex Account**, authenticate, select a character, and choose
**Play Now**. See [RuneLite's login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

The JAR is `build/libs/days-since-death-1.0.0.jar`. It is a plugin artifact, not
a standalone game client; use the development launcher to load it.

## Verification and review status

Tests use synthetic events and saved records. They cover duration boundaries,
first use, streak resets, best records, backwards clock adjustments, account
switches, storage restoration, corrupt data, duplicate events, and filtering
deaths of other actors. No deliberate live-account death is needed to run them.

This plugin has not been submitted to or approved by the RuneLite Plugin Hub.
Its scope is historical statistics only. Consult [Jagex's guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
and [RuneLite's restrictions](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
before adding features. Passing tests is not approval. Repository guidance is
in [AGENTS.md](AGENTS.md).

## Optional GitHub Actions build

`docs/ci-workflow.example.yml` contains a build-and-test workflow. To activate
it, add it as `.github/workflows/build.yml` using GitHub's web editor or a token
with workflow permission. It is an example file, not an active workflow in
the initial repository; the available CLI token cannot upload workflow files.

## License

BSD 2-Clause. The Gradle wrapper comes from the RuneLite example-plugin setup
and Gradle; its embedded notices apply. No third-party audio or images are
bundled.
