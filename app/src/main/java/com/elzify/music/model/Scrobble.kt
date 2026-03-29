package com.cappielloantonio.tempo.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scrobble")
data class Scrobble(
    @PrimaryKey(autoGenerate = true)
    val dbId: Long = 0,
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "submission")
    val submission: Boolean,
    @ColumnInfo(name = "server")
    val server: String
)
