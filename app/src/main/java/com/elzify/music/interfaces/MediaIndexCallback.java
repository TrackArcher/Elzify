package com.elzify.music.interfaces;

import androidx.annotation.Keep;

@Keep
public interface MediaIndexCallback {
    default void onRecovery(int index) {}
}
