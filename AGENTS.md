# RuneLite development: account safety comes first

These instructions apply to every task in this repository, including ideas,
implementation, local testing, and publication. The user prioritizes avoiding
account penalties over adding features. Never promise a plugin is ban-proof.

## Check rules before building or recommending live tests

- Read current official sources before implementing a new gameplay feature:
  - https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1
  - https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features
  - https://github.com/runelite/plugin-hub#readme
- Describe the exact trigger, data used, output, and any game actions; evaluate
  the whole behavior, rather than assuming cosmetic intent makes it permissible.
- If compliance is unclear, explain the specific uncertainty and seek maintainer
  clarification before implementing the disputed behavior or recommending live
  account testing. Continue independent work that does not depend on it.
- Local/developer mode, a toggle, user input, passing tests, or using RuneLite
  does not by itself make a feature compliant. Existing plugins are not blanket
  precedents, and acceptance is not guaranteed.

## Do not implement prohibited or evasive behavior

- No automatic game inputs/actions, anti-idle behavior, logout prevention,
  input forwarding, or attempts to avoid detection or enforcement.
- No extra boss-mechanic cues, attack predictions/counters, prayer instructions,
  automatic safe-position guidance, or prohibited PvP assistance.
- Do not change click areas or menus in ways prohibited by current rules.
- Do not automatically focus, minimize, or otherwise manipulate client windows.
- Do not insert/modify outgoing chat, alter other plugins' settings, collect
  player scouting data, or store login credentials.
- Use permitted Java/RuneLite APIs; check current restrictions on file/network
  access, audio, reflection, native code, subprocesses, and dependencies.

## Known rejected feature in the prior project

- Automatic FAHHHH playback on Graardor's slam was rejected as an additional
  boss-mechanic audio indicator: https://github.com/runelite/plugin-hub/pull/19632
- That project's code is historical, not an approved example. Do not recommend using
  the automatic effect on a live account, resubmit it unchanged, or restore it
  under another name. A manual alternative still requires its own review.

## Productivity features

- Prefer user-entered tasks, notes, manual timers, and session summaries.
- Keep gameplay controlled by the player. Do not capture unrelated application
  contents or keystrokes, launch work actions, or transmit work notes without
  explicit authorization. Use fake events and offline tests where possible.
