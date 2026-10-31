# Days Since Death

A RuneLite plugin by **sheriff-cam** that records your own observed deaths and
shows elapsed time since the last one. Records are separate for each character.

## Version 2 features

- Compact, content-sized counter with a full **Last Death** label, **LD**, clock
  icon, clock + LD, or no label. Choose text colour and background opacity.
- Automatic time breakdown or total hours, days, weeks, and 365-day years.
  Automatic format can show seconds at every duration; total-unit formats stay numeric.
- Real-world elapsed time or observed logged-in survival time, with personal bests.
- Optional exclusions for deaths on dedicated Last Man Standing and PvP Arena
  worlds. All other contexts count. Excluded deaths remain in history.
- Up to 100 recent death/correction events with dates, region/tile coordinates,
  previous streak lengths, and editable personal notes (up to 500 characters).
- Undo the latest counted death after two confirmation prompts. The event remains
  in history as UNDONE, and the record is labelled as user-corrected.
- Manually supply a known last-death date, labelled as manual. Future dates are
  rejected, and unknown historical logged-in time is never invented.
- JSON backup export, validated restore with a character/data preview, and a
  recovery snapshot of the record from before the last restore.
- Tracking coverage: last seen online, observed playtime, and the most recent
  20 unobserved intervals. Ordinary offline time also produces gaps.
- Manually generated PNG streak cards with a preview and character name hidden
  by default. Cards and backups are saved only to destinations you select.

## Use

Install **Days Since Death** from RuneLite's Plugin Hub. Open the clock sidebar
for the Record, Log, Gaps, and Setup tabs. Display toggles apply immediately;
use Apply options after adjusting the colour or opacity. The native plugin
settings also expose the counter and exclusion preferences.

Before the first observed or manually supplied death, the counter says Unknown.
The sidebar separately shows time since tracking began. A `+` on observed playtime
means the earlier part of that streak was not measured (for example, after a
version 1 migration or manually entered date).

## Accuracy, storage, and migration

Real-world time includes time logged out. Logged-in time measures only intervals
observed while the plugin is active; loading/hopping interruptions, offline time,
and long stalls do not accrue it. Years are 365-day durations, not anniversaries.

The plugin cannot recover deaths before installation, while it is disabled, or
on another client/device without the plugin. A coverage gap does not prove a
death occurred. No killer or cause is guessed from combat events.

Character data uses RuneLite's standard configuration storage and its usual
account-sync behaviour. The plugin has no separate service or network client.
Backups include character names and personal notes; anonymous PNG cards do not.

Version 2 first loads an existing v2 record. Otherwise it migrates the published
v1 last-death timestamp, count, and best recorded interval into a separate v2
configuration key. The original v1 value is preserved. Historical logged-in time
and detailed history cannot be recovered from v1. Invalid saved data is left
untouched rather than silently reset.

Experimental `dayssincedeathlocal` data is not automatically imported into the
release. To bring across a preview record, export its JSON backup in the preview
and explicitly restore it in the release. The restore dialog shows the source
and target character before replacement.

## Development

Requires Java 11+, with Java 17 or 21 recommended for development. The macOS
Java desktop module flag is configured automatically.

```sh
./gradlew test jar
./gradlew run
```

The development client uses the normal RuneLite configuration directory. Enable
Days Since Death in plugin settings. For Jagex Accounts, use the client's Jagex
Accounts sidebar; see [RuneLite's official login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

The artifact is `build/libs/days-since-death-2.0.0.jar`. It is a plugin, not a
standalone game client. Updates reach the Plugin Hub after maintainer review.

## Offline sample interface

```sh
./gradlew preview
```

This opens a standalone sample interface without starting RuneLite or reading
or writing a character profile. Sample edits remain in memory; exports only
write files you explicitly choose. Sample death buttons do not interact with
RuneScape. To render reference images instead of opening a window:

```sh
./gradlew preview --args="--render=/tmp/days-since-death-preview"
```

Tests use synthetic records and events. They cover streaks, durations, display
geometry, migrations, character switching, production/experimental-data isolation,
logged-in time, undo, exclusions, bounded history, invalid backups, and anonymous
cards. No deliberate live-account deaths are required.

The scope is historical statistics only: no gameplay inputs, extra boss cues,
notifications, window manipulation, chat changes, or runtime network requests.
See [AGENTS.md](AGENTS.md) for development constraints and [CHANGELOG.md](CHANGELOG.md)
for release notes. The optional workflow in `docs/ci-workflow.example.yml` has
not been activated; local checks and RuneLite's submission pipeline verify builds.

## License

BSD 2-Clause. Gradle wrapper notices apply to the wrapper. No third-party audio
or images are bundled.
