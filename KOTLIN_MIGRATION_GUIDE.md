# Severus Keyboard — Complete Java → Kotlin Migration Guide

## Overview

- **Total files:** 93 Java files
- **Total lines:** ~22,600
- **Strategy:** Bottom-up, leaf-first. Convert files with zero dependents first, work upward to `LatinIME` last.
- **Rule:** Rename one file at a time. Build + run on device after EVERY file. If it breaks, revert that single file.

---

## Phase 0 — Prerequisites (Do this ONCE before any file conversion)

### Step 0.1 — Add Kotlin plugin to build.gradle (project-level)

```groovy
// build.gradle (project root)
buildscript {
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21'
    }
}
```

### Step 0.2 — Add Kotlin plugin to app/build.gradle

```groovy
// app/build.gradle
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

android {
    // ... existing config stays the same
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.13.1'
}
```

### Step 0.3 — Update proguard-rules.pro

After ALL files are converted, replace the `-keep` rules with:

```proguard
# Keep IME service
-keep class rkr.simplekeyboard.inputmethod.latin.LatinIME { *; }

# Keep settings fragments (referenced from XML)
-keep class rkr.simplekeyboard.inputmethod.latin.settings.*Fragment { *; }

# Keep classes referenced from AndroidManifest.xml
-keep class rkr.simplekeyboard.inputmethod.latin.settings.SettingsActivity { *; }
-keep class rkr.simplekeyboard.inputmethod.latin.SystemBroadcastReceiver { *; }

# Keep R class
-keep class rkr.simplekeyboard.inputmethod.R { *; }
-keep class rkr.simplekeyboard.inputmethod.R$* { *; }
```

### Step 0.4 — Create src/main/kotlin directory

```
app/src/main/kotlin/rkr/simplekeyboard/inputmethod/
```

Place all `.kt` files here. Android Studio treats `java/` and `kotlin/` as equivalent source sets.

---

## Phase 1 — Pure Data Classes & Constants (Zero Dependencies)

**Why first:** These have no Android framework dependencies, no singletons, no inheritance. They convert in 2 minutes each. This builds confidence and establishes the Kotlin pattern.

### Wave 1.1 — Constants & Enums (4 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `latin/common/Constants.java` | 155 | Convert `static final` → `const val` or `object`. Inner classes → `object`. `isLetterCode()` → top-level function or companion. |
| `latin/common/CollectionUtils.java` | small | Likely deletable — use Kotlin stdlib (`listOf`, `mutableListOf`). |
| `latin/common/CoordinateUtils.java` | small | Convert to `object` with `@JvmStatic` if called from Java during partial migration. |
| `latin/define/DebugFlags.java` | small | Convert to `object`. |

### Wave 1.2 — Data-heavy classes (6 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `latin/Subtype.java` | 166 | → `data class Subtype(...)`. Equals/hashCode auto-generated. |
| `latin/settings/SettingsValues.java` | ~100 | → `data class SettingsValues(...)`. All `public final` fields → constructor params. |
| `latin/settings/SpacingAndPunctuations.java` | small | → `data class`. |
| `keyboard/KeyboardId.java` | ~300 | → `data class`. Complex `equals`/`hashCode` in current code gets auto-generated. |
| `keyboard/Key.java` | ~200 | → `data class` or regular class depending on mutability. |
| `latin/InputAttributes.java` | small | → `data class`. |

### Wave 1.3 — String/Utility classes (deletable or trivial)

| File | Lines | Action |
|------|-------|--------|
| `latin/common/StringUtils.java` | small | Most methods use Kotlin stdlib equivalents. Convert only what's still needed. |
| `latin/common/LocaleUtils.java` | small | Convert to `object`. |
| `latin/utils/RecapitalizeStatus.java` | small | → `enum class RecapitalizeStatus`. |
| `latin/utils/InputTypeUtils.java` | small | → `object` with `@JvmStatic`. |

**Build + test after each file. Commit individually.**

