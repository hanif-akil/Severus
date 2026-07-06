package rkr.simplekeyboard.inputmethod.latin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public final class ClipboardHistory implements ClipboardManager.OnPrimaryClipChangedListener {
    private static final String TAG = "ClipboardHistory";
    private static final int MAX_ITEMS = 20;

    private static ClipboardHistory sInstance;

    private final ClipboardManager mClipboardManager;
    private final List<String> mItems = new ArrayList<>();
    private ClipboardChangeListener mListener;

    public interface ClipboardChangeListener {
        void onClipboardChanged();
    }

    private ClipboardHistory(final Context context) {
        mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        mClipboardManager.addPrimaryClipChangedListener(this);
        loadCurrentClipboard();
    }

    public static synchronized ClipboardHistory getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ClipboardHistory(context.getApplicationContext());
        }
        return sInstance;
    }

    public void setListener(final ClipboardChangeListener listener) {
        mListener = listener;
    }

    @Override
    public void onPrimaryClipChanged() {
        final ClipData clipData = mClipboardManager.getPrimaryClip();
        if (clipData != null && clipData.getItemCount() > 0) {
            final CharSequence text = clipData.getItemAt(0).getText();
            if (text != null && text.length() > 0) {
                addItem(text.toString());
            }
        }
    }

    private void loadCurrentClipboard() {
        if (mClipboardManager.hasPrimaryClip()) {
            final ClipData clipData = mClipboardManager.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                final CharSequence text = clipData.getItemAt(0).getText();
                if (text != null && text.length() > 0) {
                    mItems.add(text.toString());
                }
            }
        }
    }

    private void addItem(final String text) {
        // Remove duplicate if exists
        mItems.remove(text);
        // Add to front
        mItems.add(0, text);
        // Trim to max size
        while (mItems.size() > MAX_ITEMS) {
            mItems.remove(mItems.size() - 1);
        }
        if (mListener != null) {
            mListener.onClipboardChanged();
        }
    }

    public List<String> getItems() {
        return new ArrayList<>(mItems);
    }

    public int getItemCount() {
        return mItems.size();
    }

    public void removeItem(final int index) {
        if (index >= 0 && index < mItems.size()) {
            mItems.remove(index);
            if (mListener != null) {
                mListener.onClipboardChanged();
            }
        }
    }

    public void clear() {
        mItems.clear();
        if (mListener != null) {
            mListener.onClipboardChanged();
        }
    }
}
