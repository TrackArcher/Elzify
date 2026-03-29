package com.elzify.music.lastfm.api

import com.elzify.music.lastfm.LastFmRetrofitClient
import com.elzify.music.lastfm.models.LastFmTrackResponse
import retrofit2.Call

class TrackClient {
    private val trackService: TrackService =
        LastFmRetrofitClient().retrofit.create(TrackService::class.java)

    fun getTrackInfo(artist: String, track: String, username: String, apiKey: String): Call<LastFmTrackResponse> {
        return trackService.getTrackInfo(artist = artist, track = track, username = username, apiKey = apiKey)
    }
}
