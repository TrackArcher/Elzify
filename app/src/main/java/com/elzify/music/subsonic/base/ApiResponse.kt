package com.elzify.music.subsonic.base

import androidx.annotation.Keep
import com.elzify.music.subsonic.models.SubsonicResponse
import com.google.gson.annotations.SerializedName

@Keep
class ApiResponse {
    @SerializedName("subsonic-response")
    lateinit var subsonicResponse: SubsonicResponse
}