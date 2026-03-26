package com.elzify.music.interfaces;

import androidx.annotation.Keep;

@Keep

public interface RadioCallback {
    default void onDismiss() {}
}
