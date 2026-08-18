package io.github.sheepdestroyer.materialisheep.widget

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import io.github.sheepdestroyer.materialisheep.Preferences
import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthorBadgeTest {

    private lateinit var context: Context
    private lateinit var itemManager: ItemManager
    private lateinit var adapter: SinglePageItemRecyclerViewAdapter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(io.github.sheepdestroyer.materialisheep.R.style.AppTheme)
        itemManager = mock(ItemManager::class.java)
        val state = SinglePageItemRecyclerViewAdapter.SavedState(ArrayList())
        adapter = SinglePageItemRecyclerViewAdapter(itemManager, state, false)
        Preferences.setUsername(context, "my_user")
        adapter.initDisplayOptions(context)
    }

    @Test
    fun testAppendAuthorBadges_whenAuthorIsOP_appendsOP() {
        adapter.setStoryAuthor("story_author")
        val commentItem = mock(Item::class.java)
        `when`(commentItem.by).thenReturn("story_author")

        val textView = TextView(context)
        textView.text = "2 hours ago"
        adapter.appendAuthorBadges(textView, commentItem)

        assertTrue(textView.text.toString().contains("OP"))
        assertFalse(textView.text.toString().contains("YOU"))
    }

    @Test
    fun testAppendAuthorBadges_whenAuthorIsCurrentUser_appendsYOU() {
        adapter.setStoryAuthor("story_author")
        val commentItem = mock(Item::class.java)
        `when`(commentItem.by).thenReturn("my_user")

        val textView = TextView(context)
        textView.text = "10 minutes ago"
        adapter.appendAuthorBadges(textView, commentItem)

        assertTrue(textView.text.toString().contains("YOU"))
        assertFalse(textView.text.toString().contains("OP"))
    }

    @Test
    fun testAppendAuthorBadges_whenAuthorIsThirdParty_noBadge() {
        adapter.setStoryAuthor("story_author")
        val commentItem = mock(Item::class.java)
        `when`(commentItem.by).thenReturn("random_reader")

        val textView = TextView(context)
        textView.text = "5 minutes ago"
        adapter.appendAuthorBadges(textView, commentItem)

        assertEquals("5 minutes ago", textView.text.toString())
    }

    @Test
    fun testAppendAuthorBadges_nullOrEmptyAuthor_doesNothing() {
        adapter.setStoryAuthor("story_author")
        val commentItem = mock(Item::class.java)
        `when`(commentItem.by).thenReturn(null)

        val textView = TextView(context)
        textView.text = "1 hour ago"
        adapter.appendAuthorBadges(textView, commentItem)

        assertEquals("1 hour ago", textView.text.toString())
    }
}
