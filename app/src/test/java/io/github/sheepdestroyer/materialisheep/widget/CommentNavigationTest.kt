package io.github.sheepdestroyer.materialisheep.widget

import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommentNavigationTest {

    private lateinit var itemManager: ItemManager
    private lateinit var adapter: SinglePageItemRecyclerViewAdapter
    private lateinit var item1: Item // Root A (level 1)
    private lateinit var item2: Item // Child A.1 (level 2)
    private lateinit var item3: Item // Child A.1.a (level 3)
    private lateinit var item4: Item // Child A.2 (level 2)
    private lateinit var item5: Item // Root B (level 1)

    @Before
    fun setUp() {
        itemManager = mock(ItemManager::class.java)

        item1 = mock(Item::class.java).apply {
            `when`(id).thenReturn("1")
            `when`(longId).thenReturn(1L)
            `when`(level).thenReturn(1)
        }
        item2 = mock(Item::class.java).apply {
            `when`(id).thenReturn("2")
            `when`(longId).thenReturn(2L)
            `when`(level).thenReturn(2)
        }
        item3 = mock(Item::class.java).apply {
            `when`(id).thenReturn("3")
            `when`(longId).thenReturn(3L)
            `when`(level).thenReturn(3)
        }
        item4 = mock(Item::class.java).apply {
            `when`(id).thenReturn("4")
            `when`(longId).thenReturn(4L)
            `when`(level).thenReturn(2)
        }
        item5 = mock(Item::class.java).apply {
            `when`(id).thenReturn("5")
            `when`(longId).thenReturn(5L)
            `when`(level).thenReturn(1)
        }

        val list = arrayListOf(item1, item2, item3, item4, item5)
        val state = SinglePageItemRecyclerViewAdapter.SavedState(list)
        adapter = SinglePageItemRecyclerViewAdapter(itemManager, state, false)
    }

    @Test
    fun testFindParentPosition_deepComment_returnsDirectParent() {
        // Item 3 (level 3) at index 2 -> parent is Item 2 (level 2) at index 1
        assertEquals(1, adapter.findParentPosition(2, item3))

        // Item 2 (level 2) at index 1 -> parent is Item 1 (level 1) at index 0
        assertEquals(0, adapter.findParentPosition(1, item2))

        // Item 4 (level 2) at index 3 -> parent is Item 1 (level 1) at index 0
        assertEquals(0, adapter.findParentPosition(3, item4))
    }

    @Test
    fun testFindParentPosition_rootComment_returnsMinusOne() {
        // Item 1 (level 1) at index 0 has no parent
        assertEquals(-1, adapter.findParentPosition(0, item1))

        // Item 5 (level 1) at index 4 has no parent
        assertEquals(-1, adapter.findParentPosition(4, item5))
    }

    @Test
    fun testFindRootPosition_nestedComment_returnsSubtreeRoot() {
        // Item 3 (level 3) at index 2 -> root is Item 1 at index 0
        assertEquals(0, adapter.findRootPosition(2, item3))

        // Item 4 (level 2) at index 3 -> root is Item 1 at index 0
        assertEquals(0, adapter.findRootPosition(3, item4))

        // Item 5 (level 1) at index 4 -> root is itself at index 4
        assertEquals(4, adapter.findRootPosition(4, item5))
    }

    @Test
    fun testFindParentPosition_nullOrBoundary_returnsMinusOne() {
        assertEquals(-1, adapter.findParentPosition(-1, item1))
        assertEquals(-1, adapter.findParentPosition(0, null))
    }
}
