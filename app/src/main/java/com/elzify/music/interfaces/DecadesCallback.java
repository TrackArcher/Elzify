package com.elzify.music.interfaces;

import androidx.annotation.Keep;

@Keep
public interface DecadesCallback {
    default void onLoadYear(int year) {}
}
