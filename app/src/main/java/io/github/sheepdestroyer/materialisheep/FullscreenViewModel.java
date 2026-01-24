package io.github.sheepdestroyer.materialisheep;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FullscreenViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isFullscreen = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsFullscreen() {
        return isFullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        isFullscreen.setValue(fullscreen);
    }
}
