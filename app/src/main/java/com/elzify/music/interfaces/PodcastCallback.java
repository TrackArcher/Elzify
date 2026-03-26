package com.elzify.music.interfaces;

import androidx.annotation.Keep;

@Keep

public interface PodcastCallback {
    default void onDismiss() {}
}
