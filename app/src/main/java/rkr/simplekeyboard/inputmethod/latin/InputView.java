/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2018 Raimondas Rimkus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import rkr.simplekeyboard.inputmethod.R;

public final class InputView extends FrameLayout {

    private LinearLayout mToolbarContainer;
    private ImageView mClipboardButton;
    private ImageView mSettingsButton;
    private ImageView mNumPadButton;
    private FrameLayout mClipboardPopupContainer;
    private boolean mToolbarVisible = true;

    private OnToolbarActionListener mListener;

    public interface OnToolbarActionListener {
        void onClipboardClicked();
        void onSettingsClicked();
        void onNumPadClicked();
    }

    public InputView(final Context context) {
        super(context);
    }

    public InputView(final Context context, final AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public InputView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mToolbarContainer = findViewById(R.id.toolbar_container);
        mClipboardButton = findViewById(R.id.toolbar_clipboard);
        mSettingsButton = findViewById(R.id.toolbar_settings);
        mNumPadButton = findViewById(R.id.toolbar_numpad);
        mClipboardPopupContainer = findViewById(R.id.clipboard_popup_container);

        if (mClipboardButton != null) {
            mClipboardButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onClipboardClicked();
                }
            });
        }

        if (mSettingsButton != null) {
            mSettingsButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onSettingsClicked();
                }
            });
        }

        if (mNumPadButton != null) {
            mNumPadButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onNumPadClicked();
                }
            });
        }
    }

    public void setOnToolbarActionListener(final OnToolbarActionListener listener) {
        mListener = listener;
    }

    public void setToolbarVisible(final boolean visible) {
        mToolbarVisible = visible;
        if (mToolbarContainer != null) {
            mToolbarContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public boolean isToolbarVisible() {
        return mToolbarVisible;
    }

    public void showClipboardPopup(final List<String> items, final OnClipboardItemClickListener listener) {
        if (mClipboardPopupContainer == null) return;
        mClipboardPopupContainer.removeAllViews();

        if (items == null || items.isEmpty()) {
            final TextView emptyView = new TextView(getContext());
            emptyView.setText("Clipboard is empty");
            emptyView.setTextColor(0xFFFFFFFF);
            emptyView.setPadding(16, 12, 16, 12);
            emptyView.setTextSize(14);
            mClipboardPopupContainer.addView(emptyView);
        } else {
            final LinearLayout listLayout = new LinearLayout(getContext());
            listLayout.setOrientation(LinearLayout.VERTICAL);
            listLayout.setBackgroundColor(0xFF222222);
            listLayout.setPadding(0, 4, 0, 4);

            final int maxDisplay = Math.min(items.size(), 5);
            for (int i = 0; i < maxDisplay; i++) {
                final String item = items.get(i);
                final TextView itemView = new TextView(getContext());
                final String displayText = item.length() > 50 ? item.substring(0, 50) + "..." : item;
                itemView.setText(displayText.replace("\n", " "));
                itemView.setTextColor(0xFFFFFFFF);
                itemView.setTextSize(14);
                itemView.setPadding(16, 10, 16, 10);
                itemView.setBackgroundColor(0xFF333333);
                final int index = i;
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onClipboardItemClick(item);
                    }
                    hideClipboardPopup();
                });
                listLayout.addView(itemView);
            }

            mClipboardPopupContainer.addView(listLayout);
        }

        mClipboardPopupContainer.setVisibility(View.VISIBLE);
    }

    public void hideClipboardPopup() {
        if (mClipboardPopupContainer != null) {
            mClipboardPopupContainer.setVisibility(View.GONE);
            mClipboardPopupContainer.removeAllViews();
        }
    }

    public interface OnClipboardItemClickListener {
        void onClipboardItemClick(String text);
    }
}
