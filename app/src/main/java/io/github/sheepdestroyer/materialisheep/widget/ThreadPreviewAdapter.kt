package io.github.sheepdestroyer.materialisheep.widget

import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.sheepdestroyer.materialisheep.AlertDialogBuilder
import io.github.sheepdestroyer.materialisheep.AppUtils
import io.github.sheepdestroyer.materialisheep.ItemActivity
import io.github.sheepdestroyer.materialisheep.R
import io.github.sheepdestroyer.materialisheep.accounts.UserServices
import io.github.sheepdestroyer.materialisheep.data.Item
import io.github.sheepdestroyer.materialisheep.data.ItemManager

class ThreadPreviewAdapter(
    itemManager: ItemManager,
    userServices: UserServices,
    popupMenu: PopupMenu,
    alertDialogBuilder: AlertDialogBuilder<*>,
    private val targetAuthor: String?
) : ItemRecyclerViewAdapter<SubmissionViewHolder>(itemManager) {

    private val items = mutableListOf<Item>()
    private var levelIndicatorWidth: Int = 0

    init {
        this.mUserServices = userServices
        this.mPopupMenu = popupMenu
        this.mAlertDialogBuilder = alertDialogBuilder
    }

    fun submitList(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // For simplicity with this small list. DiffUtil could be used but maybe overkill for static thread preview.
    }

    override fun getItem(position: Int): Item? {
        return if (position in items.indices) items[position] else null
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = mLayoutInflater.inflate(R.layout.item_submission, parent, false)
        val holder = SubmissionViewHolder(view)

        // Default margin is handled in bind now to allow recycling
        holder.mCommentButton.visibility = View.GONE

        // Initialize level indicator width if not set
        if (levelIndicatorWidth == 0) {
            levelIndicatorWidth = AppUtils.getDimensionInDp(mContext, R.dimen.level_indicator_width)
        }

        return holder
    }

    override fun bind(holder: SubmissionViewHolder, item: Item) {
        super.bind(holder, item)

        // Handle Indentation (Level)
        // In the original code, viewType was position, and margin was width * viewType.
        // So indentation corresponds to position in the list.
        val position = holder.bindingAdapterPosition
        val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
        params.leftMargin = levelIndicatorWidth * position
        holder.itemView.layoutParams = params

        // Bind Data
        holder.mPostedTextView.text = item.getDisplayedTime(mContext)
        holder.mPostedTextView.append(
            item.getDisplayedAuthor(
                mContext,
                !TextUtils.equals(item.by, targetAuthor),
                0
            )
        )
        holder.mMoreButton.visibility = View.GONE

        if (TextUtils.equals(item.type, Item.COMMENT_TYPE)) {
            holder.mTitleTextView.text = null
            holder.itemView.setOnClickListener(null)
            holder.mCommentButton.visibility = View.GONE
        } else {
            holder.mTitleTextView.text = item.displayedTitle
            holder.mCommentButton.visibility = View.VISIBLE
            holder.mCommentButton.setOnClickListener { openItem(item) }
        }

        val hasTitle = (holder.mTitleTextView.length() > 0)
        holder.mTitleTextView.visibility = if (hasTitle) View.VISIBLE else View.GONE

        val hasContent = (holder.mContentTextView.length() > 0)
        holder.mContentTextView.visibility = if (hasContent) View.VISIBLE else View.GONE
    }

    private fun openItem(item: Item) {
        mContext.startActivity(
            Intent(mContext, ItemActivity::class.java)
                .putExtra(ItemActivity.EXTRA_ITEM, item)
        )
    }
}
