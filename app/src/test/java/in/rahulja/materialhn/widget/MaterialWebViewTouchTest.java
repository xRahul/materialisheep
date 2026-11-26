package in.rahulja.materialhn.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MaterialWebViewTouchTest {

  private static class RecordingParent extends FrameLayout {
    Boolean disallowIntercept;

    RecordingParent(Context context) {
      super(context);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallow) {
      disallowIntercept = disallow;
      super.requestDisallowInterceptTouchEvent(disallow);
    }
  }

  private static class PanningWebView extends MaterialWebView {
    boolean canPanLeft;
    boolean canPanRight;

    PanningWebView(Context context) {
      super(context);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
      return direction < 0 ? canPanLeft : canPanRight;
    }
  }

  private RecordingParent mParent;
  private PanningWebView mWebView;

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    mParent = new RecordingParent(context);
    mWebView = new PanningWebView(context);
    mParent.addView(mWebView);
  }

  private void dispatchDown() {
    MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0);
    try {
      mWebView.onTouchEvent(event);
    } finally {
      event.recycle();
    }
  }

  @Test
  public void downClaimsGestureWhenPageCanPanHorizontally() {
    mWebView.canPanRight = true;

    dispatchDown();

    assertTrue("a pannable page must keep the gesture from the pager", mParent.disallowIntercept);
  }

  @Test
  public void downReleasesGestureWhenPageCannotPan() {
    mParent.disallowIntercept = true;

    dispatchDown();

    assertFalse("an unzoomed page must still swipe between tabs", mParent.disallowIntercept);
  }
}
