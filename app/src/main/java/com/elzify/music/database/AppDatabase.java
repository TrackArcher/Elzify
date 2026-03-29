package com.elzify.music.database;

import androidx.media3.common.util.UnstableApi;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.elzify.music.App;
import com.elzify.music.database.converter.DateConverters;
import com.elzify.music.database.dao.ChronologyDao;
import com.elzify.music.database.dao.DownloadDao;
import com.elzify.music.database.dao.FavoriteDao;
import com.elzify.music.database.dao.LyricsDao;
import com.elzify.music.database.dao.PlaylistDao;
import com.elzify.music.database.dao.QueueDao;
import com.elzify.music.database.dao.RecentSearchDao;
import com.elzify.music.database.dao.ScrobbleDao;
import com.elzify.music.database.dao.ServerDao;
import com.elzify.music.database.dao.SessionMediaItemDao;
import com.elzify.music.model.Chronology;
import com.elzify.music.model.Download;
import com.elzify.music.model.Favorite;
import com.elzify.music.model.LyricsCache;
import com.elzify.music.model.Queue;
import com.elzify.music.model.RecentSearch;
import com.elzify.music.model.Scrobble;
import com.elzify.music.model.Server;
import com.elzify.music.model.SessionMediaItem;
import com.elzify.music.subsonic.models.Playlist;

@UnstableApi
@Database(
        version = 16,
        entities = {Queue.class, Server.class, RecentSearch.class, Download.class, Chronology.class, Favorite.class, SessionMediaItem.class, Playlist.class, LyricsCache.class, Scrobble.class},
        autoMigrations = {@AutoMigration(from = 10, to = 11), @AutoMigration(from = 11, to = 12)}
)
@TypeConverters({DateConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    private final static String DB_NAME = "tempo_db";
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance() {
        if (instance == null) {
            instance = Room.databaseBuilder(App.getContext(), AppDatabase.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return instance;
    }

    public abstract QueueDao queueDao();

    public abstract ServerDao serverDao();

    public abstract RecentSearchDao recentSearchDao();

    public abstract DownloadDao downloadDao();

    public abstract ChronologyDao chronologyDao();

    public abstract FavoriteDao favoriteDao();

    public abstract SessionMediaItemDao sessionMediaItemDao();

    public abstract PlaylistDao playlistDao();

    public abstract LyricsDao lyricsDao();

    public abstract ScrobbleDao scrobbleDao();
}
