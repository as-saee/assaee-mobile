# Reader roadmap (Readflow widget)

The original plan for the reader and its home-screen widget, written 2026-10-03. It is the backlog for the reader
module (M5) of [As-Sa'ee](https://github.com/as-saee/assaee-docs); the widget diagnosis below is from code reading and
still to be confirmed on a device.

## Progress
- [ ] **Phase 1: Clean import + a widget you can read a whole book from**
  - [ ] 1a. Clean-text import pipeline (extract, drop headers/footers, de-hyphenate, join paragraphs, split into sentences)
  - [ ] 1b. Import-quality preview (first few cards, warning for pages with no text)
  - [ ] 1c. Whole-book card deck saved locally, sized to the widget ("fit the widget")
  - [ ] 1d. Native widget paging across the whole book, position saved and synced back to the library
  - [ ] 1e. Tap-to-open at the exact card, including when the app is already open in the background
  - [ ] 1f. Device test with the acceptance list below
- [ ] **Phase 2: Make the widget attractive and quick to use**
  - [ ] 2a. Paper, night and pure-black (OLED) themes; system corner radius; serif type
  - [ ] 2b. Small / medium / large layouts; cover, thin progress bar, time-per-card
  - [ ] 2c. Tap zones (left/right = back/forward, centre = open) and long-press to save a card
  - [ ] 2d. Widget picker preview and description
- [ ] **Phase 3: Habit layer**
  - [ ] 3a. Daily goal in cards or minutes, shown on the widget
  - [ ] 3b. Forgiving streak (weekly target, never loses progress)
  - [ ] 3c. "After I ___, I read" plan with at most one reminder a day, backing off if ignored
- [ ] **Phase 4: Remember what you read**
  - [ ] 4a. Saved-cards list; saved cards resurface a few days later
  - [ ] 4b. Measured reading speed, minutes left, finish-date estimate
  - [ ] 4c. Next-book queue
  - [ ] 4d. Optional quiet notification card with Previous/Next (lock-screen substitute, off by default)
  - [ ] 4e. Backup and export of progress and saved cards
- [ ] **Phase 5: Later**
  - [ ] 5a. Read aloud · 5b. Scanned-PDF text recognition · 5c. 5-minute sprint timer · 5d. Optional toggles (dyslexia font, bionic reading, one-word-at-a-time)
  - [ ] 5e. Life planner: reuse the goal/streak/reminder engine for other habits, calendar time for reading, weekly reflection

## The idea
Readflow is a personal app for the author's own PDF books. The goal is a reading habit despite little time and a short
attention span. The home-screen widget is the product: read a book without opening the app. The app later grows into a
life planner, but reading comes first.

Out of scope: the original React web reader, which lives in [readflow-web](https://github.com/as-saee/readflow-web).

## Why this order
Two web research passes (habit science; competing apps and their reviews) shaped the plan. Confidence note: the app
research relied on search results and review roundups, not raw store reviews, so treat it as directional.

- **No mainstream app lets you read a book from a widget.** Existing widgets only show status (streak, cover, progress).
  Readwise's widget shows highlights. So the core idea is open, and it has to be solid before anything else.
- **The widget is only as good as the text.** PDFs store positioned characters, not paragraphs. Columns merge, headers,
  footers and page numbers pollute the text, words are hyphenated at line ends, and scanned pages give no text at all.
  Cleaning once at import, and saving the result, is the only reliable approach. Reflowing at widget time would be slow
  and flaky. Hence Phase 1 starts with import, and every later phase depends on it.
- **Small pieces beat long sessions.** People read about 240 words a minute, so 60–100 words is about 20 seconds: one
  glance. The author chose "fit the widget": each card holds as much text as fits the widget's current size, always ending
  at a full sentence, and re-flows if the widget is resized. Position is stored by place in the book, not by card number,
  so resizing never loses your place.
- **Visible progress toward a near goal drives action** (goal-gradient effect), so the widget shows "cards left today" and
  section progress, not only percent of the book.
- **Habits form slowly and one miss doesn't break them** (about 66 days on average). Broken streaks make people quit, and
  forgiving designs retain more users. So the streak is weekly-target based and never punishes.
- **"After X, I do Y" plans improve follow-through a lot**, hence the reminder in Phase 3. One reminder a day at most.
- **Spaced resurfacing helps memory** (the Readwise ritual), hence Phase 4a.

## Why the current widget fails (diagnosed from code, 2026-10-03; to be confirmed on a device)
1. It receives only one page's chunks, and Next stops at the end of that page (`ReadingWidgetProvider.kt`).
2. Chunks are about 650 characters at 18sp in a non-scrolling box, so text is clipped (`reading_widget.xml`, `lib/main.dart`).
3. "Original page" mode is invisible: the image has zero height (`reading_widget.xml`).
4. Tapping the widget while the app is already running does nothing: the app asks for the pending tap only once, at start.
5. Reading progress made on the widget is never written back to the library.
6. The app and the widget code both write the same saved values with different number types, which is fragile.
7. Plain grey buttons, square corners, no dark mode, no preview.

## How it will be built (technical decisions)
- **Keep XML widget layouts (RemoteViews); do not move to Jetpack Glance.** Glance compiles down to the same limits and
  adds dependencies. Use only allowed views (no ConstraintLayout or custom views, which cause "can't load widget").
- **The widget never shows PDF page images.** There is a hard memory cap on images in a widget update; it is the likely
  reason "original page" mode misbehaves. Widget shows text only. "View original page" opens in the app.
- **Flutter prepares the deck; Kotlin pages through it.** Flutter extracts and cleans the text once, and writes the
  whole-book deck to a file in app storage. Kotlin reads that file and handles Previous/Next itself, so paging is instant
  and never needs to wake Flutter (a headless Flutter start on every tap is slow). Kotlin writes position back for the app.
- **One storage format, written by one side.** Replace the shared `flutter.widget.*` values with a single small state
  file (position, book, theme, goal) plus the deck file. This removes the mismatched-type fragility and avoids the newer
  Flutter storage backend that native code can't read.
- **Reliable buttons:** one explicit, immutable PendingIntent per widget and action with a unique request code; the
  provider calls the base `onReceive`; state changes use partial updates for text. Update only on user action. Never
  animate or refresh on a timer (battery, and a known cause of broken widgets).
- **Responsive layouts:** separate small / medium / large layouts supplied together so the launcher picks one without
  waking the app. Use the system corner radius, theme-aware colours, and declare cell sizes plus resize limits.
- **Import cleaning:** read column by column, drop text that repeats at the same position across pages (headers, footers,
  page numbers), de-hyphenate, join lines into paragraphs, split into sentences. Let the user crop margins. Pages with no
  text are flagged, not silently skipped. Every card keeps a link back to its original page.
- **Local only:** no accounts, no ads, no subscriptions, no sync. Backup/export instead, so progress can't be lost.

## What we deliberately will not do
- Accounts, ads, paywalls, trial traps.
- Streak punishment, lost points, leaderboards, shaming, more than one reminder a day.
- Constant widget refresh or animation.
- Any design where progress can be lost, or where the app needs the internet.
- Social feeds, ratings, recommendations, catalog features. The books already exist; time is the problem.
- Reflow with no way back to the original page.

## Acceptance tests
Phase 1 (on a real phone, using ADB/logcat when something fails):
1. Import two PDFs; both stay in the library, each with a quality preview.
2. A multi-column PDF and a PDF with page headers import without header/footer text or broken words in the cards.
3. Pick a book for the widget: title, text, and progress appear.
4. Next/Previous move through the entire book, across page boundaries, and survive a phone restart.
5. Resize the widget: text re-flows, nothing is clipped, and you are at the same place in the book.
6. Tap the widget with the app closed, then again with the app open in the background: both open the book at the same card.
7. Reading on the widget updates the page shown in the app's library.
8. A scanned PDF shows a clear "no text on these pages" warning instead of a blank widget.

Phase 2: the widget looks right in light, dark and pure-black themes at all three sizes; tap zones and long-press save work.

## Risks and open items
- Device testing is still pending: each phase ends with an installable build for testing on a phone, until a phone is
  connected over USB for live testing.
- Import cleaning is heuristic and will not be perfect on every PDF. The preview and margin cropping are the safety net.
- Scanned PDFs need text recognition (Phase 5b). Until then they are flagged, not read.
