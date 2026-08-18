package io.github.sheepdestroyer.materialisheep;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.core.widget.NestedScrollView;
import io.github.sheepdestroyer.materialisheep.annotation.Synthetic;
import io.github.sheepdestroyer.materialisheep.data.FileDownloader;
import io.github.sheepdestroyer.materialisheep.data.Item;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;
import io.github.sheepdestroyer.materialisheep.data.ReadabilityClient;
import io.github.sheepdestroyer.materialisheep.data.ResponseListener;
import io.github.sheepdestroyer.materialisheep.data.WebItem;
import io.github.sheepdestroyer.materialisheep.widget.AdBlockWebViewClient;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.github.sheepdestroyer.materialisheep.widget.CacheableWebView;
import io.github.sheepdestroyer.materialisheep.widget.MaterialWebView;
import io.github.sheepdestroyer.materialisheep.widget.PopupMenu;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import javax.inject.Inject;
import javax.inject.Named;
import okhttp3.Call;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.github.sheepdestroyer.materialisheep.DataModule.HN;

/** A fragment that displays a web page. */
public class WebFragment extends LazyLoadFragment implements Scrollable, KeyDelegate.BackInterceptor {
  public static final String EXTRA_ITEM = WebFragment.class.getName() + ".EXTRA_ITEM";
  private static final String STATE_EMPTY = "state:empty";
  private static final String STATE_READABILITY = "state:readability";
  private static final String STATE_FULLSCREEN = "state:fullscreen";
  private static final String STATE_CONTENT = "state:content";
  private static final int DEFAULT_PROGRESS = 20;
  public static final String PDF_LOADER_URL = "file:///android_asset/pdf/index.html";
  private static final String PDF_MIME_TYPE = "application/pdf";
  @Synthetic MaterialWebView mWebView;
  private NestedScrollView mScrollView;
  @Synthetic boolean mExternalRequired = false;

  @Inject
  @Named(HN)
  ItemManager mItemManager;

