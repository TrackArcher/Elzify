package com.elzify.music.subsonic.api.navidrome

import androidx.annotation.Keep

@Keep
data class NavidromeCredentials(
    val username: String,
    val password: String
)

@Keep
data class NavidromeLoginResponse(
    val id: String?,
    val name: String?,
    val token: String?,
    val subsonicSalt: String?,
    val subsonicToken: String?
)

@Keep
data class NavidromeArtist(
    val id: String,
    val name: String?,
    val albumCount: Int?,
    val playCount: Long?,
    val playDate: String?,
    val songCount: Int?,
    val size: Long?,
    val coverArtId: String?,
    val starred: Boolean?,
    val starredAt: String?,
    val orderArtistName: String?
)

@Keep
data class NavidromeSong(
    val id: String,
    val title: String?,
    val album: String?,
    val artist: String?,
    val albumId: String?,
    val artistId: String?,
    val albumArtist: String?,
    val albumArtistId: String?,
    val duration: Double?,
    val bitRate: Int?,
    val size: Long?,
    val suffix: String?,
    val contentType: String?,
    val path: String?,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val playCount: Long?,
    val playDate: String?,
    val coverArtId: String?,
    val starred: Boolean?,
    val starredAt: String?,
    val rating: Int?
)
