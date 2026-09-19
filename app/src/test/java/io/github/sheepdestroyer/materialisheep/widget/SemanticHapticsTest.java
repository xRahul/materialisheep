package io.github.sheepdestroyer.materialisheep.widget;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.github.sheepdestroyer.materialisheep.AlertDialogBuilder;
import io.github.sheepdestroyer.materialisheep.ApplicationComponent;
import io.github.sheepdestroyer.materialisheep.ListActivity;
import io.github.sheepdestroyer.materialisheep.MaterialisticApplication;
import io.github.sheepdestroyer.materialisheep.accounts.UserServices;
import io.github.sheepdestroyer.materialisheep.data.Favorite;
import io.github.sheepdestroyer.materialisheep.data.FavoriteManager;
import io.github.sheepdestroyer.materialisheep.data.Item;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;
import io.github.sheepdestroyer.materialisheep.data.SessionManager;
import io.github.sheepdestroyer.materialisheep.data.SyncScheduler;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S)
public class SemanticHapticsTest {

    @Mock
    ApplicationComponent mApplicationComponent;

    private ListActivity mActivity;
    private FrameLayout mContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mActivity = Robolectric.buildActivity(ListActivity.class).get();
        mContainer = new FrameLayout(mActivity);
        mActivity.setContentView(mContainer);
        MaterialisticApplication app = (MaterialisticApplication) mActivity.getApplicationContext();
        app.applicationComponent = mApplicationComponent;

        org.mockito.Mockito.doAnswer(invocation -> {
            StoryRecyclerViewAdapter adapter = invocation.getArgument(0);
            adapter.mItemManager = mock(ItemManager.class);
            adapter.mSessionManager = mock(SessionManager.class);
            adapter.mFavoriteManager = mock(FavoriteManager.class);
            adapter.mUserServices = mock(UserServices.class);
            adapter.mPopupMenu = mock(PopupMenu.class);
            adapter.mAlertDialogBuilder = mock(AlertDialogBuilder.class);
            return null;
        }).when(mApplicationComponent).inject(any(StoryRecyclerViewAdapter.class));
    }

    @Test
    public void testStoryRecyclerViewAdapter_ToggleSave_TriggersConfirm() {
        StoryRecyclerViewAdapter adapter = new StoryRecyclerViewAdapter(mActivity);
        RecyclerView recyclerView = new RecyclerView(mActivity);
        mContainer.addView(recyclerView);
        recyclerView.setAdapter(adapter);

        Item item = mock(Item.class);
        when(item.getId()).thenReturn("1");
        when(item.isFavorite()).thenReturn(false);
        adapter.toggleSave(item);

        assertEquals(HapticFeedbackConstants.CONFIRM, shadowOf(recyclerView).lastHapticFeedbackPerformed());
    }

    @Test
    public void testStoryRecyclerViewAdapter_OnVotedFailure_TriggersReject() {
        StoryRecyclerViewAdapter adapter = new StoryRecyclerViewAdapter(mActivity);
        RecyclerView recyclerView = new RecyclerView(mActivity);
        mContainer.addView(recyclerView);
        recyclerView.setAdapter(adapter);

        Item item = mock(Item.class);
        when(item.getId()).thenReturn("2");
        StoryRecyclerViewAdapter.VoteAction voteAction = new StoryRecyclerViewAdapter.VoteAction("2", item, 0);

        adapter.onVoted(voteAction, false);
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(recyclerView).lastHapticFeedbackPerformed());

        adapter.onVoted(voteAction, null);
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(recyclerView).lastHapticFeedbackPerformed());
    }

    @Test
    public void testStoryRecyclerViewAdapter_OnSwiped_TriggersGestureThresholdActivate() {
        StoryRecyclerViewAdapter adapter = new StoryRecyclerViewAdapter(mActivity);
        RecyclerView recyclerView = new RecyclerView(mActivity);
        mContainer.addView(recyclerView);
        recyclerView.setAdapter(adapter);

        View itemView = new View(mActivity);
        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(itemView) {};

        adapter.mCallback.onSwiped(holder, ItemTouchHelper.LEFT);
        assertEquals(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE, shadowOf(itemView).lastHapticFeedbackPerformed());
    }

    @Test
    public void testFavoriteRecyclerViewAdapter_Haptics() {
        FavoriteRecyclerViewAdapter.ActionModeDelegate delegate = mock(FavoriteRecyclerViewAdapter.ActionModeDelegate.class);
        FavoriteRecyclerViewAdapter adapter = new FavoriteRecyclerViewAdapter(mActivity, delegate);
        adapter.mFavoriteManager = mock(FavoriteManager.class);
        adapter.mUserServices = mock(UserServices.class);
        adapter.mSyncScheduler = mock(SyncScheduler.class);
        adapter.mPopupMenu = mock(PopupMenu.class);
        adapter.mAlertDialogBuilder = mock(AlertDialogBuilder.class);

        RecyclerView recyclerView = new RecyclerView(mActivity);
        mContainer.addView(recyclerView);
        recyclerView.setAdapter(adapter);

        // onVoted success -> CONFIRM
        adapter.onVoted(true);
        assertEquals(HapticFeedbackConstants.CONFIRM, shadowOf(recyclerView).lastHapticFeedbackPerformed());

        // onVoted failure -> REJECT
        adapter.onVoted(false);
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(recyclerView).lastHapticFeedbackPerformed());

        adapter.onVoted(null);
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(recyclerView).lastHapticFeedbackPerformed());

        // removeSelection -> CONFIRM
        adapter.removeSelection();
        assertEquals(HapticFeedbackConstants.CONFIRM, shadowOf(recyclerView).lastHapticFeedbackPerformed());

        // onSwiped -> GESTURE_THRESHOLD_ACTIVATE
        View itemView = new View(mActivity);
        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(itemView) {};
        adapter.mCallback.onSwiped(holder, ItemTouchHelper.LEFT);
        assertEquals(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE, shadowOf(itemView).lastHapticFeedbackPerformed());
    }

    @Test
    public void testItemRecyclerViewAdapter_OnVotedHaptics() {
        ItemManager itemManager = mock(ItemManager.class);
        TestItemRecyclerViewAdapter adapter = new TestItemRecyclerViewAdapter(itemManager);
        RecyclerView recyclerView = new RecyclerView(mActivity);
        mContainer.addView(recyclerView);
        recyclerView.setAdapter(adapter);

        // onVoted success -> CONFIRM
        adapter.onVoted(true);
        assertEquals(HapticFeedbackConstants.CONFIRM, shadowOf(recyclerView).lastHapticFeedbackPerformed());

        // onVoted failure -> REJECT
        adapter.onVoted(false);
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(recyclerView).lastHapticFeedbackPerformed());

        adapter.onVoted(null);
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(recyclerView).lastHapticFeedbackPerformed());
    }

    static class TestItemRecyclerViewAdapter extends ItemRecyclerViewAdapter<ItemRecyclerViewAdapter.ItemViewHolder> {
        TestItemRecyclerViewAdapter(ItemManager itemManager) {
            super(itemManager);
        }

        @Override
        protected Item getItem(int position) {
            return null;
        }

        @Override
        public int getItemCount() {
            return 0;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }
    }
}