  @Inject PopupMenu mPopupMenu;
  private KeyDelegate.NestedScrollViewHelper mScrollableHelper;
  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
  private ViewGroup mFullscreenView;
  private ViewGroup mScrollViewContent;
  @Synthetic ImageButton mButtonRefresh;
  private ViewSwitcher mControls;
  private EditText mEditText;
  private View mButtonMore;
  private View mButtonNext;
  protected ProgressBar mProgressBar;
  private boolean mFullscreen;
  private boolean mIsPdf;
  protected String mContent;
  private AppUtils.SystemUiHelper mSystemUiHelper;
  private View mFragmentView;
  private FullscreenViewModel mFullscreenViewModel;
  @Inject ReadabilityClient mReadabilityClient;
  @Inject FileDownloader mFileDownloader;
  private WebItem mItem;
  private boolean mIsHackerNewsUrl, mEmpty, mReadability;
  private PdfAndroidJavascriptBridge mPdfAndroidJavascriptBridge;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    ((MaterialisticApplication) getActivity().getApplication()).applicationComponent.inject(this);
    mPreferenceObservable.subscribe(
        context,
        this::onPreferenceChanged,
        R.string.pref_readability_font,
        R.string.pref_readability_line_height,
        R.string.pref_readability_text_size);
    mFullscreenViewModel = new androidx.lifecycle.ViewModelProvider(requireActivity())
        .get(FullscreenViewModel.class);
    mFullscreenViewModel.getFullscreenEvent().observe(this, this::setFullscreen);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      mFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN, false);
      mContent = savedInstanceState.getString(STATE_CONTENT);
      mEmpty = savedInstanceState.getBoolean(STATE_EMPTY, false);
      mReadability = savedInstanceState.getBoolean(STATE_READABILITY, false);
      mItem = BundleCompat.getParcelable(savedInstanceState, EXTRA_ITEM, WebItem.class);
    } else {
      mReadability =
          Preferences.getDefaultStoryView(getActivity()) == Preferences.StoryViewMode.Readability;
      mItem = BundleCompat.getParcelable(getArguments(), EXTRA_ITEM, WebItem.class);
    }
    mIsHackerNewsUrl = AppUtils.isHackerNewsUrl(mItem);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

    mFragmentView = inflater.inflate(R.layout.fragment_web, container, false);
    mFullscreenView = (ViewGroup) mFragmentView.findViewById(R.id.fullscreen);
    mScrollViewContent = (ViewGroup) mFragmentView.findViewById(R.id.scroll_view_content);
    mScrollView = (NestedScrollView) mFragmentView.findViewById(R.id.nested_scroll_view);
    mControls = (ViewSwitcher) mFragmentView.findViewById(R.id.control_switcher);
    mWebView = (MaterialWebView) mFragmentView.findViewById(R.id.web_view);
    mButtonRefresh = (ImageButton) mFragmentView.findViewById(R.id.button_refresh);
    mButtonMore = mFragmentView.findViewById(R.id.button_more);
    mButtonNext = mFragmentView.findViewById(R.id.button_next);
    mButtonNext.setEnabled(false);
    mEditText = (EditText) mFragmentView.findViewById(R.id.edittext);
    setUpWebControls(mFragmentView);
    setUpWebView(mFragmentView);

    return mFragmentView;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mScrollableHelper = new KeyDelegate.NestedScrollViewHelper(mScrollView);
    mSystemUiHelper = new AppUtils.SystemUiHelper(getActivity().getWindow());
    mSystemUiHelper.setEnabled(!getResources().getBoolean(R.bool.multi_pane));
    if (mFullscreen) {
      setFullscreen(true);
    }
    // KEYCODE_BACK interception (KeyDelegate) stops working on API 33+ once
    // enableOnBackInvokedCallback is on, so route WebView history navigation
    // through the OnBackPressedDispatcher.
    requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(
            getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                // In pager hosts (ItemActivity / multi-pane BaseListActivity)
                // off-screen fragments keep their callbacks armed; only the
                // current page may consume back, matching the old
                // setBackInterceptor(getCurrent(...)) per-press re-pick.
                if (requireActivity() instanceof BaseListActivity
                    && !((BaseListActivity) requireActivity()).isCurrentPage(WebFragment.this)) {
                  return;
                }
                if (requireActivity() instanceof ItemActivity
                    && !((ItemActivity) requireActivity()).isCurrentPage(WebFragment.this)) {
                  return;
                }
                if (mWebView != null && mWebView.canGoBack()) {
                  mWebView.goBack();
                } else {
                  setEnabled(false);
                  requireActivity().getOnBackPressedDispatcher().onBackPressed();
                  setEnabled(true);
                }
              }
            });
  }

  @Override
  protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_article, menu);
  }

  @Override
  protected void prepareOptionsMenu(Menu menu) {
    MenuItem menuReadability = menu.findItem(R.id.menu_readability);
    menuReadability.setVisible(modeToggleEnabled());
    mMenuTintDelegate.setIcon(
        menuReadability,
        mReadability ? R.drawable.ic_web_black_24dp : R.drawable.ic_chrome_reader_mode_black_24dp);
    menuReadability.setTitle(mReadability ? R.string.article : R.string.readability);
    menu.findItem(R.id.menu_font_options).setVisible(fontEnabled());
  }

  @Override
  public boolean onMenuItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_font_options) {
      showPreferences();
      return true;
    }
    if (item.getItemId() == R.id.menu_readability) {
      mReadability = !mReadability;
      load();
      return true;
    }
    return super.onMenuItemSelected(item);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mWebView != null) {
      mWebView.onResume();
      mWebView.resumeTimers();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    pauseWebView();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(STATE_FULLSCREEN, mFullscreen);
    outState.putString(STATE_CONTENT, mContent);
    outState.putParcelable(EXTRA_ITEM, mItem);
    outState.putBoolean(STATE_EMPTY, mEmpty);
    outState.putBoolean(STATE_READABILITY, mReadability);
  }

  @Override
  public void onDestroyView() {
    if (mPdfAndroidJavascriptBridge != null) {
      mPdfAndroidJavascriptBridge.cleanUp();
      mPdfAndroidJavascriptBridge = null;
    }
    if (mWebView != null) {
      ViewGroup parent = (ViewGroup) mWebView.getParent();
      if (parent != null) {
        parent.removeView(mWebView);
      }
      mWebView.stopLoading();
      mWebView.setWebChromeClient(null);
      mWebView.setWebViewClient(null);
      mWebView.setDownloadListener(null);
      mWebView.loadUrl("about:blank");
      mWebView.clearHistory();
      mWebView.removeAllViews();
      mWebView.destroy();
      mWebView = null;
    }
    mScrollView = null;
    mFullscreenView = null;
    mScrollViewContent = null;
    mControls = null;
    mButtonRefresh = null;
    mButtonMore = null;
    mButtonNext = null;
    mEditText = null;
    mProgressBar = null;
    mScrollableHelper = null;
    mSystemUiHelper = null;
    mFragmentView = null;
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mDisposables.clear();
    if (mPdfAndroidJavascriptBridge != null) {
      mPdfAndroidJavascriptBridge.cleanUp();
      mPdfAndroidJavascriptBridge = null;
    }
    if (mWebView != null) {
      mWebView.destroy();
      mWebView = null;
    }
    // Note: mReadabilityClient is a singleton, do not call destroy() here.
    // Subscriptions are fire-and-forget and managed internally.
  }

  @Override
  public void onDetach() {
    mPreferenceObservable.unsubscribe(getActivity());
    super.onDetach();
  }

  @Override
  public void scrollToTop() {
    if (mFullscreen) {
      if (mWebView != null) {
        mWebView.pageUp(true);
      }
    } else if (mScrollableHelper != null) {
      mScrollableHelper.scrollToTop();
    }
  }

  @Override
  public boolean scrollToNext() {
    if (mFullscreen) {
      if (mWebView != null) {
        mWebView.pageDown(false);
        return true;
      }
      return false;
    } else {
      return mScrollableHelper != null && mScrollableHelper.scrollToNext();
    }
  }

  @Override
  public boolean scrollToPrevious() {
    if (mFullscreen) {
      if (mWebView != null) {
        mWebView.pageUp(false);
        return true;
      }
      return false;
    } else {
      return mScrollableHelper != null && mScrollableHelper.scrollToPrevious();
    }
  }

  @Override
  public boolean onBackPressed() {
    if (mWebView != null && mWebView.canGoBack()) {
      mWebView.goBack();
      return true;
    }
    return false;
  }

  @Override
  protected void load() {
    if (mWebView == null) {
      return;
    }
    mWebView.setVisibility(View.INVISIBLE);
    if (mIsHackerNewsUrl) {
      bindContent();
    } else if (mReadability && !mEmpty) {
      if (TextUtils.isEmpty(mContent)) {
        parse();
      } else {
        loadContent();
      }
    } else {
      loadUrl();
    }
  }

  private void loadUrl() {
    setWebSettings(true);
    reloadUrl(mItem.getUrl());
  }

  private void reloadUrl(String url) {
    reloadUrl(url, null);
  }

  @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"}) // We are a browser and need JS; PDF loader is local and trusted
  private void reloadUrl(String url, @Nullable String pdfFilePath) {
    if (mWebView == null) {
      return;
    }
    mIsPdf = false;
    if (mPdfAndroidJavascriptBridge != null) {
      mPdfAndroidJavascriptBridge.cleanUp();
      mWebView.removeJavascriptInterface("PdfAndroidJavascriptBridge");
    }
    if (pdfFilePath != null && TextUtils.equals(PDF_LOADER_URL, url)) {
      mWebView.getSettings().setJavaScriptEnabled(true);
      setProgress(80);
      mIsPdf = true;
      mPdfAndroidJavascriptBridge =
          new PdfAndroidJavascriptBridge(
              pdfFilePath,
              new PdfAndroidJavascriptBridge.Callbacks() {
                @Override
                public void onFailure() {
                  offerExternalApp();
                  setProgress(100);
                }

                @Override
                public void onLoad() {
                  setProgress(100);
                }
              });
      mWebView.addJavascriptInterface(mPdfAndroidJavascriptBridge, "PdfAndroidJavascriptBridge");
      mWebView.setInitialScale(1);
    }
    mWebView.reloadUrl(url);
  }

  @Synthetic
  void loadContent() {
    if (mWebView == null) {
      return;
    }
    setWebSettings(false);
    mWebView.reloadHtml(AppUtils.wrapHtml(getActivity(), mContent));
  }

  private void parse() {
    if (mProgressBar != null) {
      mProgressBar.setProgress(DEFAULT_PROGRESS);
    }
    mReadabilityClient.parse(mItem.getId(), mItem.getUrl(), new ReadabilityCallback(this));
  }

  private void bindContent() {
    if (mItem instanceof Item) {
      mContent = ((Item) mItem).getText();
      loadContent();
    } else {
      AppUtils.addDisposable(mDisposables, mItemManager.getItem(mItem.getId(), ItemManager.MODE_DEFAULT, new ItemResponseListener(this)));
    }
  }

  private void pauseWebView() {
    if (mWebView != null) {
      mWebView.onPause();
      mWebView.pauseTimers();
    }
  }

  private boolean fontEnabled() {
    return mReadability && !mEmpty && !TextUtils.isEmpty(mContent);
  }

  private boolean modeToggleEnabled() {
    return !mIsHackerNewsUrl && mWebView != null && !mWebView.canGoBack();
  }

  private void setUpWebControls(View view) {
    view.findViewById(R.id.toolbar_web).setOnClickListener(v -> scrollToTop());
    view.findViewById(R.id.button_back).setOnClickListener(v -> {
      if (mWebView != null) {
        mWebView.goBack();
      }
    });
    view.findViewById(R.id.button_forward).setOnClickListener(v -> {
      if (mWebView != null) {
        mWebView.goForward();
      }
    });
    view.findViewById(R.id.button_clear)
        .setOnClickListener(
            v -> {
              if (mSystemUiHelper != null) {
                mSystemUiHelper.setFullscreen(true);
              }
              reset();
              if (mControls != null) {
                mControls.showNext();
              }
            });
    view.findViewById(R.id.button_find)
        .setOnClickListener(
            v -> {
              if (mEditText != null) {
                mEditText.requestFocus();
              }
              toggleSoftKeyboard(true);
              if (mControls != null) {
                mControls.showNext();
              }
            });
    mButtonRefresh.setOnClickListener(
        v -> {
          if (mWebView != null) {
            if (mWebView.getProgress() < 100) {
              mWebView.stopLoading();
            } else {
              mWebView.reload();
            }
          }
        });
    view.findViewById(R.id.button_exit)
        .setOnClickListener(v -> mFullscreenViewModel.setFullscreen(false));
    mButtonNext.setOnClickListener(v -> {
      if (mWebView != null) {
        mWebView.findNext(true);
      }
    });
    mButtonMore.setOnClickListener(
        v ->
            mPopupMenu
                .create(getActivity(), mButtonMore, Gravity.NO_GRAVITY)
                .inflate(R.menu.menu_web)
                .setOnMenuItemClickListener(
                    item -> {
                      if (item.getItemId() == R.id.menu_font_options) {
                        showPreferences();
                        return true;
                      }
                      if (item.getItemId() == R.id.menu_zoom_in) {
                        if (mWebView != null) {
                          mWebView.zoomIn();
                        }
                        return true;
                      }
                      if (item.getItemId() == R.id.menu_zoom_out) {
                        if (mWebView != null) {
                          mWebView.zoomOut();
                        }
                        return true;
                      }
                      return false;
                    })
                .setMenuItemVisible(R.id.menu_font_options, fontEnabled())
                .show());
    mEditText.setOnEditorActionListener(
        (v, actionId, event) -> {
          findInPage();
          return true;
        });
  }

  private void setUpWebView(View view) {
    mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
    mWebView.setBackgroundColor(Color.TRANSPARENT);
    mWebView.setWebViewClient(
        new AdBlockWebViewClient(Preferences.adBlockEnabled(getActivity())) {
          @Override
          public void onPageStarted(android.webkit.WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (getActivity() != null) {
              getActivity().invalidateOptionsMenu();
            }
          }

          @Override
          public void onPageFinished(android.webkit.WebView view, String url) {
            super.onPageFinished(view, url);
            if (getActivity() != null) {
              getActivity().invalidateOptionsMenu();
            }
          }
        });
    mWebView.setWebChromeClient(
        new CacheableWebView.ArchiveClient() {
          @Override
          public void onProgressChanged(android.webkit.WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (!mIsPdf) {
              setProgress(newProgress);
            }
          }
        });
    mWebView.setDownloadListener(
        (url, userAgent, contentDisposition, mimetype, contentLength) -> {
          if (getActivity() == null) {
            return;
          }
          if (mimetype.equals(PDF_MIME_TYPE)) {
            setProgress(10);
            mIsPdf = true;
            downloadFileAndRenderPdf();
          } else {
            offerExternalApp();
          }
        });
    AppUtils.toggleWebViewZoom(mWebView.getSettings(), false);
  }

  private void offerExternalApp() {
    if (getActivity() == null || mItem == null) {
      return;
    }
    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mItem.getUrl()));
    if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
      return;
    }
    mExternalRequired = true;
    if (mWebView != null) {
      mWebView.setVisibility(GONE);
    }
    View emptyView = getActivity().findViewById(R.id.empty);
    if (emptyView != null) {
      emptyView.setVisibility(VISIBLE);
    }
    View downloadButton = getActivity().findViewById(R.id.download_button);
    if (downloadButton != null) {
      downloadButton.setOnClickListener(v -> startActivity(intent));
    }
  }

  private void setProgress(int progress) {
    if (mProgressBar != null) {
      mProgressBar.setProgress(progress);
      mProgressBar.setVisibility(progress == 100 ? GONE : VISIBLE);
    }
    if (mButtonRefresh != null) {
      mButtonRefresh.setImageResource(
          progress == 100 ? R.drawable.ic_refresh_white_24dp : R.drawable.ic_clear_white_24dp);
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void setWebSettings(boolean isRemote) {
    mReadability = !isRemote;
    if (mWebView != null) {
      mWebView.setBackgroundColor(isRemote ? Color.WHITE : Color.TRANSPARENT);
      mWebView.getSettings().setLoadWithOverviewMode(isRemote);
      mWebView.getSettings().setUseWideViewPort(isRemote);
      mWebView.getSettings().setJavaScriptEnabled(isRemote);
    }
    if (getActivity() != null) {
      getActivity().invalidateOptionsMenu();
    }
  }

  @Synthetic
  void setFullscreen(boolean isFullscreen) {
    if (getView() == null || mWebView == null || mControls == null || mScrollViewContent == null) {
      return;
    }
    mFullscreen = isFullscreen;
    mControls.setVisibility(isFullscreen ? VISIBLE : View.GONE);
    AppUtils.toggleWebViewZoom(mWebView.getSettings(), isFullscreen);
    ViewGroup.LayoutParams params = mWebView.getLayoutParams();
    if (isFullscreen) {
      if (mScrollViewContent.getParent() != mFullscreenView) {
        if (mScrollViewContent.getParent() != null) {
          ((ViewGroup) mScrollViewContent.getParent()).removeView(mScrollViewContent);
        }
        if (mFullscreenView != null) {
          mFullscreenView.addView(mScrollViewContent);
        }
      }
      if (mScrollView != null) {
        mWebView.scrollTo(mScrollView.getScrollX(), mScrollView.getScrollY());
      }
      params.height = ViewGroup.LayoutParams.MATCH_PARENT;
    } else {
      reset();
      // We'll zoom out until it returns false, which means it has min zoom level.
      // It's quite dangerous piece of code - potentially could lead to infinite loop,
      // so let's add some reasonable limit just in case
      int i = 0;
      while (mWebView.zoomOut() && i < 30) {
        i++;
      }
      if (mScrollView != null && mScrollViewContent.getParent() != mScrollView) {
        if (mScrollViewContent.getParent() != null) {
          ((ViewGroup) mScrollViewContent.getParent()).removeView(mScrollViewContent);
        }
        mScrollView.addView(mScrollViewContent);
      }
      if (mScrollView != null) {
        mScrollView.post(() -> {
          if (mScrollView != null && mWebView != null) {
            mScrollView.scrollTo(mWebView.getScrollX(), mWebView.getScrollY());
          }
        });
      }
      params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
    }
    mWebView.setLayoutParams(params);
  }

  private void showPreferences() {
    Bundle args = new Bundle();
    args.putInt(PopupSettingsFragment.EXTRA_TITLE, R.string.font_options);
    args.putIntArray(
        PopupSettingsFragment.EXTRA_XML_PREFERENCES, new int[] {R.xml.preferences_readability});
    PopupSettingsFragment fragment = new PopupSettingsFragment();
    fragment.setArguments(args);
    fragment.show(getParentFragmentManager(), PopupSettingsFragment.class.getName());
  }

  private void onPreferenceChanged(int key, boolean contextChanged) {
    if (!contextChanged) {
      load();
    }
  }

  private void reset() {
    if (mEditText != null) {
      mEditText.setText(null);
    }
    if (mButtonNext != null) {
      mButtonNext.setEnabled(false);
    }
    toggleSoftKeyboard(false);
    if (mWebView != null) {
      mWebView.clearMatches();
    }
  }

  private void findInPage() {
    if (mEditText == null || mWebView == null) {
      return;
    }
    String query = mEditText.getText().toString().trim();
    if (TextUtils.isEmpty(query)) {
      return;
    }
    mWebView.setFindListener(
        (activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
          if (isDoneCounting) {
            handleFindResults(numberOfMatches);
          }
        });
    mWebView.findAllAsync(query);
  }

  private void handleFindResults(int numberOfMatches) {
    if (mButtonNext != null) {
      mButtonNext.setEnabled(numberOfMatches > 0);
    }
    if (numberOfMatches == 0) {
      if (getContext() != null) {
        Toast.makeText(getContext(), R.string.no_matches, Toast.LENGTH_SHORT).show();
      }
    } else {
      toggleSoftKeyboard(false);
    }
  }

  private void toggleSoftKeyboard(boolean visible) {
    if (getActivity() == null || mEditText == null) {
      return;
    }
    InputMethodManager imm =
        (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm == null) {
      return;
    }
    if (visible) {
      imm.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
    } else {
      imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }
  }

  @Synthetic
  void onParsed(String content) {
    if (isAttached()) {
      mContent = content;
      if (!TextUtils.isEmpty(mContent)) {
        loadContent();
      } else {
        mEmpty = true;
        if (mReadability) {
          Toast.makeText(getActivity(), R.string.readability_failed, Toast.LENGTH_SHORT).show();
        }
        loadUrl();
      }
    }
  }

  @Synthetic
  void onItemLoaded(@NonNull Item response) {
    getActivity().invalidateOptionsMenu();
    mItem = response;
    bindContent();
  }

  private void downloadFileAndRenderPdf() {
    mFileDownloader.downloadFile(
        mItem.getUrl(),
        PDF_MIME_TYPE,
        new FileDownloader.FileDownloaderCallback() {
          @Override
          public void onFailure(Call call, IOException e) {
            offerExternalApp();
          }

          @Override
          public void onSuccess(String filePath) {
            reloadUrl(PDF_LOADER_URL, filePath);
          }
        });
  }

  static class ReadabilityCallback implements ReadabilityClient.Callback {
    private final WeakReference<WebFragment> mReadabilityFragment;

    @Synthetic
    ReadabilityCallback(WebFragment webFragment) {
      mReadabilityFragment = new WeakReference<>(webFragment);
    }

    @Override
    public void onResponse(String content) {
      if (mReadabilityFragment.get() != null && mReadabilityFragment.get().isAttached()) {
        mReadabilityFragment.get().onParsed(content);
      }
    }
  }

  static class ItemResponseListener implements ResponseListener<Item> {
    private final WeakReference<WebFragment> mFragment;

    @Synthetic
    ItemResponseListener(WebFragment webFragment) {
      mFragment = new WeakReference<>(webFragment);
    }

    @Override
    public void onResponse(@Nullable Item response) {
      if (mFragment.get() != null && mFragment.get().isAttached() && response != null) {
        mFragment.get().onItemLoaded(response);
      }
    }

    @Override
    public void onError(String errorMessage) {
      // do nothing
    }
  }

  static class PdfAndroidJavascriptBridge {
    private File mFile;
    private @Nullable RandomAccessFile mRandomAccessFile;
    private @Nullable Callbacks mCallback;
    private Handler mHandler;

    PdfAndroidJavascriptBridge(String filePath, @Nullable Callbacks callback) {
      mFile = new File(filePath);
      mCallback = callback;
      mHandler = new Handler(Looper.getMainLooper());
    }

    @JavascriptInterface
    public String getChunk(long begin, long end) {
      if (begin < 0 || end < begin || (end - begin) > 10 * 1024 * 1024) {
        return "";
      }
      try {
        if (mRandomAccessFile == null) {
          mRandomAccessFile = new RandomAccessFile(mFile, "r");
        }
        if (mRandomAccessFile != null) {
          final int bufferSize = (int) (end - begin);
          byte[] data = new byte[bufferSize];
          mRandomAccessFile.seek(begin);
          mRandomAccessFile.readFully(data);
          return Base64.encodeToString(data, Base64.DEFAULT);
        } else {
          return "";
        }
      } catch (EOFException e) {
        Log.e("Exception", e.toString());
        return "";
      } catch (IOException e) {
        Log.e("Exception", e.toString());
        return "";
      }
    }

    @JavascriptInterface
    public long getSize() {
      return mFile.length();
    }

    @JavascriptInterface
    public void onLoad() {
      if (mCallback != null) {
        mHandler.post(() -> mCallback.onLoad());
      }
    }

    @JavascriptInterface
    public void onFailure() {
      if (mCallback != null) {
        mHandler.post(() -> mCallback.onFailure());
      }
    }

    public void cleanUp() {
      try {
        if (mRandomAccessFile != null) {
          mRandomAccessFile.close();
        }
      } catch (IOException e) {
        Log.e("Exception", e.toString());
      }
    }

    interface Callbacks {
      void onFailure();

      void onLoad();
    }
  }
}
