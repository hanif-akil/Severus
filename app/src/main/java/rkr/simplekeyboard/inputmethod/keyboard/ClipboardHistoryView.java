package rkr.simplekeyboard.inputmethod.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.ClipboardHistory;

public class ClipboardHistoryView extends LinearLayout implements ClipboardHistory.ClipboardChangeListener {
    private KeyboardActionListener mListener;
    private final LayoutInflater mInflater;
    private LinearLayout mListView;
    private ScrollView mScrollView;
    private TextView mEmptyView;
    private boolean mIsShowing;

    public ClipboardHistoryView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mListView = findViewById(R.id.clipboard_list);
        mScrollView = findViewById(R.id.clipboard_scroll);
        mEmptyView = findViewById(R.id.clipboard_empty);
    }

    public void setListener(final KeyboardActionListener listener) {
        mListener = listener;
    }

    public void show(final ViewGroup parent) {
        if (mIsShowing) {
            return;
        }
        final ClipboardHistory history = ClipboardHistory.getInstance(getContext());
        history.setListener(this);
        refreshItems(history);
        // Ensure we're not already attached
        final ViewGroup currentParent = (ViewGroup) getParent();
        if (currentParent != null) {
            currentParent.removeView(this);
        }
        parent.addView(this);
        mIsShowing = true;
    }

    public void dismiss() {
        if (!mIsShowing) {
            return;
        }
        final ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        final ClipboardHistory history = ClipboardHistory.getInstance(getContext());
        history.setListener(null);
        mIsShowing = false;
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    @Override
    public void onClipboardChanged() {
        final ClipboardHistory history = ClipboardHistory.getInstance(getContext());
        refreshItems(history);
    }

    private void refreshItems(final ClipboardHistory history) {
        final List<String> items = history.getItems();
        mListView.removeAllViews();

        if (items.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mScrollView.setVisibility(View.GONE);
            return;
        }

        mEmptyView.setVisibility(View.GONE);
        mScrollView.setVisibility(View.VISIBLE);

        for (int i = 0; i < items.size(); i++) {
            final String text = items.get(i);
            final View itemView = mInflater.inflate(R.layout.clipboard_history_item, mListView, false);
            final TextView textView = itemView.findViewById(R.id.clipboard_item_text);
            final ImageButton deleteButton = itemView.findViewById(R.id.clipboard_item_delete);

            textView.setText(text);

            final int index = i;
            itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onTextInput(text);
                }
                dismiss();
            });

            deleteButton.setOnClickListener(v -> {
                history.removeItem(index);
            });

            mListView.addView(itemView);
        }
    }
}
