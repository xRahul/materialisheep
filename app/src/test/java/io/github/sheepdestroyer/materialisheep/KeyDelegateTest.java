package io.github.sheepdestroyer.materialisheep;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class KeyDelegateTest {
    @Mock
    RecyclerView recyclerView;
    @Mock
    LinearLayoutManager layoutManager;
    @Mock
    RecyclerView.Adapter adapter;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(recyclerView.getLayoutManager()).thenReturn(layoutManager);
        when(recyclerView.getAdapter()).thenReturn(adapter);
    }

    @Test
    public void testGetCurrentPosition_Normal() {
        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(5);
        when(layoutManager.findLastCompletelyVisibleItemPosition()).thenReturn(10);
        when(adapter.getItemCount()).thenReturn(20);

        KeyDelegate.RecyclerViewHelper helper = new KeyDelegate.RecyclerViewHelper(recyclerView, KeyDelegate.RecyclerViewHelper.SCROLL_ITEM);
        assertEquals(5, helper.getCurrentPosition());
    }

    @Test
    public void testGetCurrentPosition_AtBottom() {
        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(15);
        when(layoutManager.findLastCompletelyVisibleItemPosition()).thenReturn(19);
        when(adapter.getItemCount()).thenReturn(20);

        KeyDelegate.RecyclerViewHelper helper = new KeyDelegate.RecyclerViewHelper(recyclerView, KeyDelegate.RecyclerViewHelper.SCROLL_ITEM);
        // This is expected to fail before fix, as it currently returns 15 (first visible)
        // We expect it to return 19 after the fix
        assertEquals(19, helper.getCurrentPosition());
    }

    @Test
    public void testGetCurrentPosition_ShortList() {
        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(0);
        when(layoutManager.findLastCompletelyVisibleItemPosition()).thenReturn(4);
        when(adapter.getItemCount()).thenReturn(5);

        KeyDelegate.RecyclerViewHelper helper = new KeyDelegate.RecyclerViewHelper(recyclerView, KeyDelegate.RecyclerViewHelper.SCROLL_ITEM);
        assertEquals(0, helper.getCurrentPosition());
    }
}