---

## Phase 2 — Keyboard Internal Engine (Core rendering & touch)

**Why second:** These are the keyboard's rendering backbone. They depend on Phase 1 classes and each other, but NOT on `LatinIME` or settings.

### Wave 2.1 — Drawing & Preview (6 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `keyboard/internal/DrawingProxy.java` | interface | → `interface DrawingProxy` (trivial) |
| `keyboard/internal/DrawingPreviewPlacerView.java` | small | View subclass. Keep 3 constructors. |
| `keyboard/internal/KeyDrawParams.java` | small | Data class. |
| `keyboard/internal/KeyPreviewDrawParams.java` | small | Data class. |
| `keyboard/internal/KeyPreviewView.java` | small | View subclass. |
| `keyboard/internal/KeyPreviewChoreographer.java` | ~200 | Convert singleton to `object`. |

### Wave 2.2 — Key Specs & Parsing (6 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `keyboard/internal/KeySpecParser.java` | small | → `object`. |
| `keyboard/internal/KeyStyle.java` | interface | → `interface`. |
| `keyboard/internal/KeyStylesSet.java` | small | Regular class. |
| `keyboard/internal/MoreKeySpec.java` | small | Data class. |
| `keyboard/internal/KeyboardCodesSet.java` | small | → `object`. |
| `keyboard/internal/KeyboardIconsSet.java` | small | → `object`. |

### Wave 2.3 — Keyboard State Machine (4 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `keyboard/internal/AlphabetShiftState.java` | small | → `enum class`. |
| `keyboard/internal/ShiftKeyState.java` | small | Regular class. |
| `keyboard/internal/ModifierKeyState.java` | small | Regular class. |
| `keyboard/internal/KeyboardState.java` | ~400 | Complex state machine. Use Kotlin sealed class for states. |

### Wave 2.4 — Keyboard Builder & Layout (6 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `keyboard/internal/KeyboardParams.java` | small | Data class. |
| `keyboard/internal/KeyboardRow.java` | small | Regular class. |
| `keyboard/internal/KeyboardTextsTable.java` | ~500 | Large array constants → Kotlin arrays. |
| `keyboard/internal/KeyboardTextsSet.java` | small | Regular class. |
| `keyboard/internal/KeyboardBuilder.java` | ~1000 | **Biggest file.** Generic builder pattern. Convert carefully, keep `@JvmStatic` on factory methods. |
| `keyboard/internal/UniqueKeysCache.java` | small | → `object` singleton. |

### Wave 2.5 — Keyboard Models & Views (12 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `keyboard/Keyboard.java` | ~200 | Regular class with inner `Case` data class. |
| `keyboard/ProximityInfo.java` | small | → `data class`. |
| `keyboard/KeyDetector.java` | small | Regular class. |
| `keyboard/MoreKeysDetector.java` | small | Regular class. |
| `keyboard/MoreKeysPanel.java` | interface | → `interface`. |
| `keyboard/MoreKeysKeyboard.java` | ~200 | View subclass. |
| `keyboard/MoreKeysKeyboardView.java` | ~150 | View subclass. |
| `keyboard/KeyboardActionListener.java` | interface | → `interface`. |
| `keyboard/KeyboardView.java` | ~523 | **View subclass.** Keep all constructors with `@JvmOverloads`. Careful with `onDraw`/`onMeasure`. |
| `keyboard/MainKeyboardView.java` | ~600 | View subclass. Same as above. |
| `keyboard/PointerTracker.java` | ~945 | **Complex.** Singleton pattern. Convert to `object` or companion. Touch handling is critical. |
| `keyboard/KeyboardSwitcher.java` | ~411 | Singleton. Convert to `object`. |

**Build + test after each file. This phase is the riskiest — test keyboard rendering and touch after every 2-3 files.**

---

## Phase 3 — IME Service Layer (LatinIME + connections)

**Why third:** Depends on everything in Phase 1 and 2. These are the "glue" files.

