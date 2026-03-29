package com.elzify.music.subsonic.models

import androidx.annotation.Keep

@Keep
class Error {
    var code: Int? = null
    var message: String? = null
}