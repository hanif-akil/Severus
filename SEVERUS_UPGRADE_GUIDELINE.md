# Severus Keyboard — Full Upgrade Guideline
(Kotlin migration retry, Modern Android 16 Settings UI, Top Bar features, Avro Bangla, Long-press Copy/Paste)

---

## PART 0 — Why Your Kotlin Migration Failed (Read This First)

You said you migrated to Kotlin and hit "serious issues" and had to revert. This is a **very common, well-known failure pattern**. Here are the exact issues almost everyone hits, in the order they usually appear:

### Issue 1: Converting everything at once
**Problem:** Trying to convert the whole codebase (IME service, keyboard views, settings, all at once).
**Why it breaks:** Kotlin-Java interop has subtle rules (platform types, nullability, `@JvmStatic`, companion objects). Converting 50+ files simultaneously means 50+ places something can silently break, and you can't isolate which change caused the crash.
**Fix:** Never convert more than 1 file per commit. Build and run the app after every single file conversion.

### Issue 2: `findViewById` and layout inflation returning platform types
**Problem:** Kotlin treats Java return values as "platform types" (`String!`) which bypass null-checking. Code compiles fine, then crashes at runtime exactly like your clipboard bug.
**Why it breaks:** You get zero warning at compile time, so you assume Kotlin "isn't helping," but it's actually Java's un-annotated APIs leaking through.
**Fix:** Add `@Nullable`/`@NonNull` annotations to your Java code BEFORE converting it to Kotlin, so Kotlin knows the real nullability.

### Issue 3: Static fields / singletons (e.g., `ClipboardHistory.getInstance()`)
**Problem:** Java singletons converted to Kotlin `companion object` change bytecode signatures. Any Java file still calling the old static method breaks at compile time across the whole module.
**Why it breaks:** Mixed Java/Kotlin singleton access requires `@JvmStatic` on companion functions or Java call-sites break.
**Fix:** Always add `@JvmStatic` to companion object functions that are called from remaining Java files.

### Issue 4: Custom Views (`ClipboardHistoryView`, `MainKeyboardView`) constructors
**Problem:** Kotlin requires explicit constructor overloads for Views (`context`, `context+attrs`, `context+attrs+defStyleAttr`). Missing one causes `InflateException` crashes only at runtime, only in certain XML usages.
**Fix:** When converting a custom View class, copy ALL Java constructor overloads exactly, using `@JvmOverloads` on the primary constructor.

### Issue 5: Gradle/KAPT vs KSP confusion
**Problem:** Many tutorials mix old `kapt` with new `ksp`, or use `kotlin-android-extensions` (deprecated/removed since Kotlin 1.8+). This alone causes the exact "unclear stack trace, build fails" symptom you saw with Gradle.
**Fix:** Use `ksp` only, never `kapt` for new setups. Don't use `kotlin-android-extensions` — use `findViewById` or ViewBinding instead.

### Issue 6: Reverting halfway and leaving mixed artifacts
**Problem:** When you "deleted everything and restarted to Java," if any `.kt` file, empty Kotlin source folder, or leftover `kotlin-android` plugin line stayed in `build.gradle`, Gradle gets confused about which compiler to invoke.
**Fix:** Before reverting, run `git clean -xdf` and re-clone fresh from a known-good Java commit rather than manually deleting files.

### Recommended path forward for migration (if you try again later):
1. Do NOT migrate now. Ship the Java features below first (stability first, as you requested).
2. When you do migrate later: one file at a time, `@JvmStatic` on all companions, `@JvmOverloads` on all View constructors, `ksp` not `kapt`, commit + build + run after every file.
5. Treat migration as a 3-6 month background task, never a weekend project.

**For now: stay on Java.** Everything below is written to work in Java, matching your stated priorities (fast, stable, secure).

---

## PART 1 — Modern "Android 16" Style Settings UI

Android's current design language (Material 3 Expressive) uses: large rounded corner cards, bigger title typography, edge-to-edge content, dynamic color (Material You), grouped preference cards instead of flat lists.

### Step 1.1 — Update dependencies
- In `app/build.gradle`, update `androidx.preference:preference` to the latest 1.2.x version, and add `com.google.android.material:material` latest 1.12.x version.
- Enable `android:enableOnBackInvokedCallback="true"` in the manifest `<application>` tag (predictive back gesture, required for "modern" feel).

