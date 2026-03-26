package com.elzify.music.interfaces;

import androidx.annotation.Keep;

@Keep
public interface StarCallback {
    default void onError() {}
    default void onSuccess() {}
}
