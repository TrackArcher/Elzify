package com.elzify.music.subsonic.api.open;

import android.util.Log;

import com.elzify.music.subsonic.RetrofitClient;
import com.elzify.music.subsonic.Subsonic;
import com.elzify.music.subsonic.base.ApiResponse;

import retrofit2.Call;

public class OpenClient {
    private static final String TAG = "OpenClient";

    private final Subsonic subsonic;
    private final OpenService openService;

    public OpenClient(Subsonic subsonic) {
        this.subsonic = subsonic;
        this.openService = new RetrofitClient(subsonic).getRetrofit().create(OpenService.class);
    }

    public Call<ApiResponse> getLyricsBySongId(String id) {
        Log.d(TAG, "getLyricsBySongId()");
        return openService.getLyricsBySongId(subsonic.getParams(), id);
    }

    public Call<ApiResponse> getRecentlyPlayed(int count) {
        Log.d(TAG, "getRecentlyPlayed()");
        return openService.getRecentlyPlayed(subsonic.getParams(), count);
    }
}
