package com.elzify.music.interfaces;

import androidx.annotation.Keep;

@Keep
public interface PlaylistCallback {
    default void onDismiss() {}
}