### Wave 3.1 — Input Logic & Events (5 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `event/Event.java` | ~150 | → `data class` or sealed class for event types. |
| `event/InputTransaction.java` | small | → `data class`. |
| `latin/inputlogic/InputLogic.java` | ~600 | Regular class. No singleton. |
| `latin/AvroPhoneticConverter.java` | ~277 | → `object`. All methods are `static`. |
| `latin/RichInputConnection.java` | ~535 | Regular class. Replace `ExecutorService` with coroutines. |

### Wave 3.2 — IME Service Infrastructure (6 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `latin/ClipboardStore.java` | ~91 | Regular class. Replace SharedPreferences access with Kotlin property delegates. |
| `latin/AudioAndHapticFeedbackManager.java` | ~158 | Singleton → `object`. Replace `ExecutorService` with coroutines. |
| `latin/RichInputMethodManager.java` | ~724 | **Complex singleton.** Inner `SubtypeList` class → nested class. Convert to `object`. |
| `latin/InputAttributes.java` | small | Data class. |
| `latin/SystemBroadcastReceiver.java` | small | Regular class. |
| `latin/utils/LeakGuardHandlerWrapper.java` | small | Generic class. |

### Wave 3.3 — The Main Service (1 file)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `latin/LatinIME.java` | ~984 | **LAST to convert.** This is the root of the dependency tree. Convert after ALL other files are Kotlin. `InputMethodService` subclass. Replace `Handler` with coroutines where possible. |

---

## Phase 4 — Settings (UI layer)

**Why last:** Settings fragments use deprecated `PreferenceActivity`/`PreferenceFragment`. This is a good opportunity to migrate to Jetpack Compose or at least `PreferenceFragmentCompat`.

### Wave 4.1 — Compat layer (3 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `compat/PreferenceManagerCompat.java` | ~31 | → `object`. Simple wrapper. |
| `compat/MenuItemIconColorCompat.java` | small | → `object`. |
| `compat/EditorInfoCompatUtils.java` | small | → `object`. |

### Wave 4.2 — Settings Core (4 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `latin/settings/Settings.java` | ~351 | Singleton → `object`. Static methods → `@JvmStatic` or top-level. SharedPreferences access → Kotlin property delegates. |
| `latin/settings/SettingsActivity.java` | ~158 | `PreferenceActivity` subclass. Keep as-is during migration, modernize later. |
| `latin/settings/InputMethodSettingsFragment.java` | ~50 | Fragment subclass. |
| `latin/settings/InputMethodSettingsImpl.java` | ~87 | Regular class. |

### Wave 4.3 — Settings Fragments (12 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `latin/settings/SubScreenFragment.java` | ~123 | Base fragment. Convert first. |
| `latin/settings/SettingsFragment.java` | ~70 | Extends `InputMethodSettingsFragment`. |
| `latin/settings/PreferencesSettingsFragment.java` | ~59 | Extends `SubScreenFragment`. |
| `latin/settings/KeyPressSettingsFragment.java` | ~155 | Extends `SubScreenFragment`. |
| `latin/settings/AppearanceSettingsFragment.java` | small | Extends `SubScreenFragment`. |
| `latin/settings/ThemeSettingsFragment.java` | small | Extends `SubScreenFragment`. |
| `latin/settings/LanguagesSettingsFragment.java` | small | Extends `SubScreenFragment`. |
| `latin/settings/SingleLanguageSettingsFragment.java` | small | Extends `SubScreenFragment`. |
| `latin/settings/SeekBarDialogPreference.java` | small | `DialogPreference` subclass. |
| `latin/settings/RadioButtonPreference.java` | small | `RadioButton` subclass. |
| `latin/settings/ColorDialogPreference.java` | small | `DialogPreference` subclass. |

### Wave 4.4 — Settings Utilities (10 files)

