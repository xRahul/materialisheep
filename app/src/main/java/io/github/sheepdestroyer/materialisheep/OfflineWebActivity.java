/*
 * Copyright (c) 2016 Ha Duy Trung
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

package io.github.sheepdestroyer.materialisheep;

import android.graphics.Color;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import io.github.sheepdestroyer.materialisheep.widget.AdBlockWebViewClient;
import io.github.sheepdestroyer.materialisheep.widget.CacheableWebView;

/**
 * An activity that displays a web page in offline mode.
 */
public class OfflineWebActivity extends ThemedActivity {
    static final String EXTRA_URL = OfflineWebActivity.class.getName() + ".EXTRA_URL";
    private WebView mWebView;
    private final OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
                updateBackPressedCallback();
            }
        }
    };

    private void updateBackPressedCallback() {
        mBackPressedCallback.setEnabled(mWebView != null && mWebView.canGoBack());
    }

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState(Bundle)}.
     *                           Otherwise it is null.
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MaterialisticApplication) getApplication()).applicationComponent.inject(this);
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }
        setTitle(url);
        setContentView(R.layout.activity_offline_web);
        final NestedScrollView scrollView = (NestedScrollView) findViewById(R.id.nested_scroll_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnClickListener(v -> scrollView.smoothScrollTo(0, 0));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setSubtitle(R.string.offline);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        mWebView = (WebView) findViewById(R.id.web_view);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setWebViewClient(new AdBlockWebViewClient(Preferences.adBlockEnabled(this)) {
            @Override
            public void onPageFinished(WebView view, String url) {
                setTitle(view.getTitle());
                updateBackPressedCallback();
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                updateBackPressedCallback();
            }
        });
        mWebView.setWebChromeClient(new CacheableWebView.ArchiveClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    mWebView.setBackgroundColor(Color.WHITE);
                    mWebView.setVisibility(View.VISIBLE);
                }
            }
        });
        AppUtils.toggleWebViewZoom(mWebView.getSettings(), true);
        getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
        updateBackPressedCallback();
        mWebView.loadUrl(url);
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
