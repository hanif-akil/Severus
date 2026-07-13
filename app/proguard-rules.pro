# Add project specific ProGuard rules here.

# Keep IME service
-keep class rkr.simplekeyboard.inputmethod.latin.LatinIME { *; }

# Keep settings fragments (referenced from XML android:fragment attributes)
-keep class rkr.simplekeyboard.inputmethod.latin.settings.*Fragment { *; }

# Keep settings activity (referenced from AndroidManifest.xml)
-keep class rkr.simplekeyboard.inputmethod.latin.settings.SettingsActivity { *; }

# Keep broadcast receiver (referenced from AndroidManifest.xml)
-keep class rkr.simplekeyboard.inputmethod.latin.SystemBroadcastReceiver { *; }

# Keep R class and resource IDs
-keep class rkr.simplekeyboard.inputmethod.R { *; }
-keep class rkr.simplekeyboard.inputmethod.R$* { *; }

# Keep custom views referenced from XML layouts
-keep class rkr.simplekeyboard.inputmethod.latin.InputView { *; }
-keep class rkr.simplekeyboard.inputmethod.keyboard.MainKeyboardView { *; }
-keep class rkr.simplekeyboard.inputmethod.keyboard.MoreKeysKeyboardView { *; }
-keep class rkr.simplekeyboard.inputmethod.keyboard.internal.DrawingPreviewPlacerView { *; }
-keep class rkr.simplekeyboard.inputmethod.keyboard.internal.KeyPreviewView { *; }

# Keep custom preferences referenced from XML
-keep class rkr.simplekeyboard.inputmethod.latin.settings.SeekBarDialogPreference { *; }
-keep class rkr.simplekeyboard.inputmethod.latin.settings.ColorDialogPreference { *; }
-keep class rkr.simplekeyboard.inputmethod.latin.settings.RadioButtonPreference { *; }