### Step 1.2 — Apply Material 3 theme
- Change your app theme parent (in `themes.xml`) from the old AppCompat/Material2 parent to a `Theme.Material3.DynamicColors.*` variant.
- Add `<item name="android:windowLightNavigationBar">` and edge-to-edge flags so the settings screen draws behind the status/nav bars like modern Android apps.

### Step 1.3 — Restyle the Settings screen from flat list → grouped cards
- Your current `prefs.xml` uses plain `PreferenceScreen` entries (flat list, old style).
- Wrap each `PreferenceScreen` group ("Preferences", "Key Press", "Appearance") inside a `PreferenceCategory` with `app:iconSpaceReserved="false"` and give each an icon — this alone visually turns it from "Android 8 style list" to "Android 16 style grouped card list."
- Increase list item height and add rounded card backgrounds via a custom `Preference` style (`android:layout` pointing to a custom XML with a `MaterialCardView` wrapper).

### Step 1.4 — Top App Bar redesign
- Replace the existing basic `Toolbar`/ActionBar in `SettingsActivity` with a `MaterialToolbar` using the "Large Top App Bar" / "Medium Top App Bar" style from Material 3 (the big-title-that-shrinks-on-scroll look used across Android 16 system apps).
- Make the toolbar collapse on scroll using `CollapsingToolbarLayout` + `AppBarLayout` if you want the full modern effect, or keep it fixed-height if you prefer simplicity/speed (recommended, since you prioritized speed — collapsing toolbars cost a small layout performance overhead).

---

## PART 2 — Top Bar Icons (Clipboard, Settings Gear, Numberpad Toggle)

This is about the **keyboard's own top bar** (the strip above the keys while typing), not the Settings app screen.

### Step 2.1 — Locate the existing top bar
- Find `res/layout/suggestion_strip.xml` or the toolbar layout referenced by `MainKeyboardView`/`KeyboardSwitcher`. This is the row that currently would hold suggestions; since you don't want text prediction, this strip is currently empty/unused space — perfect place for icons.

### Step 2.2 — Add three icon buttons to that strip
- Add three `ImageButton`s inside that row layout: clipboard icon, gear/settings icon, and a "123" numberpad-toggle icon, evenly spaced or right-aligned.
- Style them as flat/borderless icon buttons with a ripple effect (`?attr/selectableItemBackgroundBorderless`) to match Material 3 icon button look.

### Step 2.3 — Wire up the clipboard icon
- On click, call the existing `showClipboardHistoryPanel()` method you already have in `MainKeyboardView` (the one from your crash-fix earlier). This reuses code you already fixed.