| File | Lines | Conversion Notes |
|------|-------|-----------------|
| `latin/utils/SubtypeLocaleUtils.java` | ~671 | **Large.** → `object`. Switch statements → Kotlin `when`. |
| `latin/utils/SubtypePreferenceUtils.java` | small | → `object`. |
| `latin/utils/LanguageOnSpacebarUtils.java` | small | → `object`. |
| `latin/utils/LocaleResourceUtils.java` | small | → `object`. |
| `latin/utils/TypefaceUtils.java` | small | → `object`. |
| `latin/utils/ResourceUtils.java` | small | → `object`. |
| `latin/utils/ViewLayoutUtils.java` | ~94 | → `object`. |
| `latin/utils/XmlParseUtils.java` | small | → `object`. |
| `latin/utils/DialogUtils.java` | small | → `object`. |
| `latin/utils/FragmentUtils.java` | ~46 | → `object`. |
| `latin/utils/ApplicationUtils.java` | ~84 | → `object`. |
| `latin/utils/CapsModeUtils.java` | small | → `object`. |

---

## Conversion Patterns Reference

### Pattern 1: Java Singleton → Kotlin `object`

```java
// BEFORE (Java)
public class KeyboardSwitcher {
    private static final KeyboardSwitcher sInstance = new KeyboardSwitcher();
    public static KeyboardSwitcher getInstance() { return sInstance; }
    private KeyboardSwitcher() {}
}
```

```kotlin
// AFTER (Kotlin)
object KeyboardSwitcher {
    // All methods become direct members
}
```

### Pattern 2: Java Static Methods → `companion object`

```java
// BEFORE
public class Settings {
    public static boolean readShowToolbar(SharedPreferences prefs) {
        return prefs.getBoolean("pref_show_toolbar", true);
    }
}
```

```kotlin
// AFTER
class Settings {
    companion object {
        @JvmStatic
        fun readShowToolbar(prefs: SharedPreferences): Boolean =
            prefs.getBoolean("pref_show_toolbar", true)
    }
}
```

**IMPORTANT:** Add `@JvmStatic` on EVERY companion method called from unconverted Java files during partial migration.

### Pattern 3: Java View Class → Kotlin with `@JvmOverloads`

```java
// BEFORE
public class InputView extends FrameLayout {
    public InputView(Context context) { super(context); }
    public InputView(Context context, AttributeSet attrs) { super(context, attrs, 0); }
    public InputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
```

```kotlin
// AFTER
class InputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    // ...
}
```

### Pattern 4: Java Data Holder → Kotlin `data class`

```java
// BEFORE
public final class SettingsValues {
    public final boolean mShowToolbar;
    public final boolean mLongPressCvxEnabled;
    // ... 15 more fields
    public SettingsValues(SharedPreferences prefs, Resources res, InputAttributes attr) {
        mShowToolbar = Settings.readShowToolbar(prefs);
        mLongPressCvxEnabled = Settings.readLongPressCvxEnabled(prefs);
        // ...
    }
}
```

```kotlin
// AFTER
data class SettingsValues(
    val showToolbar: Boolean,
    val longPressCvxEnabled: Boolean,
    // ...
) {
    companion object {
        fun create(prefs: SharedPreferences, res: Resources, attr: InputAttributes): SettingsValues {
            return SettingsValues(
                showToolbar = Settings.readShowToolbar(prefs),
                longPressCvxEnabled = Settings.readLongPressCvxEnabled(prefs),
                // ...
            )
        }
    }
}
```

### Pattern 5: Java Interface → Kotlin Interface

```java
// BEFORE
public interface KeyboardActionListener {
    void onPressKey(int primaryCode, int repeatCount, boolean isSinglePointer);
    void onReleaseKey(int primaryCode, boolean withSliding);
    // ...
}
```

```kotlin
// AFTER
interface KeyboardActionListener {
    fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean)
    fun onReleaseKey(primaryCode: Int, withSliding: Boolean)
}
```

### Pattern 6: Java Enum → Kotlin `enum class`

