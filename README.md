# EmojiArena

## What this is

A demo built for a take-home task proposing an engagement/retention/sharing feature for VMP Messenger. Sending any single emoji in a chat turns it into a large, tappable bubble. Tapping it launches a physics-based mini-game using that emoji as the ball. Finishing the game posts a score card back into the thread, with an automatic "beat this score" counter-challenge from the contact. A "Challenge others" option fans the same challenge out to multiple contacts at once.

## Why this feature

Facebook Messenger already ships an emoji-triggered mini-game (the basketball emoji) — this isn't a novel invention, it's a proven mechanic that VMP Messenger currently lacks. It maps onto the three things the task asked for:

- **Engagement** — the game loop itself gives people a reason to interact beyond text.
- **Retention** — a "beat this score" challenge pulls the other person back into the app.
- **Sharing** — the "Challenge others" fan-out turns one game session into outreach to several contacts at once.

## How to run it

Clone the repo, open it in Android Studio, run on a device or emulator (API 28+). No backend setup, no config, no environment variables.

## What's real vs. mocked

- **No backend.** All state lives in memory inside a ViewModel and resets on app restart. This is deliberate, not unfinished.
- **Single device, one real user.** The "contacts" are scripted: their replies are canned text sent after an artificial delay to simulate a live conversation.
- **The pre-seeded incoming challenge from Tanvir** exists to demonstrate the "already engaged" state from a cold start, without requiring the user to generate it first.
- **Emoji-message detection** uses a simple heuristic, not full Unicode-correct grapheme handling — acceptable for a demo, not production-grade.

## What production would need to add

- Real push-based delivery instead of scripted replies, so this works with actual second users.
- Server-authoritative scoring — right now a score is entirely client-computed before being shared, so a user could fake a high score; a real version would need the server to validate or generate scores.
- Proper Unicode grapheme-cluster detection for the emoji trigger, to correctly handle skin-tone modifiers, flags, and multi-codepoint emoji.
- Persistence (local database or backend) so state survives app restarts.
