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

package in.rahulja.materialhn.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebViewClient;
import in.rahulja.materialhn.annotation.Synthetic;

public class MaterialWebView extends android.webkit.WebView {
  static final String BLANK = "about:blank";
  static final String FILE = "file:///";
  private final HistoryWebViewClient mClient = new HistoryWebViewClient();
  @Synthetic boolean mClearHistoryOnCommit;
  @Synthetic String mPendingUrl;
  private final int mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
  private float mDownX, mDownY;

  public MaterialWebView(Context context) {
    this(context, null);
  }

  public MaterialWebView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MaterialWebView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    super.setWebViewClient(mClient);
  }

  @Override
  public void setWebViewClient(WebViewClient client) {
    mClient.wrap(client);
  }

  @Override
  public boolean canGoBack() {
    return !mClearHistoryOnCommit && super.canGoBack();
  }

  public void reloadUrl(String url) {
    if (getProgress() < 100) {
      stopLoading(); // this fires onPageFinished for the current URL
    }
    mClearHistoryOnCommit = true;
    mPendingUrl = url;
    loadUrl(url);
  }

  public void reloadHtml(String html) {
    stopLoading();
    mClearHistoryOnCommit = true;
    mPendingUrl = FILE;
    loadDataWithBaseURL(FILE, html, "text/html", "UTF-8", null);
  }

  @SuppressLint("ClickableViewAccessibility") // the WebView handles clicks itself
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    ViewParent parent = getParent();
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        mDownX = event.getX();
        mDownY = event.getY();
        // ViewPager2 intercepts on the same event once slop is passed, so a
        // pannable page must claim the gesture before that happens.
        if (parent != null) {
          parent.requestDisallowInterceptTouchEvent(
              canScrollHorizontally(-1) || canScrollHorizontally(1));
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (parent != null
            && GestureIntent.claimsHorizontal(
                event.getX() - mDownX,
                event.getY() - mDownY,
                canScrollHorizontally(-1),
                canScrollHorizontally(1),
                mTouchSlop)) {
          parent.requestDisallowInterceptTouchEvent(true);
        }
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (parent != null) {
          parent.requestDisallowInterceptTouchEvent(false);
        }
        break;
      default:
        break;
    }
    return super.onTouchEvent(event);
  }

  protected WebResourceResponse interceptRequest(WebResourceRequest request) {
    return null;
  }

  static class HistoryWebViewClient extends WebViewClient {
    private static final String TAG = "MaterialWebView";
    private static final java.util.Set<String> EXTERNAL_SCHEMES =
        new java.util.HashSet<>(
            java.util.Arrays.asList("mailto", "tel", "market", "sms"));

    private WebViewClient mClient;

    /**
     * True when a URL may be opened from page content. http and https load in the
     * WebView; mailto, tel, sms, market and this app's own scheme are handed to the
     * system. Everything else (javascript:, file:, content:, intent:, data:) is
     * dropped, so a page cannot reach local storage or launch arbitrary intents.
     */
    static boolean isAllowedNavigation(String url) {
      if (TextUtils.isEmpty(url)) {
        return false;
      }
      android.net.Uri uri = android.net.Uri.parse(url);
      String scheme = uri.getScheme();
      if (scheme == null) {
        return false;
      }
      scheme = scheme.toLowerCase(java.util.Locale.ROOT);
      return "http".equals(scheme)
          || "https".equals(scheme)
          || EXTERNAL_SCHEMES.contains(scheme)
          || scheme.equals(in.rahulja.materialhn.BuildConfig.APPLICATION_ID);
    }

    private static boolean handleNavigation(android.webkit.WebView view, String url) {
      if (TextUtils.isEmpty(url)) {
        return true;
      }
      android.net.Uri uri = android.net.Uri.parse(url);
      String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
      if ("http".equals(scheme) || "https".equals(scheme)) {
        return false;
      }
      if (!isAllowedNavigation(url)) {
        android.util.Log.w(TAG, "Blocked navigation to scheme: " + scheme);
        return true;
      }
      android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);
      try {
        view.getContext().startActivity(intent);
      } catch (android.content.ActivityNotFoundException ignored) {
        // No handler installed; nothing else to do.
      }
      return true;
    }

    @Override
    public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
      if (handleNavigation(view, url)) {
        return true;
      }
      return mClient != null && mClient.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public boolean onRenderProcessGone(
        android.webkit.WebView view, android.webkit.RenderProcessGoneDetail detail) {
      if (mClient != null) {
        return mClient.onRenderProcessGone(view, detail);
      }
      return super.onRenderProcessGone(view, detail);
    }

    @Override
    public boolean shouldOverrideUrlLoading(
        android.webkit.WebView view, WebResourceRequest request) {
      if (request.isForMainFrame() && "GET".equalsIgnoreCase(request.getMethod())) {
        String url = request.getUrl().toString();
        String upgraded = ArticleUrlPolicy.upgradeToHttps(url);
        if (upgraded != null && !upgraded.equals(url)) {
          view.loadUrl(upgraded);
          return true;
        }
      }
      if (handleNavigation(view, request.getUrl().toString())) {
        return true;
      }
      return mClient != null && mClient.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onPageStarted(android.webkit.WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);
      view.pageUp(true);
      if (mClient != null) {
        mClient.onPageStarted(view, url, favicon);
      }
    }

    @Override
    public void onPageCommitVisible(android.webkit.WebView view, String url) {
      super.onPageCommitVisible(view, url);
      view.setVisibility(VISIBLE);
      if (view instanceof MaterialWebView) {
        ((MaterialWebView) view).mPendingUrl = null;
      }
      clearPendingHistory(view);
      if (mClient != null) {
        mClient.onPageCommitVisible(view, url);
      }
    }

    @Override
    public void onPageFinished(android.webkit.WebView view, String url) {
      super.onPageFinished(view, url);
      // Fallback for pages where onPageCommitVisible never fires (data: documents),
      // without revealing a page that a newer load has already superseded.
      if (view instanceof MaterialWebView) {
        MaterialWebView webView = (MaterialWebView) view;
        if (webView.mPendingUrl != null
            && (TextUtils.equals(url, webView.mPendingUrl)
                || (TextUtils.equals(webView.mPendingUrl, FILE)
                    && (url == null || url.startsWith("data:"))))) {
          view.setVisibility(VISIBLE);
          webView.mPendingUrl = null;
        }
      }
      clearPendingHistory(view);
      if (mClient != null) {
        mClient.onPageFinished(view, url);
      }
    }

    @Override
    public void doUpdateVisitedHistory(
        android.webkit.WebView view, String url, boolean isReload) {
      super.doUpdateVisitedHistory(view, url, isReload);
      if (mClient != null) {
        mClient.doUpdateVisitedHistory(view, url, isReload);
      }
    }

    @Override
    public void onReceivedError(
        android.webkit.WebView view,
        WebResourceRequest request,
        android.webkit.WebResourceError error) {
      if (request.isForMainFrame() && view instanceof MaterialWebView) {
        // A load that never commits must not keep back navigation disabled.
        ((MaterialWebView) view).mClearHistoryOnCommit = false;
      }
      if (mClient != null) {
        mClient.onReceivedError(view, request, error);
      }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(
        android.webkit.WebView view,
        int errorCode,
        String description,
        String failingUrl) {
      if (mClient != null) {
        mClient.onReceivedError(view, errorCode, description, failingUrl);
      } else {
        super.onReceivedError(view, errorCode, description, failingUrl);
      }
    }

    @Override
    public void onReceivedSslError(
        android.webkit.WebView view,
        android.webkit.SslErrorHandler handler,
        android.net.http.SslError error) {
      if (mClient != null) {
        mClient.onReceivedSslError(view, handler, error);
      } else {
        super.onReceivedSslError(view, handler, error);
      }
    }

    private static void clearPendingHistory(android.webkit.WebView view) {
      if (view instanceof MaterialWebView && ((MaterialWebView) view).mClearHistoryOnCommit) {
        ((MaterialWebView) view).mClearHistoryOnCommit = false;
        view.clearHistory();
      }
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(
        android.webkit.WebView view, WebResourceRequest request) {
      if (view instanceof MaterialWebView) {
        WebResourceResponse response = ((MaterialWebView) view).interceptRequest(request);
        if (response != null) {
          return response;
        }
      }
      return mClient != null
          ? mClient.shouldInterceptRequest(view, request)
          : super.shouldInterceptRequest(view, request);
    }

    @Synthetic
    void wrap(WebViewClient client) {
      mClient = client;
    }
  }
}
