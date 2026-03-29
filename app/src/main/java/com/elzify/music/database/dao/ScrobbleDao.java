package com.cappielloantonio.tempo.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.cappielloantonio.tempo.model.Scrobble;

import java.util.List;

@Dao
public interface ScrobbleDao {
    @Query("SELECT * FROM scrobble WHERE server = :server ORDER BY timestamp ASC")
    List<Scrobble> getPendingScrobbles(String server);

    @Insert
    void insert(Scrobble scrobble);

    @Delete
    void delete(Scrobble scrobble);

    @Query("DELETE FROM scrobble WHERE dbId = :dbId")
    void deleteById(Long dbId);
}
