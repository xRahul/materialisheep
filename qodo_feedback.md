## PR Reviewer Guide 🔍

#### (Review updated until commit https://github.com/sheepdestroyer/materialisheep/commit/33c516489344a4e81a3b428985825ceb5244ac96)


Here are some key observations to aid the review process:

<table>
<tr><td>⏱️&nbsp;<strong>Estimated effort to review</strong>: 4 🔵🔵🔵🔵⚪</td></tr>
<tr><td>🧪&nbsp;<strong>No relevant tests</strong></td></tr>
<tr><td>🔒&nbsp;<strong>No security concerns identified</strong></td></tr>
<tr><td>⚡&nbsp;<strong>Recommended focus areas for review</strong><br><br>

<details><summary><a href='https://github.com/sheepdestroyer/materialisheep/pull/56/files#diff-cd1bd6c16056eabdbb05b9b46cd4f05e46572722c428ea2fa7bb3250beeb6545R521-R546'><strong>Redundant setup</strong></a>

The ViewPager2 setup appears duplicated (adapter/offscreenPageLimit set twice) which can hide ordering bugs and cause unnecessary work. Consolidate initialization to a single, clearly ordered setup block to avoid subtle state issues (e.g., callbacks firing twice, page limit being reset unexpectedly).
</summary>

```java
mAdapter = new ItemPagerAdapter(this,
        new ItemPagerAdapter.Builder()
                .setItem(story)
                .setShowArticle(hasText || !mExternalBrowser)
                .setCacheMode(getIntent().getIntExtra(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT))
                .setRetainInstance(true)
                .setDefaultViewMode(mStoryViewMode));
mViewPager.setAdapter(mAdapter);
mViewPager.setOffscreenPageLimit(2);
mViewPager.setAdapter(mAdapter);
mViewPager.setOffscreenPageLimit(2);
mTabLayoutMediator = new TabLayoutMediator(mTabLayout, mViewPager,
        (tab, position) -> tab.setText(mAdapter.getPageTitle(position)));
mTabLayoutMediator.attach();
mPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        toggleFabs(position == 0, mNavButton, mReplyButton);
        Fragment fragment = getFragment(position);
        if (fragment instanceof LazyLoadFragment) {
            ((LazyLoadFragment) fragment).loadNow();
        }
    }
};
mViewPager.registerOnPageChangeCallback(mPageChangeCallback);
```

</details>

<details><summary><a href='https://github.com/sheepdestroyer/materialisheep/pull/56/files#diff-541bfd1c727ef903fd6bb20ddb37eb4d1dbd94a80a601db8b7804f8b61d358c2R589-R625'><strong>Fragment lifecycle risk</strong></a>

unbindViewPager() removes fragments by constructing tags and committing with allowingStateLoss. Tag assumptions and state-loss commits can lead to inconsistent fragment state (especially around rotations/process death), and manually removing fragments managed by FragmentStateAdapter can conflict with ViewPager2’s own FragmentManager usage. Validate this behavior across configuration changes and consider safer teardown (e.g., nulling adapter + relying on FragmentStateAdapter lifecycle, or committing only when state is not saved).
</summary>

```java
@SuppressLint("RestrictedApi")
private void unbindViewPager() {
    if (mViewPager == null) {
        return;
    }
    if (mTabLayoutMediator != null) {
        mTabLayoutMediator.detach();
        mTabLayoutMediator = null;
    }

    if (mPageChangeCallback != null) {
        mViewPager.unregisterOnPageChangeCallback(mPageChangeCallback);
        mPageChangeCallback = null;
    }
    if (mTabSelectedListener != null) {
        mTabLayout.removeOnTabSelectedListener(mTabSelectedListener);
        mTabSelectedListener = null;
    }

    // Remove fragments by Tag ("f" + id). IDs are 0 and 1.
    RecyclerView.Adapter<?> adapter = mViewPager.getAdapter();
    if (adapter instanceof FragmentStateAdapter) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        int itemCount = adapter.getItemCount();
        for (int position = 0; position < itemCount; position++) {
            long itemId = ((FragmentStateAdapter) adapter).getItemId(position);
            String tag = "f" + itemId; // ViewPager2 tag format
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                transaction.remove(fragment);
            }
        }
        transaction.commitAllowingStateLoss();
    }
}
```

</details>

<details><summary><a href='https://github.com/sheepdestroyer/materialisheep/pull/56/files#diff-cd1bd6c16056eabdbb05b9b46cd4f05e46572722c428ea2fa7bb3250beeb6545R599-R613'><strong>Fragment lookup</strong></a>

getFragment() relies on ViewPager2 FragmentStateAdapter tag format ("f" + itemId). This is an internal convention and may break if stable IDs change or if adapter overrides itemId/containsItem semantics. Consider a more robust approach (e.g., keeping references via FragmentManager callbacks, or ensuring stable IDs + containsItem are correctly implemented if IDs ever stop matching positions).
</summary>

```java
/**
 * Retrieves the fragment at the specified position.
 *
 * @param position The position of the fragment.
 * @return The fragment, or null.
 */
private Fragment getFragment(int position) {
    // Tag format for FragmentStateAdapter is "f" + itemId (default itemId is
    // position)
    if (mAdapter == null) {
        return null;
    }
    long itemId = mAdapter.getItemId(position);
    return getSupportFragmentManager().findFragmentByTag("f" + itemId);
}
```

</details>

</td></tr>
</table>

