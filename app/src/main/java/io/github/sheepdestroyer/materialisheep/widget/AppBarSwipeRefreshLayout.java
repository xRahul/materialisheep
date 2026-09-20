/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.sheepdestroyer.materialisheep.widget;

import android.app.Activity;
import android.content.Context;
import com.google.android.material.appbar.AppBarLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import io.github.sheepdestroyer.materialisheep.AppUtils;
import io.github.sheepdestroyer.materialisheep.R;

/**
 * Minor extension to {@link SwipeRefreshLayout} that is appbar-aware, only enabling when appbar
 * has been fully expanded.
 * This class assumes activity layout contains an {@link AppBarLayout} with {@link R.id#appbar}
 */
public class AppBarSwipeRefreshLayout extends SwipeRefreshLayout implements AppBarLayout.OnOffsetChangedListener {
    private AppBarLayout mAppBar;

    public AppBarSwipeRefreshLayout(Context context) {
        super(context);
        applyThemedColors(this, context);
    }

    public AppBarSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyThemedColors(this, context);
    }

    /**
     * Modernizes SwipeRefreshLayout indicator with Material 3 / themed tokens.
     * Indicator spinner: colorSecondary -> fallback colorAccent.
     * Indicator background: colorPrimaryContainer -> fallback colorSurface -> fallback colorCardBackground.
     */
    public static void applyThemedColors(@NonNull SwipeRefreshLayout swipeRefreshLayout, @NonNull Context context) {
        int spinnerColorResId = AppUtils.getThemedResId(context, com.google.android.material.R.attr.colorSecondary);
        if (spinnerColorResId == 0) {
            spinnerColorResId = AppUtils.getThemedResId(context, androidx.appcompat.R.attr.colorAccent);
        }

        int bgColorResId = AppUtils.getThemedResId(context, com.google.android.material.R.attr.colorPrimaryContainer);
        if (bgColorResId == 0) {
            bgColorResId = AppUtils.getThemedResId(context, com.google.android.material.R.attr.colorSurface);
        }
        if (bgColorResId == 0) {
            bgColorResId = AppUtils.getThemedResId(context, R.attr.colorCardBackground);
        }

        if (spinnerColorResId != 0) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(context, spinnerColorResId));
        }
        if (bgColorResId != 0) {
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(context, bgColorResId));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getContext() instanceof Activity) {
            mAppBar = (AppBarLayout) ((Activity) getContext()).findViewById(R.id.appbar);
            if (mAppBar != null) {
                mAppBar.addOnOffsetChangedListener(this);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAppBar != null) {
            mAppBar.removeOnOffsetChangedListener(this);
            mAppBar = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        this.setEnabled(i == 0);
    }
}
