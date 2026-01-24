package io.github.sheepdestroyer.materialisheep

import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.Window
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sheepdestroyer.materialisheep.accounts.UserServices
import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager
import io.github.sheepdestroyer.materialisheep.databinding.ActivityThreadPreviewBinding
import io.github.sheepdestroyer.materialisheep.widget.CommentItemDecoration
import io.github.sheepdestroyer.materialisheep.widget.PopupMenu
import io.github.sheepdestroyer.materialisheep.widget.SnappyLinearLayoutManager
import io.github.sheepdestroyer.materialisheep.widget.ThreadPreviewAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class ThreadPreviewActivity : ThemedActivity() {

    @Inject
    @Named(DataModule.HN)
    lateinit var itemManager: ItemManager

    @Inject
    lateinit var keyDelegate: KeyDelegate

    @Inject
    lateinit var userServices: UserServices

    @Inject
    lateinit var popupMenu: PopupMenu

    @Inject
    lateinit var alertDialogBuilder: AlertDialogBuilder<androidx.appcompat.app.AlertDialog>

    private lateinit var binding: ActivityThreadPreviewBinding
    private lateinit var viewModel: ThreadPreviewViewModel
    private lateinit var adapter: ThreadPreviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as MaterialisticApplication).applicationComponent.inject(this)

        val item = IntentCompat.getParcelableExtra(intent, EXTRA_ITEM, Item::class.java)
        if (item == null) {
            finish()
            return
        }

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityThreadPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            displayOptions = androidx.appcompat.app.ActionBar.DISPLAY_SHOW_HOME or
                    androidx.appcompat.app.ActionBar.DISPLAY_SHOW_TITLE or
                    androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP
        }

        setupRecyclerView(item)
        setupViewModel(item)
    }

    private fun setupRecyclerView(startItem: Item) {
        adapter = ThreadPreviewAdapter(
            itemManager = itemManager,
            userServices = userServices,
            popupMenu = popupMenu,
            alertDialogBuilder = alertDialogBuilder,
            targetAuthor = startItem.by
        )

        binding.recyclerView.apply {
            layoutManager = SnappyLinearLayoutManager(this@ThreadPreviewActivity, false)
            addItemDecoration(CommentItemDecoration(this@ThreadPreviewActivity))
            this.adapter = this@ThreadPreviewActivity.adapter
        }

        keyDelegate.setScrollable(
            KeyDelegate.RecyclerViewHelper(
                binding.recyclerView,
                KeyDelegate.RecyclerViewHelper.SCROLL_ITEM
            ),
            null
        )
    }

    private fun setupViewModel(startItem: Item) {
        val factory = ThreadPreviewViewModel.Factory(itemManager, Dispatchers.IO)
        viewModel = ViewModelProvider(this, factory)[ThreadPreviewViewModel::class.java]

        viewModel.loadThread(startItem)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submitList(items)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        keyDelegate.attach(this)
    }

    override fun onStop() {
        super.onStop()
        keyDelegate.detach(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return keyDelegate.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        return keyDelegate.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event)
    }

    override fun isDialogTheme(): Boolean {
        return true
    }

    companion object {
        const val EXTRA_ITEM = "io.github.sheepdestroyer.materialisheep.ThreadPreviewActivity.EXTRA_ITEM"
    }
}
