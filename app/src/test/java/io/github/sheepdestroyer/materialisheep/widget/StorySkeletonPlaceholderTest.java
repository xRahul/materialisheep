package io.github.sheepdestroyer.materialisheep.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.animation.ValueAnimator;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
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
import io.github.sheepdestroyer.materialisheep.R;
import io.github.sheepdestroyer.materialisheep.accounts.UserServices;
import io.github.sheepdestroyer.materialisheep.data.FavoriteManager;
import io.github.sheepdestroyer.materialisheep.data.Item;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;
import io.github.sheepdestroyer.materialisheep.data.SessionManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S)
public class StorySkeletonPlaceholderTest {

    @Mock
    ApplicationComponent mApplicationComponent;

    private ListActivity mActivity;
    private FrameLayout mContainer;
    private RecyclerView mRecyclerView;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mActivity = Robolectric.buildActivity(ListActivity.class).get();
        mContainer = new FrameLayout(mActivity);
        mActivity.setContentView(mContainer);
        mRecyclerView = new RecyclerView(mActivity);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        mContainer.addView(mRecyclerView);
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
    public void testInitialLoadingState_ShowsSkeletonCountAndType() {
        StoryRecyclerViewAdapter adapter = new StoryRecyclerViewAdapter(mActivity);
        mRecyclerView.setAdapter(adapter);

        assertEquals(0, adapter.getItemCount());

        adapter.setLoading(true);
        assertTrue(adapter.isLoading());
        assertEquals(StoryRecyclerViewAdapter.SKELETON_COUNT, adapter.getItemCount());

        int skeletonType = StoryRecyclerViewAdapter.getViewTypeSkeleton();
        assertEquals(2, skeletonType);
        for (int i = 0; i < StoryRecyclerViewAdapter.SKELETON_COUNT; i++) {
            assertEquals(skeletonType, adapter.getItemViewType(i));
        }

        adapter.setLoading(false);
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void testUnhydratedStoryItems_ReturnSkeletonViewType() {
        StoryRecyclerViewAdapter adapter = new StoryRecyclerViewAdapter(mActivity);
        mRecyclerView.setAdapter(adapter);

        Item unhydrated1 = mock(Item.class);
        when(unhydrated1.getId()).thenReturn("1");
        when(unhydrated1.getLongId()).thenReturn(1L);
        when(unhydrated1.getLocalRevision()).thenReturn(0);

        Item unhydrated2 = mock(Item.class);
        when(unhydrated2.getId()).thenReturn("2");
        when(unhydrated2.getLongId()).thenReturn(2L);
        when(unhydrated2.getLocalRevision()).thenReturn(-1);

        adapter.setItems(new Item[]{unhydrated1, unhydrated2});

        assertEquals(2, adapter.getItemCount());
        int skeletonType = StoryRecyclerViewAdapter.getViewTypeSkeleton();
        assertEquals(skeletonType, adapter.getItemViewType(0));
        assertEquals(skeletonType, adapter.getItemViewType(1));
    }

    @Test
    public void testHydratedStoryItems_ReturnCardOrFlatViewType() {
        StoryRecyclerViewAdapter adapter = new StoryRecyclerViewAdapter(mActivity);
        mRecyclerView.setAdapter(adapter);

        Item hydrated = mock(Item.class);
        when(hydrated.getId()).thenReturn("1");
        when(hydrated.getLongId()).thenReturn(1L);
        when(hydrated.getLocalRevision()).thenReturn(1);
        when(hydrated.getRank()).thenReturn(1);

        adapter.setItems(new Item[]{hydrated});

        assertEquals(1, adapter.getItemCount());
        assertEquals(ListRecyclerViewAdapter.VIEW_TYPE_CARD, adapter.getItemViewType(0));

        adapter.setCardViewEnabled(false);
        assertEquals(ListRecyclerViewAdapter.VIEW_TYPE_FLAT, adapter.getItemViewType(0));
    }

    @Test
    public void testOnCreateViewHolder_InflatesSkeletonViewHolderAndViews() {
        StoryRecyclerViewAdapter adapter = new StoryRecyclerViewAdapter(mActivity);
        mRecyclerView.setAdapter(adapter);

        int skeletonType = StoryRecyclerViewAdapter.getViewTypeSkeleton();
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(mRecyclerView, skeletonType);

        assertTrue(holder instanceof StoryRecyclerViewAdapter.SkeletonViewHolder);
        assertNotNull(holder.itemView.findViewById(R.id.skeleton_score));
        assertNotNull(holder.itemView.findViewById(R.id.skeleton_title_1));
        assertNotNull(holder.itemView.findViewById(R.id.skeleton_title_2));
        assertNotNull(holder.itemView.findViewById(R.id.skeleton_subtitle));
        assertNotNull(holder.itemView.findViewById(R.id.skeleton_comment));
    }

    @Test
    public void testSkeletonViewHolder_AnimationLifecycle() {
        StoryRecyclerViewAdapter adapter = new StoryRecyclerViewAdapter(mActivity);
        mRecyclerView.setAdapter(adapter);

        int skeletonType = StoryRecyclerViewAdapter.getViewTypeSkeleton();
        StoryRecyclerViewAdapter.SkeletonViewHolder holder =
                (StoryRecyclerViewAdapter.SkeletonViewHolder) adapter.onCreateViewHolder(mRecyclerView, skeletonType);

        adapter.onBindViewHolder(holder, 0);

        ValueAnimator animator = holder.getAnimator();
        assertNotNull(animator);
        assertTrue(animator.isRunning());
        assertEquals(900L, animator.getDuration());
        assertEquals(ValueAnimator.REVERSE, animator.getRepeatMode());
        assertTrue(animator.getRepeatCount() == ValueAnimator.INFINITE || animator.getRepeatCount() == 1);

        adapter.onViewDetachedFromWindow(holder);
        assertNull(holder.getAnimator());
        assertEquals(1.0f, holder.itemView.getAlpha(), 0.001f);

        adapter.onViewAttachedToWindow(holder);
        assertNotNull(holder.getAnimator());
        assertTrue(holder.getAnimator().isRunning());

        adapter.onViewRecycled(holder);
        assertNull(holder.getAnimator());
        assertEquals(1.0f, holder.itemView.getAlpha(), 0.001f);
    }
}
