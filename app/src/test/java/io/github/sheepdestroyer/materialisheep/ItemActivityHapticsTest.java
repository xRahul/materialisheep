package io.github.sheepdestroyer.materialisheep;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.github.sheepdestroyer.materialisheep.accounts.UserServices;
import io.github.sheepdestroyer.materialisheep.data.FavoriteManager;
import io.github.sheepdestroyer.materialisheep.data.Item;
import io.github.sheepdestroyer.materialisheep.data.ItemManager;
import io.github.sheepdestroyer.materialisheep.data.SessionManager;
import io.github.sheepdestroyer.materialisheep.data.WebItem;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S)
public class ItemActivityHapticsTest {

    @Mock
    ApplicationComponent mApplicationComponent;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Context context = ApplicationProvider.getApplicationContext();
        MaterialisticApplication app = (MaterialisticApplication) context;
        app.applicationComponent = mApplicationComponent;

        org.mockito.Mockito.doAnswer(invocation -> {
            ItemActivity activity = invocation.getArgument(0);
            activity.mItemManager = mock(ItemManager.class);
            activity.mSessionManager = mock(SessionManager.class);
            activity.mFavoriteManager = mock(FavoriteManager.class);
            activity.mUserServices = mock(UserServices.class);
            activity.mCustomTabsDelegate = mock(CustomTabsDelegate.class);
            activity.mKeyDelegate = mock(KeyDelegate.class);
            activity.mAlertDialogBuilder = mock(AlertDialogBuilder.class);
            return null;
        }).when(mApplicationComponent).inject(any(ItemActivity.class));
    }

    @Test
    public void testItemActivity_Haptics() {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn("42");
        when(item.isStoryType()).thenReturn(true);
        when(item.isFavorite()).thenReturn(false);
        when(item.getUrl()).thenReturn("https://news.ycombinator.com/item?id=42");
        when(item.getDisplayedTitle()).thenReturn("Test Title");
        when(item.getDisplayedAuthor(any(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(new android.text.SpannableString("by test"));
        when(item.getDisplayedTime(any())).thenReturn(new android.text.SpannableString("1 hour ago"));
        when(item.getType()).thenReturn(Item.STORY_TYPE);

        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);

        ItemActivity itemActivity = Robolectric.buildActivity(ItemActivity.class, intent).create().get();
        View rootView = itemActivity.findViewById(android.R.id.content);
        View voteButton = itemActivity.findViewById(R.id.vote_button);
        View bookmark = itemActivity.findViewById(R.id.bookmarked);

        // Vote click -> CONFIRM
        voteButton.performClick();
        assertEquals(HapticFeedbackConstants.CONFIRM, shadowOf(voteButton).lastHapticFeedbackPerformed());

        // Bookmark click -> CONFIRM
        bookmark.performClick();
        assertEquals(HapticFeedbackConstants.CONFIRM, shadowOf(bookmark).lastHapticFeedbackPerformed());

        // onVoted failure -> REJECT on rootView
        itemActivity.onVoted(item, null);
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(rootView).lastHapticFeedbackPerformed());

        itemActivity.onVoted(item, false);
        assertEquals(HapticFeedbackConstants.REJECT, shadowOf(rootView).lastHapticFeedbackPerformed());
    }
}
