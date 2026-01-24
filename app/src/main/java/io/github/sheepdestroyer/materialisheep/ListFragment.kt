/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.sheepdestroyer.materialisheep

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import io.github.sheepdestroyer.materialisheep.data.AlgoliaClient
import io.github.sheepdestroyer.materialisheep.data.AlgoliaPopularClient
import io.github.sheepdestroyer.materialisheep.data.FavoriteManager
import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager
import io.github.sheepdestroyer.materialisheep.data.MaterialisticDatabase
import io.github.sheepdestroyer.materialisheep.widget.StoryRecyclerViewAdapter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * A fragment that displays a list of stories.
 *
 * Rewritten to adhere to Modern Android Development standards:
 * - Uses [lifecycleScope] and [repeatOnLifecycle] for safe Flow collection.
 * - Uses [StoryListViewModel] with StateFlow.
 * - Enforces Strict Type safety and Null Safety.
 */
class ListFragment : BaseListFragment() {

    companion object {
        @JvmField
        val EXTRA_ITEM_MANAGER = "${ListFragment::class.java.name}.EXTRA_ITEM_MANAGER"
        @JvmField
        val EXTRA_FILTER = "${ListFragment::class.java.name}.EXTRA_FILTER"
        private const val STATE_FILTER = "state:filter"
        private const val STATE_CACHE_MODE = "state:cacheMode"
    }

    private val preferenceObservable = Preferences.Observable()

    private val observer = Observer<Uri> { uri ->
        if (uri == null) return@Observer

        var toastMessageResId = 0
        if (FavoriteManager.isAdded(uri)) {
            toastMessageResId = R.string.toast_saved
        } else if (FavoriteManager.isRemoved(uri)) {
            toastMessageResId = R.string.toast_removed
        }

        if (toastMessageResId == 0) return@Observer

        mRecyclerView?.let { recyclerView ->
            Snackbar.make(recyclerView, toastMessageResId, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo) { adapter.toggleSave(uri.lastPathSegment) }
                .show()
        }
    }

    private var internalAdapter: StoryRecyclerViewAdapter? = null
    override fun getAdapter(): StoryRecyclerViewAdapter {
        if (internalAdapter == null) {
            internalAdapter = StoryRecyclerViewAdapter(context)
        }
        return internalAdapter!!
    }

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    @Inject
    @Named(DataModule.HN)
    lateinit var hnItemManager: ItemManager

    @Inject
    @Named(DataModule.ALGOLIA)
    lateinit var algoliaItemManager: ItemManager

    @Inject
    @Named(DataModule.POPULAR)
    lateinit var popularItemManager: ItemManager

    private lateinit var storyListViewModel: StoryListViewModel
    private lateinit var errorView: View
    private lateinit var emptyView: View

    private var refreshCallback: RefreshCallback? = null
    private var filter: String? = null
    private var cacheMode = ItemManager.MODE_DEFAULT

