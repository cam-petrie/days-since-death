# Days Since Death — local preview

This working branch contains the **local v2 preview** requested on October 31,
2026. Nothing from this preview has been pushed to GitHub or submitted to the
RuneLite Plugin Hub. The published plugin remains unchanged.

## View it without logging in

```sh
./gradlew preview
```

This opens a standalone sample interface. It never starts RuneLite, logs into
an account, or reads/writes any RuneLite character record. Sample counted and
LMS deaths, undo, manual dates, notes, settings, backup/restore, and card export
can all be exercised here. Sample edits stay in memory; exported files are
written only when you choose a destination. Reset samples restores the fixture.

## What is included

1. **Safe-death exclusions:** optional dedicated Last Man Standing and PvP Arena
   world exclusions. Defaults still count all deaths. This is not a universal
   safe-death detector: other minigames, practice modes, ordinary PvP worlds,
   and unrecognised contexts still count. Excluded events remain in history.
2. **Undo:** restore the streak before the most recent counted death. The event
   remains in history as `UNDONE`; personal-best and playtime values are restored,
   including observed playtime accumulated since that death. Corrections are labelled.
3. **Manual dates:** enter a known date in local time (`yyyy-MM-dd HH:mm`). Future
   dates are rejected. Manually supplied dates are labelled, not represented as
   observed events. Historical logged-in time is not invented: it starts at zero
   and is marked partial with `+`.
4. **Logged-in survival time:** choose real-world elapsed time or time actually
   observed logged in. Offline time, loading/hopping interruptions, and long
   stalls do not accrue logged-in time. Best observed playtime is tracked too.
5. **History:** the newest 100 events with dates, observed region/tile coordinates,
   prior streaks, event type, and editable notes up to 500 characters. No killer
   or cause is guessed from combat events.
6. **Backup/restore:** user-selected JSON files with validation and a preview of
   character, last death, count, and history before replacement. A recovery
   snapshot is kept before restoring; use **Recover pre-restore record** to restore
   it. File work runs off the game/UI threads. Backups include names and notes.
7. **Coverage:** last observed online time and the most recent 20 unobserved
   intervals. Ordinary offline time is included in these gaps; a gap does not
   imply a death happened. Deaths on mobile/other clients remain unknown.
8. **Compact display:** label visibility, seconds, text colour, background opacity,
   selected duration units, wall-clock/logged-in basis, and a hover tooltip. The
   in-game box remains a single line. The sidebar includes a counter preview.
9. **Streak card:** manually preview and save a PNG. Character name is hidden by
   default; there is no upload or automatic sharing. The card identifies manual
   dates/corrections and explains observation limits.

## Local RuneLite development build

```sh
./gradlew run
```

The development client loads **Days Since Death (Local Preview)**. It uses the
normal RuneLite profile directory, so existing profiles remain available.
If the published version is also installed, its counter may also be visible;
you can manually disable that version in this development client if desired.
The preview never changes other plugins' settings.

The preview stores character data under `dayssincedeathlocal`, separate from
the published plugin's `dayssincedeath` records. On first use it copies an
existing published timestamp read-only, preserving the original. Existing v1
playtime and coverage are unknown and labelled accordingly. The settings/data
still use RuneLite's standard configuration storage, so RuneLite's own account
sync may apply; “local preview” means unpublished source, not a separate cloud
service or a guarantee that RuneLite will never sync its configuration.

No gameplay automation, additional boss cues, window manipulation, notifications,
or gameplay/network actions have been added. The changes have not been reviewed
or approved for the Plugin Hub. Account safety guidance remains in [AGENTS.md](AGENTS.md).
Use the offline sample preview for evaluation without interacting with a game world.

For a Jagex Account, the development client's Jagex Accounts sidebar handles
login; see [RuneLite's official instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Build and verification

Requires Java 11+, with Java 17 or 21 recommended for development. The macOS
Java desktop module flag is configured for the game launcher.

```sh
./gradlew test jar
```

The local artifact is `build/libs/days-since-death-2.0.0-local.jar`.
Tests use synthetic events and records, with no deliberate live-account deaths.
They cover original duration/streak behaviour, v1 migration, separate character
storage, original-record isolation, logged-in time, undo, manual dates, exclusions,
coverage limits, notes, history bounds, backup validation, and anonymous cards.

To render screenshots of the offline interface without opening a window:

```sh
./gradlew preview --args="--render=/tmp/days-since-death-preview"
```

`docs/ci-workflow.example.yml` remains an optional workflow template; it has not
been activated or uploaded. This local working branch is `codex/local-improvements`.

## License

BSD 2-Clause. The Gradle wrapper comes from the RuneLite example-plugin setup
and Gradle; its embedded notices apply. No third-party audio or images are bundled.