### Step 2.4 — Wire up the settings gear icon
- On click, launch `SettingsActivity` via an `Intent` from the IME service context (`getContext().startActivity(intent)` with `FLAG_ACTIVITY_NEW_TASK` since it's launched from a service, not an activity).

### Step 2.5 — Wire up the numberpad toggle icon
- This should call the same code path your existing "switch to symbols/number layout" key already uses (`Keyboard.CODE_SWITCH_ALPHA_SYMBOL` or whatever your `KeyboardSwitcher` currently uses for the "?123" key). You're just duplicating that action onto a persistent top-bar icon so the user doesn't need to leave the alphabet layout to reach it.
- Make the icon toggle state (show "ABC" icon when in number mode, "123" icon when in alphabet mode) by listening to the same keyboard-mode-changed callback the switcher already fires.

---

## PART 3 — No Text Prediction, But Maximum Security, Speed, Stability

You explicitly do NOT want prediction. Good — that also directly helps security and speed, since prediction requires a dictionary lookup, personalization data storage, and (in many keyboards) is the #1 privacy leak vector.

### Step 3.1 — Fully disable and remove prediction code paths
- In Settings, remove/hide any "Show suggestions," "Auto-correct," "Personalized suggestions," or "Next word suggestions" preference toggles from `prefs.xml` and the corresponding fragment — don't just turn them off by default, remove the UI entirely so there's no code path that can silently re-enable them.
- In `LatinIME.java` (or wherever `SuggestedWords`/`DictionaryFacilitator` is initialized), skip loading the dictionary facilitator entirely if you're removing prediction — this reduces both APK size (no dictionary binaries needed) and startup time (no dictionary load = faster IME cold-start).

### Step 3.2 — Security hardening checklist
- Ensure `android:exported="false"` is set on the IME service in the manifest (should already be true from your earlier fix) — this is the single most important security setting for a keyboard.
- Add `android:allowBackup="false"` for the IME service data specifically if backups aren't needed for keyboard state — this prevents clipboard/typed-data leakage through Android's auto-backup to cloud.
- Ensure no analytics, crash reporting SDK, or ad SDK exists in `build.gradle` dependencies — audit the dependency list once and document it in your README as a security commitment (FOSS users specifically look for this).
- Add a `network_security_config.xml` that blocks all cleartext traffic and disallows any network permission entirely (a keyboard should have zero internet permission — verify `INTERNET` permission is NOT in your manifest at all).

### Step 3.3 — Speed and stability checklist
- Make sure the clipboard panel fix from earlier (measure-before-attach, attach-before-populate) is applied — this was your main stability bug.
- Avoid `Handler.postDelayed` chains for animations on the top bar icons; use simple state-based visibility (`View.GONE`/`View.VISIBLE`) instead of animated transitions if you want maximum responsiveness on low-end devices.
- Enable R8 full mode (`android.enableR8.fullMode=true` in `gradle.properties`) and enable `minifyEnabled true` + `shrinkResources true` in the release build type — smaller APK loads faster and starts faster, especially relevant since you removed the dictionary/prediction weight already.
- Avoid any `Thread.sleep` or synchronous I/O in the IME's `onCreate`/`onStartInputView` lifecycle methods — all persistence (like clipboard history) should be read on a background thread and posted back, never blocking the main thread during keyboard show.

---

## PART 4 — Avro Phonetic Layout for Bangla

Avro Phonetic is a transliteration scheme (type "ami" → get "আমি"), fundamentally different from your existing Bengali/Unijoy/Akkhor layouts, which are direct key-to-glyph mappings. This requires actual transliteration logic, not just a new XML layout.

### Step 4.1 — Understand the difference from existing Bengali layouts
- Your existing `kbd_bengali.xml`, `kbd_bengali_unijoy.xml`, `kbd_bengali_akkhor.xml` are all **static key maps** — one physical key press = one Bengali glyph. This is straightforward XML work you already have examples of.
- Avro Phonetic is a **rule-based conversion engine** — you type Roman/English letters and a parser converts sequences like "kh," "a," "500+ pattern rules" into correct Bengali conjuncts. This is NOT expressible as a static XML key map.

### Step 4.2 — Two implementation approaches (pick one)

**Approach A (Recommended for stability/speed priority): Port the Avro phonetic rule engine as a lightweight parsing module**
- The open-source Avro Phonetic transliteration rules are publicly documented (used by OmicronLab's Avro Keyboard and many open-source clones like `ibus-avro`, `Ridmik`, and various phonetic JS/Python libraries). You implement a Java class `AvroPhoneticConverter` that takes the raw Latin-character buffer the user is typing and converts it to Bengali Unicode using the same rule table, then feed the converted text into the existing input-connection commit path.
- This keeps everything in Java (matches your no-Kotlin decision), keeps it fully offline (no network/security concern), and reuses your existing English QWERTY key layout as the input layer — the "layout" itself doesn't change, only what happens to the composed text before it's committed.

**Approach B (Simpler but more limited): Static approximate key map**
- Build a `kbd_bengali_avro.xml` similar to your existing Bengali XMLs, but this only lets you approximate common Avro key positions — it will NOT give correct phonetic transliteration behavior (e.g., typing "kk" for a specific conjunct) since that logic requires the rule engine, not a key map. Only choose this if you specifically want "Avro-like key positions" rather than "true Avro phonetic behavior."

**Given your emphasis on correctness for daily Bangla typing, Approach A is what actual Avro users expect — go with A.**

### Step 4.3 — Where to hook the converter in
- Intercept text in the same place your existing input logic commits characters to the input connection (`LatinIME.onCodeInput` or equivalent) — buffer Latin characters, run them through `AvroPhoneticConverter`, and commit the Bengali result, similar to how existing IMEs handle composing text with `setComposingText()` + `finishComposingText()`.
- Add an entry to `settings/languages` (wherever Bengali/Unijoy/Akkhor are currently listed as selectable layouts) for "Bengali (Avro Phonetic)" so users can pick it exactly like the other Bengali layouts.

### Step 4.4 — Settings placement
- Add "Bengali - Avro Phonetic" as a new layout choice inside your existing language/layout picker (same list as Bengali, Bengali Unijoy, Bengali Akkhor) — no new settings screen needed, just a new entry in the same list, consistent with the existing 3 Bengali options already in the codebase.

---

## PART 5 — Long-Press C/V/X for Copy/Paste/Cut (Toggleable in Settings)

### Step 5.1 — Identify the C, V, X keys in the layout
- These are standard QWERTY keys already defined in your `rowkeys_qwerty*.xml` files. You need to add a long-press action to specifically these three keys, not a general "all keys get long press" feature.

### Step 5.2 — Add long-press codes to the key XML
- In the row-key XML definitions for C, V, and X specifically, add a `longPressCode` (or your codebase's equivalent attribute — check how other keys like period/comma already define long-press popups, since Simple Keyboard-based projects usually support this attribute already for accented characters) pointing to special internal codes, e.g., `CODE_COPY`, `CODE_PASTE`, `CODE_CUT` (define these as new constants alongside existing special codes like `CODE_SHIFT`, `CODE_DELETE`).

### Step 5.3 — Handle the new codes in the input processing logic
- In the same place your keyboard currently handles special codes like delete/shift/enter (likely in `PointerTracker` or `LatinIME.onCodeInput`), add three new cases: `CODE_COPY` → call `InputConnection.performContextMenuAction(android.R.id.copy)`, `CODE_PASTE` → `android.R.id.paste`, `CODE_CUT` → `android.R.id.cut`. These are standard Android InputConnection actions and work regardless of what app the keyboard is currently typing into.

### Step 5.4 — Make it toggleable from Settings
- Add one new `SwitchPreferenceCompat` to your `KeyPressSettingsFragment` (the existing settings screen for key-press-related behavior) labeled something like "Long-press C/V/X for Copy/Paste/Cut," backed by a new boolean preference key (e.g., `pref_key_long_press_cvx`).
- In the long-press handling code from Step 5.3, check this preference's value before executing the copy/paste/cut action — if disabled, fall through to the normal long-press behavior for that key (or no action at all) instead of triggering the clipboard action. Read the preference value once when the keyboard loads (not on every keypress) and cache it, refreshing the cached value only when settings change, to avoid any per-keystroke performance cost (keeps it fast, per your priority).

### Step 5.5 — Default state
- Default this to **off**, since silently intercepting long-press on C/V/X could surprise users who expect accented-character popups (if your layout has any) or simply expect normal key repeat behavior. Let users opt in from Settings.

---

## Suggested Order of Implementation (Priority Order Matching Your Stated Priorities: Stability → Security → Speed → Features)

1. **Part 3** (remove prediction, security hardening, speed checklist) — do this first since it's foundational and touches the fewest files.
2. **Part 2** (top bar icons: clipboard/settings/numberpad) — builds on your already-fixed clipboard code, moderate complexity.
3. **Part 5** (long-press C/V/X toggle) — small, isolated, easy to test in isolation.
4. **Part 1** (modern Settings UI restyle) — purely visual, no risk to core typing stability, do after core features are locked in.
5. **Part 4** (Avro phonetic) — most complex (needs a real rule engine), do last so it doesn't block shipping the other four improvements.

---

## Final Notes

- Every part above is designed to work fully in **Java** — no Kotlin required, consistent with your decision to stay on Java after the migration issues.
- Test after each Part independently (build + run on device) rather than implementing all 5 parts before testing anything — this avoids the exact "too many changes at once, can't isolate the crash" problem that caused your Kotlin migration to fail.
- None of these five features require adding internet/network permissions, external libraries with telemetry, or any cloud dependency — this keeps your security and "no data leaves the device" stance fully intact throughout.