    interface RefreshCallback {
        fun onRefreshed()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as MaterialisticApplication).applicationComponent.inject(this)
        if (context is RefreshCallback) {
            refreshCallback = context
        }
        preferenceObservable.subscribe(
            context,
            this::onPreferenceChanged,
            R.string.pref_highlight_updated,
            R.string.pref_username,
            R.string.pref_auto_viewed
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            filter = savedInstanceState.getString(STATE_FILTER)
            cacheMode = savedInstanceState.getInt(STATE_CACHE_MODE)
        } else {
            filter = arguments?.getString(EXTRA_FILTER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        errorView = view.findViewById(R.id.empty)
        emptyView = view.findViewById(R.id.empty_search)
        mRecyclerView = view.findViewById(R.id.recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipe_layout)

        swipeRefreshLayout.setColorSchemeResources(R.color.white)
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(
            AppUtils.getThemedResId(activity, androidx.appcompat.R.attr.colorAccent)
        )

        // Initial state logic handled by Flow collection now

        swipeRefreshLayout.setOnRefreshListener {
            cacheMode = ItemManager.MODE_NETWORK
            adapter.setCacheMode(cacheMode)
            refresh()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MaterialisticDatabase.getInstance(context).liveData.observe(viewLifecycleOwner, observer)

        val managerClassName = arguments?.getString(EXTRA_ITEM_MANAGER)
        val itemManager: ItemManager = when (managerClassName) {
            AlgoliaClient::class.java.name -> algoliaItemManager
            AlgoliaPopularClient::class.java.name -> popularItemManager
            else -> hnItemManager
        }

        adapter.setHotThresHold(AppUtils.HOT_THRESHOLD_NORMAL)
        if (itemManager === hnItemManager && filter != null) {
            when (filter) {
                ItemManager.BEST_FETCH_MODE -> adapter.setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH)
                ItemManager.NEW_FETCH_MODE -> adapter.setHotThresHold(AppUtils.HOT_THRESHOLD_LOW)
            }
        } else if (itemManager === popularItemManager) {
            adapter.setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH)
        }

        adapter.initDisplayOptions(mRecyclerView)
        adapter.setCacheMode(cacheMode)
        adapter.setUpdateListener { showAll, itemCount, actionClickListener ->
            if (showAll) {
                Snackbar.make(
                    mRecyclerView,
                    resources.getQuantityString(
                        R.plurals.new_stories_count,
                        itemCount, itemCount
                    ),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.show_me, actionClickListener)
                    .show()
            } else {
                Snackbar.make(
                    mRecyclerView,
                    resources.getQuantityString(
                        R.plurals.showing_new_stories,
                        itemCount, itemCount
                    ),
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.show_all, actionClickListener)
                    .show()
            }
        }

        storyListViewModel = ViewModelProvider(this)[StoryListViewModel::class.java]
        storyListViewModel.inject(itemManager)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                storyListViewModel.getStories(filter, cacheMode).collect { state ->
                    swipeRefreshLayout.isRefreshing = state.isLoading

                    if (state.error != null) {
                        if (adapter.items == null || adapter.items.size() == 0) {
                             errorView.visibility = View.VISIBLE
                             mRecyclerView.visibility = View.INVISIBLE
                             emptyView.visibility = View.GONE
                        } else {
                             Toast.makeText(context, getString(R.string.connection_error), Toast.LENGTH_SHORT).show()
                        }
                    } else if (!state.isLoading) {
                        // Content loaded
                        onItemsLoaded(state.current)

                        // Handle empty state explicitly if needed, but onItemsLoaded does it.
                        // Ideally we check state.current.isEmpty() here too.
                    }

                    // Always update if we have content, even if loading/error (for last updated)
                    if (state.current.isNotEmpty()) {
                        onItemsLoaded(state.current)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_FILTER, filter)
        outState.putInt(STATE_CACHE_MODE, cacheMode)
    }

    override fun onDetach() {
        preferenceObservable.unsubscribe(activity)
        refreshCallback = null
        super.onDetach()
    }

    fun filter(filter: String) {
        this.filter = filter
        adapter.setHighlightUpdated(false)
        swipeRefreshLayout.isRefreshing = true
        refresh()
    }

    private fun onPreferenceChanged(key: Int, contextChanged: Boolean) {
        if (!contextChanged) {
            adapter.initDisplayOptions(mRecyclerView)
        }
    }

    private fun refresh() {
        adapter.setShowAll(true)
        storyListViewModel.refreshStories(filter, cacheMode)
    }

    private fun onItemsLoaded(items: List<Item>) {
        if (!isAdded) return

        adapter.setItems(items.toTypedArray())

        if (items.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            mRecyclerView.visibility = View.INVISIBLE
        } else {
            emptyView.visibility = View.GONE
            mRecyclerView.visibility = View.VISIBLE
        }

        errorView.visibility = View.GONE
        // Note: swipeRefreshLayout.isRefreshing is handled by flow collector now

        refreshCallback?.onRefreshed()
    }
}
