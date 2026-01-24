package io.github.sheepdestroyer.materialisheep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThreadPreviewViewModel(
    private val itemManager: ItemManager,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    fun loadThread(startItem: Item) {
        viewModelScope.launch {
            val threadItems = withContext(ioDispatcher) {
                val list = mutableListOf<Item>()
                var current: Item? = startItem

                // Safety check to prevent infinite loops (though unlikely with proper DAG)
                // limit to reasonable depth if needed, but HN threads can be deep.
                // A set to detect cycles could be added.
                val visited = mutableSetOf<String>()

                while (current != null) {
                    if (!visited.add(current.id)) break

                    list.add(current)

                    var parent = current.parentItem
                    // If parent object is missing but we have a parent ID, fetch it
                    if (parent == null) {
                        val parentId = current.parent
                        if (!parentId.isNullOrEmpty() && parentId != "0") {
                             parent = itemManager.getItem(parentId, ItemManager.MODE_DEFAULT)
                        }
                    }
                    current = parent
                }
                // The loop collects [child, parent, grandparent...]
                // We want [grandparent, parent, child]
                list.reversed()
            }
            _items.value = threadItems
        }
    }

    class Factory(
        private val itemManager: ItemManager,
        private val ioDispatcher: CoroutineDispatcher
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ThreadPreviewViewModel::class.java)) {
                return ThreadPreviewViewModel(itemManager, ioDispatcher) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
