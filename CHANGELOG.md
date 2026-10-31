# Changelog

## 2.0.0

- Add real-world and observed logged-in streaks, recent death history with notes,
  coverage gaps, manual dates, and undo with two confirmation prompts.
- Add optional exclusions for dedicated LMS and PvP Arena worlds; other deaths
  still count, and excluded events remain in history.
- Add validated JSON backup/restore, a pre-restore recovery snapshot, and manually
  saved PNG streak cards with character names hidden by default.
- Add full/LD/clock/clock+LD/no-label styles, colour and opacity controls, tooltips,
  and immediate display toggles. Hidden labels leave no reserved width.
- Show seconds at all Automatic-format durations when enabled.
- Migrate existing published v1 records without overwriting the original value.
  Experimental preview records remain separate and can be restored explicitly.
- Include an offline sample UI for exploring controls without an account.

## 1.0.0

- Initial compact Last Death counter with per-character timestamps, elapsed-time
  formats, longest recorded streaks, and honest first-use/observation limits.
