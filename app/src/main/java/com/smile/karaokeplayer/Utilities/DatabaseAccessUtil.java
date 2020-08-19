package com.smile.karaokeplayer.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;

import java.util.ArrayList;

public final class DatabaseAccessUtil {

    private static final String TAG = "DatabaseAccessUtil";

    private DatabaseAccessUtil() {}
    public static ArrayList<SongInfo> readPublicSongList(Context callingContext) {
        ArrayList<SongInfo> playlist;
        SongListSQLite songListSQLite = new SongListSQLite(callingContext);
        if (songListSQLite != null) {
            playlist = songListSQLite.readPlaylist();
            songListSQLite.closeDatabase();
            songListSQLite = null;
        } else {
            playlist = new ArrayList<>();
        }

        return playlist;
    }
}