```kotlin
enum class RecapitalizeStatus(val mode: Int) {
    OFF(0),
    ON_START_OF_SENTENCE(1),
    ON_WORD_BOUNDARY(2),
    IN_WORD(3);
}
```

### Pattern 7: ExecutorService → Kotlin Coroutines

```java
// BEFORE
private final ExecutorService mBackgroundThread = Executors.newSingleThreadExecutor();
mBackgroundThread.execute(() -> {
    // background work
});
```

```kotlin
// AFTER
private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

scope.launch {
    // background work
}
```

---

## Critical `@JvmStatic` / `@JvmOverloads` Checklist

During partial migration (mixed Java + Kotlin), these annotations are MANDATORY:

| Annotation | When to use | Why |
|-----------|-------------|-----|
| `@JvmStatic` | Every method in `companion object` called from Java | Java calls `ClassName.method()`, Kotlin wraps in `Companion.method()` |
| `@JvmOverloads` | Every View constructor | Android XML inflation needs all constructor overloads |
| `@JvmField` | Public fields in data-like classes | Java accesses `obj.field`, Kotlin generates `getField()` |
| `@JvmSynthetic` | To hide Kotlin-specific helpers from Java | Prevents IDE confusion |
| `@JvmName("methodName")` | When Kotlin default name differs | E.g., property getter vs explicit method |

---

## Testing Protocol

After EACH file conversion:

