package io.github.sheepdestroyer.materialisheep;

import androidx.lifecycle.ViewModel;

public class LazyLoadViewModel extends ViewModel {
    private boolean mEagerLoad;
    private boolean mLoaded;
    private boolean mInitialized;

    /**
     * Checks if the fragment should eagerly load data.
     * 
     * @return true if eager loading is enabled
     */
    public boolean isEagerLoad() {
        return mEagerLoad;
    }

    public void setEagerLoad(boolean eagerLoad) {
        mEagerLoad = eagerLoad;
    }

    /**
     * Checks if the data has been loaded.
     * 
     * @return true if data is loaded
     */
    public boolean isLoaded() {
        return mLoaded;
    }

    public void setLoaded(boolean loaded) {
        mLoaded = loaded;
    }

    /**
     * Checks if the ViewModel has been initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    public void setInitialized(boolean initialized) {
        mInitialized = initialized;
    }

}
