package com.smile.karaokeplayer.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.fragment.app.Fragment;

import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;

import java.util.ArrayList;

public final class DataOrContentAccessUtil {
    private DataOrContentAccessUtil() {}

    public static void selectFileToOpen(Activity activity, int requestCode) {
        Intent intent = createIntentForSelectingFile();
        activity.startActivityForResult(intent, requestCode);
    }

    public static void selectFileToOpen(Fragment fragment, int requestCode) {
        Intent intent = createIntentForSelectingFile();
        fragment.startActivityForResult(intent, requestCode);
    }

    public static Intent createIntentForSelectingFile() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.setType("*/*");

        return intent;
    }

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
