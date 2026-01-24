package io.github.sheepdestroyer.materialisheep

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryListViewModel(
    private val itemManager: ItemManager,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    data class StoryState(
        val previous: List<Item>? = null,
        val current: List<Item>? = null
    )

    private val _stories = MutableStateFlow(StoryState())
    val stories: StateFlow<StoryState> = _stories.asStateFlow()

    // For Java compatibility
    val storiesLiveData: LiveData<StoryState> = _stories.asLiveData()

    fun getStories(filter: String?, @ItemManager.CacheMode cacheMode: Int) {
        if (_stories.value.current != null) return
        fetchStories(filter, cacheMode)
    }

    fun refreshStories(filter: String?, @ItemManager.CacheMode cacheMode: Int) {
        if (_stories.value.current == null) return
        fetchStories(filter, cacheMode)
    }

    private fun fetchStories(filter: String?, @ItemManager.CacheMode cacheMode: Int) {
        viewModelScope.launch {
            try {
                // ItemManager.getStories is blocking, so we switch to IO dispatcher
                val itemsArray = withContext(ioDispatcher) {
                    itemManager.getStories(filter, cacheMode)
                }
                val itemsList = itemsArray?.toList() ?: emptyList()

                _stories.update { currentState ->
                    StoryState(
                        previous = currentState.current,
                        current = itemsList
                    )
                }
            } catch (e: Exception) {
                // In a real app we would expose error state.
                // For now just logging as per original behavior (which logged error).
                android.util.Log.e("StoryListViewModel", "Error loading stories", e)
            }
        }
    }

    class Factory(
        private val itemManager: ItemManager,
        private val ioDispatcher: CoroutineDispatcher
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StoryListViewModel::class.java)) {
                return StoryListViewModel(itemManager, ioDispatcher) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
