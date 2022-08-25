package com.smile.karaokeplayer.utilities;

import android.content.Context;

import com.smile.karaokeplayer.models.SongInfo;
import com.smile.karaokeplayer.models.SongListSQLite;

import java.util.ArrayList;

public final class DatabaseAccessUtil {

    private static final String TAG = "DatabaseAccessUtil";

    private DatabaseAccessUtil() {}
    public static ArrayList<SongInfo> readSavedSongList(Context callingContext, boolean isIncluded) {
        ArrayList<SongInfo> playlist;
        SongListSQLite songListSQLite = new SongListSQLite(callingContext);
        if (songListSQLite != null) {
            playlist = songListSQLite.readPlaylist(isIncluded);
            songListSQLite.closeDatabase();
            songListSQLite = null;
        } else {
            playlist = new ArrayList<>();
        }

        return playlist;
    }
}
