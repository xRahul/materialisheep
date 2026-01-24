package io.github.sheepdestroyer.materialisheep

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A view model that provides a list of stories.
 *
 * Rewritten to adhere to Modern Android Development standards:
 * - Uses [StateFlow] for UDF.
 * - Uses [viewModelScope] for structured concurrency.
 * - Uses dependency injection for Dispatchers to facilitate testing.
 */
class StoryListViewModel : ViewModel() {

    private lateinit var itemManager: ItemManager
    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Default, but can be injected

    data class StoryState(
        val lastUpdated: List<Item> = emptyList(),
        val current: List<Item> = emptyList(),
        val isLoading: Boolean = false,
        val error: Throwable? = null
    )

    private val _stories = MutableStateFlow(StoryState())
    val stories: StateFlow<StoryState> = _stories.asStateFlow()

    /**
     * Injects the dependencies.
     * Use this method to inject dependencies after ViewModel creation.
     */
    fun inject(itemManager: ItemManager, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
        this.itemManager = itemManager
        this.ioDispatcher = ioDispatcher
    }

    /**
     * Gets the list of stories.
     * Triggers a refresh if the current list is empty.
     */
    fun getStories(filter: String?, @ItemManager.CacheMode cacheMode: Int): StateFlow<StoryState> {
        if (_stories.value.current.isEmpty() && !_stories.value.isLoading) {
            refreshStories(filter, cacheMode)
        }
        return stories
    }

    /**
     * Refreshes the list of stories.
     */
    fun refreshStories(filter: String?, @ItemManager.CacheMode cacheMode: Int) {
        if (!::itemManager.isInitialized) {
            Log.e("StoryListViewModel", "ItemManager not injected!")
            _stories.update { it.copy(error = IllegalStateException("ItemManager not injected")) }
            return
        }

        viewModelScope.launch {
            _stories.update { it.copy(isLoading = true, error = null) }
            try {
                val items = withContext(ioDispatcher) {
                    itemManager.getStories(filter, cacheMode)
                }

                _stories.update { state ->
                    state.copy(
                        lastUpdated = state.current,
                        current = items?.toList() ?: emptyList(),
                        isLoading = false
                    )
                }
            } catch (t: Throwable) {
                Log.e("StoryListViewModel", "Error refreshing stories", t)
                _stories.update { it.copy(isLoading = false, error = t) }
            }
        }
    }
}
