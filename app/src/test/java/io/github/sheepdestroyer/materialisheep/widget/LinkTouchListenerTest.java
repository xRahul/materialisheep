package io.github.sheepdestroyer.materialisheep.widget;

import android.text.Layout;
import android.text.Selection;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LinkTouchListenerTest {
    private TextView textView;
    private Layout layout;
    private ClickableSpan span;
    private LinkTouchListener listener;
    private SpannableString text;

    @Before
    public void setUp() {
        textView = mock(TextView.class);
        layout = mock(Layout.class);
        span = mock(ClickableSpan.class);

        text = new SpannableString("Click me");
        text.setSpan(span, 0, 8, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        when(textView.getText()).thenReturn(text);
        when(textView.getLayout()).thenReturn(layout);
        when(textView.getTotalPaddingLeft()).thenReturn(0);
        when(textView.getTotalPaddingTop()).thenReturn(0);
        when(textView.getScrollX()).thenReturn(0);
        when(textView.getScrollY()).thenReturn(0);

        when(layout.getLineForVertical(anyInt())).thenReturn(0);
        when(layout.getOffsetForHorizontal(anyInt(), anyFloat())).thenReturn(0);
        when(layout.getLineLeft(0)).thenReturn(0f);
        when(layout.getLineRight(0)).thenReturn(100f);

        listener = spy(new LinkTouchListener() {
            @Override
            public void onLinkClick(TextView widget, ClickableSpan span) {
            }
        });
    }

    @Test
    public void testTouchOnLink() {
        long now = System.currentTimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 50f, 10f, 0);

        assertTrue(listener.onTouch(textView, event));
        assertEquals(0, Selection.getSelectionStart(text));
        assertEquals(8, Selection.getSelectionEnd(text));
        event.recycle();

        event = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 50f, 10f, 0);
        assertTrue(listener.onTouch(textView, event));
        verify(listener).onLinkClick(textView, span);
        assertEquals(-1, Selection.getSelectionStart(text));
        event.recycle();
    }

    @Test
    public void testGhostClick() {
        long now = System.currentTimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 150f, 10f, 0);

        assertFalse(listener.onTouch(textView, event));
        verify(listener, never()).onLinkClick(textView, span);
        event.recycle();
    }

    @Test
    public void testNoLink() {
        text = new SpannableString("No link");
        when(textView.getText()).thenReturn(text);

        long now = System.currentTimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 50f, 10f, 0);

        assertFalse(listener.onTouch(textView, event));
        event.recycle();
    }
}
