# Project context — read before doing anything

## What we are building

A single-device messaging app demo for a take-home task: propose and
demonstrate one engagement/retention/sharing feature for "VMP Messenger" (a
WhatsApp/Instagram/Facebook-style messaging app).

The proposed feature: sending a specific trigger emoji in a chat renders it
as a large, tappable bubble. Tapping it launches a full-screen mini-game.
Finishing the game posts a score card back into the thread. A scripted
contact reply challenges the user to beat that score, and tapping the
challenge relaunches the game. This mirrors a mechanic Facebook Messenger
already ships (the basketball-emoji game) — the pitch is "proven and
missing from VMP Messenger," not "novel invention."

This is a demo of the mechanic, not a real messaging app. No real backend,
no real second user, no persistence beyond the running session.

## Hard constraints — never violate

**No real backend. No Firebase, no network calls, no database.** All chat
state lives in memory inside a ViewModel. Losing it on app restart is
correct behaviour, not a bug — state this in the README rather than "fixing"
it later.

**One device, one real user.** The "contact" the user chats with is
scripted: their replies are canned text strings, sent after an artificial
delay to feel like a real reply. There is no second live person and no
attempt to simulate one beyond that.

**Build only the section you are given.** Sections are given one at a time
in this order:
1. Home screen — dummy contact list
2. Chat screen shell — send/receive, no scripted replies yet
3. Scripted replies — canned response after a delay
4. Emoji trigger — large tappable bubble, navigates to a stub game screen
5. The game itself — a simple timed tap/score mechanic
6. Score card + challenge loop — send score to thread, scripted counter-challenge, tapping it relaunches the game
7. Polish + README

Do not implement ahead of the current section, but structure code so later
sections slot in without rewriting earlier ones.

**Explain as you go.** After writing each file, give a short plain-English
rundown: what it does, why it exists, what the key lines mean. I need to be
able to explain every line to someone else without notes.

**Ask before restructuring.** If a file should move or a pattern should
change, say so and wait — no silent refactors.

## Additional requirements

- **Message delivery status.** Every message carries a status field
  (`sending → sent → delivered`), advancing through a short artificial
  delay and rendered as ticks — the same "feels real, isn't real" pattern
  as the scripted replies.
- **Shared state, Home and Chat.** The home screen's contact previews and
  the chat screen read off the same shared state source rather than
  independent copies, so posting a score card into a thread updates the
  home screen's preview at the same time — no separate state to
  resynchronise.

## Code organisation rules

Keep files small and single-purpose. No thousand-line files. If a file
passes roughly 150 lines, it's doing too much — split it.

- One responsibility per file. Chat state logic never lives inside a
  composable. The game's logic never lives inside the chat ViewModel.
- Composables are dumb: render state, emit events. No business logic, no
  timers, no scripted-reply logic inside a `@Composable`.
- Per-screen state lives in one ViewModel, exposed as a single `StateFlow`
  of an immutable UI state data class.
- No magic strings or numbers scattered through the code — the trigger
  emoji, delay durations, game duration, all live in one `Constants.kt`.
- Every state (loading, empty, in-progress, finished) has a visible,
  intentional UI — no blank screens, no flash of default values.

### Target package layout

```
com.shadman.emojiarena/
├── MainActivity.kt              // hosts Compose, sets up navigation
├── Constants.kt                 // trigger emoji, delays, game duration, scripted lines
├── data/
│   ├── Contact.kt                  // contact model + hardcoded contact list
│   └── Message.kt                  // message model (text / emoji-trigger / score-card types)
├── chat/
│   ├── ChatViewModel.kt             // per-contact message state, send logic, scripted replies
│   └── ScriptedReplyEngine.kt       // picks/rotates canned reply lines, owns the delay
├── game/
│   ├── GameViewModel.kt             // timer, score, game state machine
│   └── GameScreen.kt                // the full-screen game composable
└── ui/
    ├── HomeScreen.kt                // contact list
    ├── ChatScreen.kt                // message bubble list + input bar
    ├── components/
    │   ├── MessageBubble.kt            // renders text / emoji / score-card bubble types
    │   └── ScoreCard.kt                // the score-card bubble + "beat this score" button
    └── theme/                        // Color.kt, Theme.kt, Type.kt — fixed dark theme, no Material You
```

Not every file exists on day one — create each when its section arrives,
but write code into the file where it will finally live rather than
parking logic in `MainActivity` or a single giant `ChatScreen.kt`.

## Visual bar

Match the quality of my last project (QRPassAuth): a fixed dark theme, not
default Material You dynamic colour. System sans-serif font. Deliberate
spacing and a real accent colour, not stock template defaults. This screen
appears in a demo video — it needs to look like a considered product, not
a tutorial project.

## Environment

- macOS, Apple Silicon
- Android Studio, Jetpack Compose, Kotlin DSL build files
- Physical test device: Xiaomi Mi 11X (M2012K11AI), Android 13 / HyperOS
- Package: `com.shadman.emojiarena`
- minSdk 28
- Toolchain note: AGP 9.0.1. The androidx versions in
  `gradle/libs.versions.toml` are pinned to an older generation
  (coreKtx 1.15.0, lifecycleRuntimeKtx 2.8.7, activityCompose 1.9.3,
  espressoCore 3.6.1, junitVersion 1.2.1) because newer androidx requires
  AGP 9.1.0 and compileSdk 37. **Never upgrade AGP, compileSdk, targetSdk,
  or any androidx version.** If a new dependency conflicts, pin it down to
  an older compatible release instead of raising the project to meet it.

## Out of scope — do not build

- Real backend, real accounts, real second user
- Persistence across app restarts
- Any feature beyond the emoji-trigger-game-challenge loop (no stories, no
  reels, no calling — this demo is scoped to one mechanic)
- Multiplayer/real-time sync of any kind
- Dependency injection frameworks, multi-module Gradle, repository pattern
  — overkill at this size

## Sections (orientation only — wait to be given each one)

1. Home screen — dummy contacts
2. Chat screen shell — send/receive
3. Scripted replies
4. Emoji trigger → stub game screen
5. The game
6. Score card + challenge loop
7. Polish + README
