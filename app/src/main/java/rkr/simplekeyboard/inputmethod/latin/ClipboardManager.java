package rkr.simplekeyboard.inputmethod.latin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;

public class ClipboardStore {
    private static final String PREF_CLIPBOARD_ITEMS = "clipboard_items";
    private static final String PREF_CLIPBOARD_COUNT = "pref_clipboard_max_items";
    private static final int DEFAULT_MAX_ITEMS = 10;

    private final SharedPreferences mPrefs;
    private final int mMaxItems;
    private final List<String> mItems;

    public ClipboardStore(final Context context) {
        mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context);
        mMaxItems = mPrefs.getInt(PREF_CLIPBOARD_COUNT, DEFAULT_MAX_ITEMS);
        mItems = loadItems();
    }

    public int getMaxItems() {
        return mMaxItems;
    }

    public List<String> getItems() {
        return new ArrayList<>(mItems);
    }

    public String getItem(final int index) {
        if (index >= 0 && index < mItems.size()) {
            return mItems.get(index);
        }
        return null;
    }

    public int getItemCount() {
        return mItems.size();
    }

    public void addItem(final String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        mItems.remove(text);
        mItems.add(0, text);
        while (mItems.size() > mMaxItems) {
            mItems.remove(mItems.size() - 1);
        }
        saveItems();
    }

    public void removeItem(final int index) {
        if (index >= 0 && index < mItems.size()) {
            mItems.remove(index);
            saveItems();
        }
    }

    public void clearAll() {
        mItems.clear();
        saveItems();
    }

    private List<String> loadItems() {
        final List<String> items = new ArrayList<>();
        final int count = mPrefs.getInt(PREF_CLIPBOARD_ITEMS + "_count", 0);
        for (int i = 0; i < count; i++) {
            final String item = mPrefs.getString(PREF_CLIPBOARD_ITEMS + "_" + i, null);
            if (item != null && !item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    private void saveItems() {
        final SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(PREF_CLIPBOARD_ITEMS + "_count", mItems.size());
        for (int i = 0; i < mItems.size(); i++) {
            editor.putString(PREF_CLIPBOARD_ITEMS + "_" + i, mItems.get(i));
        }
        editor.apply();
    }
}