1. **Build:** `./gradlew assembleDebug` must succeed with zero errors
2. **Install:** `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. **Test checklist:**
   - [ ] Open Settings app → no crash
   - [ ] Tap a text field → keyboard appears at bottom
   - [ ] Type each letter (a-z) → characters appear
   - [ ] Press spacebar → space inserted
   - [ ] Press backspace → character deleted
   - [ ] Long-press a key → popup appears (if applicable)
   - [ ] Tap clipboard button → popup shows
   - [ ] Tap settings button → settings opens
   - [ ] Tap numpad button → switches to number layout
   - [ ] Switch language → keyboard reloads
   - [ ] Rotate screen → no crash
   - [ ] Home button → keyboard dismisses

If ANY test fails, revert the last file conversion and investigate.

---

## Recommended Timeline

| Week | Files | Focus |
|------|-------|-------|
| Week 1 | Phase 0 + Phase 1 (Wave 1.1-1.3) | Build setup + 14 data/constant files |
| Week 2 | Phase 2 (Wave 2.1-2.3) | Keyboard internals + rendering |
| Week 3 | Phase 2 (Wave 2.4-2.5) | Keyboard builder + views (hardest week) |
| Week 4 | Phase 3 (Wave 3.1-3.3) | IME service + LatinIME |
| Week 5 | Phase 4 (Wave 4.1-4.4) | Settings fragments + utilities |
| Week 6 | Cleanup | Remove `@JvmStatic` from non-Java callers, update ProGuard, final testing |

---

## File-to-File Conversion Order (Complete List)

```
1.  latin/common/Constants.java              → Constants.kt
2.  latin/common/CollectionUtils.java        → DELETE (use Kotlin stdlib)
3.  latin/common/CoordinateUtils.java        → CoordinateUtils.kt
4.  latin/define/DebugFlags.java              → DebugFlags.kt
5.  latin/common/StringUtils.java            → StringUtils.kt (or delete)
6.  latin/common/LocaleUtils.java            → LocaleUtils.kt
7.  latin/utils/RecapitalizeStatus.java      → RecapitalizeStatus.kt
8.  latin/utils/InputTypeUtils.java          → InputTypeUtils.kt
9.  latin/Subtype.java                       → Subtype.kt
10. latin/settings/SettingsValues.java       → SettingsValues.kt
11. latin/settings/SpacingAndPunctuations.java → SpacingAndPunctuations.kt
12. keyboard/KeyboardId.java                 → KeyboardId.kt
13. keyboard/Key.java                        → Key.kt
14. latin/InputAttributes.java               → InputAttributes.kt
15. keyboard/internal/DrawingProxy.java      → DrawingProxy.kt
16. keyboard/internal/DrawingPreviewPlacerView.java → DrawingPreviewPlacerView.kt
17. keyboard/internal/KeyDrawParams.java     → KeyDrawParams.kt
18. keyboard/internal/KeyPreviewDrawParams.java → KeyPreviewDrawParams.kt
19. keyboard/internal/KeyPreviewView.java    → KeyPreviewView.kt
20. keyboard/internal/KeyPreviewChoreographer.java → KeyPreviewChoreographer.kt
21. keyboard/internal/KeySpecParser.java     → KeySpecParser.kt
22. keyboard/internal/KeyStyle.java          → KeyStyle.kt
23. keyboard/internal/KeyStylesSet.java      → KeyStylesSet.kt
24. keyboard/internal/MoreKeySpec.java       → MoreKeySpec.kt
25. keyboard/internal/KeyboardCodesSet.java  → KeyboardCodesSet.kt
26. keyboard/internal/KeyboardIconsSet.java  → KeyboardIconsSet.kt
27. keyboard/internal/AlphabetShiftState.java → AlphabetShiftState.kt
28. keyboard/internal/ShiftKeyState.java     → ShiftKeyState.kt
29. keyboard/internal/ModifierKeyState.java  → ModifierKeyState.kt
30. keyboard/internal/KeyboardState.java     → KeyboardState.kt
31. keyboard/internal/KeyboardParams.java    → KeyboardParams.kt
32. keyboard/internal/KeyboardRow.java       → KeyboardRow.kt
33. keyboard/internal/KeyboardTextsTable.java → KeyboardTextsTable.kt
34. keyboard/internal/KeyboardTextsSet.java  → KeyboardTextsSet.kt
35. keyboard/internal/KeyboardBuilder.java   → KeyboardBuilder.kt
36. keyboard/internal/UniqueKeysCache.java   → UniqueKeysCache.kt
37. keyboard/internal/PointerTrackerQueue.java → PointerTrackerQueue.kt
38. keyboard/internal/NonDistinctMultitouchHelper.java → NonDistinctMultitouchHelper.kt
39. keyboard/internal/TimerProxy.java        → TimerProxy.kt
40. keyboard/internal/TimerHandler.java      → TimerHandler.kt
41. keyboard/internal/BogusMoveEventDetector.java → BogusMoveEventDetector.kt
42. keyboard/Keyboard.java                   → Keyboard.kt
43. keyboard/ProximityInfo.java              → ProximityInfo.kt
44. keyboard/KeyDetector.java                → KeyDetector.kt
45. keyboard/MoreKeysDetector.java           → MoreKeysDetector.kt
46. keyboard/MoreKeysPanel.java              → MoreKeysPanel.kt
47. keyboard/MoreKeysKeyboard.java           → MoreKeysKeyboard.kt
48. keyboard/MoreKeysKeyboardView.java       → MoreKeysKeyboardView.kt
49. keyboard/KeyboardActionListener.java     → KeyboardActionListener.kt
50. keyboard/KeyboardTheme.java              → KeyboardTheme.kt
51. keyboard/KeyboardView.java               → KeyboardView.kt
52. keyboard/MainKeyboardView.java           → MainKeyboardView.kt
53. keyboard/PointerTracker.java             → PointerTracker.kt
54. keyboard/KeyboardSwitcher.java           → KeyboardSwitcher.kt
55. keyboard/KeyboardLayoutSet.java          → KeyboardLayoutSet.kt
56. event/Event.java                         → Event.kt
57. event/InputTransaction.java              → InputTransaction.kt
58. latin/inputlogic/InputLogic.java         → InputLogic.kt
59. latin/AvroPhoneticConverter.java         → AvroPhoneticConverter.kt
60. latin/RichInputConnection.java           → RichInputConnection.kt
61. latin/ClipboardStore.java                → ClipboardStore.kt
62. latin/AudioAndHapticFeedbackManager.java → AudioAndHapticFeedbackManager.kt
63. latin/RichInputMethodManager.java        → RichInputMethodManager.kt
64. latin/SystemBroadcastReceiver.java       → SystemBroadcastReceiver.kt
65. latin/utils/LeakGuardHandlerWrapper.java → LeakGuardHandlerWrapper.kt
66. latin/LatinIME.java                      → LatinIME.kt  ← LAST
67. compat/PreferenceManagerCompat.java      → PreferenceManagerCompat.kt
68. compat/MenuItemIconColorCompat.java      → MenuItemIconColorCompat.kt
69. compat/EditorInfoCompatUtils.java        → EditorInfoCompatUtils.kt
70. latin/settings/Settings.java             → Settings.kt
71. latin/settings/SettingsActivity.java     → SettingsActivity.kt
72. latin/settings/InputMethodSettingsFragment.java → InputMethodSettingsFragment.kt
73. latin/settings/InputMethodSettingsImpl.java → InputMethodSettingsImpl.kt
74. latin/settings/SubScreenFragment.java    → SubScreenFragment.kt
75. latin/settings/SettingsFragment.java     → SettingsFragment.kt
76. latin/settings/PreferencesSettingsFragment.java → PreferencesSettingsFragment.kt
77. latin/settings/KeyPressSettingsFragment.java → KeyPressSettingsFragment.kt
78. latin/settings/AppearanceSettingsFragment.java → AppearanceSettingsFragment.kt
79. latin/settings/ThemeSettingsFragment.java → ThemeSettingsFragment.kt
80. latin/settings/LanguagesSettingsFragment.java → LanguagesSettingsFragment.kt
81. latin/settings/SingleLanguageSettingsFragment.java → SingleLanguageSettingsFragment.kt
82. latin/settings/SeekBarDialogPreference.java → SeekBarDialogPreference.kt
83. latin/settings/RadioButtonPreference.java → RadioButtonPreference.kt
84. latin/settings/ColorDialogPreference.java → ColorDialogPreference.kt
85. latin/settings/FragmentUtils.java        → FragmentUtils.kt
86. latin/utils/SubtypeLocaleUtils.java      → SubtypeLocaleUtils.kt
87. latin/utils/SubtypePreferenceUtils.java  → SubtypePreferenceUtils.kt
88. latin/utils/LanguageOnSpacebarUtils.java → LanguageOnSpacebarUtils.kt
89. latin/utils/LocaleResourceUtils.java     → LocaleResourceUtils.kt
90. latin/utils/TypefaceUtils.java           → TypefaceUtils.kt
91. latin/utils/ResourceUtils.java           → ResourceUtils.kt
92. latin/utils/ViewLayoutUtils.java         → ViewLayoutUtils.kt
93. latin/utils/XmlParseUtils.java           → XmlParseUtils.kt
94. latin/utils/DialogUtils.java             → DialogUtils.kt
95. latin/utils/ApplicationUtils.java        → ApplicationUtils.kt
96. latin/utils/CapsModeUtils.java           → CapsModeUtils.kt
```

---

## Post-Migration Cleanup

Once all 96 files are converted:

1. Delete `app/src/main/java/` directory entirely
2. Remove all `@JvmStatic` annotations (no more Java callers)
3. Remove all `@JvmOverloads` on View constructors (Kotlin handles this natively)
4. Remove `@JvmField` and `@JvmSynthetic` annotations
5. Simplify singletons: `object` instead of `companion object` + `getInstance()`
6. Replace Java-style `getters`/`setters` with Kotlin properties
7. Convert `Runnable` lambdas to Kotlin lambdas
8. Replace `synchronized` blocks with `@Synchronized` or `Mutex`
9. Run `./gradlew lint` and fix all warnings
10. Test full checklist one final time
