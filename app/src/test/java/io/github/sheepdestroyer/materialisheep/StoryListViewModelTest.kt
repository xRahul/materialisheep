package io.github.sheepdestroyer.materialisheep

import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class StoryListViewModelTest {

    @Mock
    lateinit var itemManager: ItemManager

    @Mock
    lateinit var item: Item

    private lateinit var viewModel: StoryListViewModel
    // Use Unconfined to execute immediately on the current thread (Main in Robolectric)
    private val testDispatcher = Dispatchers.Unconfined

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // We don't need setMain if we use Robolectric and don't rely on TestDispatcher controlling time.
        // We inject the dispatcher.
        viewModel = StoryListViewModel(itemManager, testDispatcher)
    }

    @Test
    fun getStories_fetches_items_and_updates_state() {
        // Arrange
        val items = arrayOf(item)
        `when`(itemManager.getStories("filter", ItemManager.MODE_DEFAULT)).thenReturn(items)

        // Act
        viewModel.getStories("filter", ItemManager.MODE_DEFAULT)
        // Ensure coroutines launched on Main (via viewModelScope) are executed.
        ShadowLooper.idleMainLooper()

        // Assert
        val state = viewModel.stories.value
        assertNotNull(state.current)
        assertEquals(1, state.current?.size)
        assertEquals(item, state.current?.get(0))
        assertNull(state.previous)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun refreshStories_updates_previous_and_current_state() {
        // Arrange
        val items1 = arrayOf(item)
        val items2 = arrayOf(item, item)
        `when`(itemManager.getStories("filter", ItemManager.MODE_DEFAULT)).thenReturn(items1)

        // Act - First Load
        viewModel.getStories("filter", ItemManager.MODE_DEFAULT)
        ShadowLooper.idleMainLooper()

        // Assert First Load
        assertEquals(1, viewModel.stories.value.current?.size)

        // Arrange - Second Load (Refresh)
        `when`(itemManager.getStories("filter", ItemManager.MODE_DEFAULT)).thenReturn(items2)

        // Act - Refresh
        viewModel.refreshStories("filter", ItemManager.MODE_DEFAULT)
        ShadowLooper.idleMainLooper()

        // Assert Refresh
        val state = viewModel.stories.value
        assertEquals(2, state.current?.size) // New list
        assertEquals(1, state.previous?.size) // Old list
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun getStories_does_nothing_if_data_already_loaded() {
         // Arrange
        val items = arrayOf(item)
        `when`(itemManager.getStories("filter", ItemManager.MODE_DEFAULT)).thenReturn(items)

        // Act - First Load
        viewModel.getStories("filter", ItemManager.MODE_DEFAULT)
        ShadowLooper.idleMainLooper()

        // Act - Call again
        viewModel.getStories("filter", ItemManager.MODE_DEFAULT)
        ShadowLooper.idleMainLooper()

        // Assert
        // Verify itemManager.getStories called only once?
        // Mockito verify
        org.mockito.Mockito.verify(itemManager, org.mockito.Mockito.times(1)).getStories("filter", ItemManager.MODE_DEFAULT)
    }

}
