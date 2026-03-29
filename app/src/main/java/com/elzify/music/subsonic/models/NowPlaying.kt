package com.elzify.music.subsonic.models

import androidx.annotation.Keep

@Keep
class NowPlaying {
    var entries: List<NowPlayingEntry>? = null
}