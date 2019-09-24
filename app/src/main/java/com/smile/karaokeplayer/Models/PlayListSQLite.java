package com.smile.karaokeplayer.Models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by lee on 09/13/2019.
 */
public class PlayListSQLite extends SQLiteOpenHelper {

    private static final String TAG = new String(".PlayListSQLite");

    private static final String _id = new String("id");
    private static final String songName = new String("songName");
    private static final String filePath = new String("filePath");
    private static final String musicTrackNo = new String("musicTrackNo");
    private static final String musicChannel = new String("musicChannel");
    private static final String vocalTrackNo = new String("vocalTrackNo");
    private static final String vocalChannel = new String("vocalChannel");

    private static final String dbName = new String("songDatabase.db");
    private static final String tableName = new String("playList");
    private static final String createTable = "create table if not exists " + tableName + " ("
            + _id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + songName + " TEXT NOT NULL ,  "
            + filePath + " TEXT NOT NULL ,  "
            + musicTrackNo + " INTEGER , "
            + musicChannel + " INTEGER , "
            + vocalTrackNo + " INTEGER , "
            + vocalChannel + " INTEGER );";

    private static final int dbVersion = 1;

    private static final int createAction = 0;
    private static final int readAction = 1;
    private static final int updateAction = 2;
    private static final int deleteAction = 3;

    private Context myContext;
    private SQLiteDatabase songDatabase;

    public PlayListSQLite(Context context) {
        super(context, dbName,null,dbVersion);
        myContext = context;
        songDatabase = null;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database , int oldVersion , int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + tableName);
        onCreate(database);
    }

    private void openScoreDatabase() {

        try {
            songDatabase = getWritableDatabase();
        } catch (SQLException ex) {
            Log.d("TAG", "Open database exception.");
            ex.printStackTrace();
        }
    }

    private ContentValues getContentValues(SongInfo songInfo, int crudAction) {
        if (songInfo == null) {
            return null;
        }

        ContentValues contentValues = new ContentValues();
        if (crudAction != createAction) {
            contentValues.put(_id, songInfo.getId());
        }
        contentValues.put(songName, songInfo.getSongName());
        contentValues.put(filePath, songInfo.getFilePath());
        contentValues.put(musicTrackNo, songInfo.getMusicTrackNo());
        contentValues.put(musicChannel, songInfo.getMusicChannel());
        contentValues.put(vocalTrackNo, songInfo.getVocalTrackNo());
        contentValues.put(vocalChannel, songInfo.getVocalChannel());

        return contentValues;
    }

    public ArrayList<SongInfo> readPlayList() {

        ArrayList<SongInfo> playList = new ArrayList<>();

        openScoreDatabase();
        if (songDatabase != null) {
            try {
                String sql = "select * from " + tableName;
                Cursor cur = songDatabase.rawQuery(sql, new String[]{});
                if (cur.moveToFirst()) {
                    do {
                        Integer id = cur.getInt(0);
                        String songName = cur.getString(1);
                        String filePath = cur.getString(2);
                        int musicTrackNo = cur.getInt(3);
                        int musicChannel = cur.getInt(4);
                        int vocalTrackNo = cur.getInt(5);
                        int vocalChannel = cur.getInt(6);
                        SongInfo songInfo = new SongInfo(id, songName, filePath, musicTrackNo, musicChannel, vocalTrackNo, vocalChannel);
                        playList.add(songInfo);
                    } while (cur.moveToNext());
                }
                cur.close();
            } catch (SQLException ex) {
                Log.d("TAG", "readPlayList() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }

        return playList;
    }

    public long addSongToPlayList(final SongInfo songInfo) {

        long result = -1;

        if (songInfo == null) {
            return result;
        }

        ContentValues contentValues = getContentValues(songInfo, updateAction);
        String whereClause = _id + " = " + songInfo.getId();
        openScoreDatabase();
        if (songDatabase != null) {
            try {
                result = songDatabase.insert(tableName, null, contentValues);
            } catch (SQLException ex) {
                Log.d("TAG", "addSongToPlayList() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }

        return result;
    }

    public void updateOneSongFromPlayList(SongInfo songInfo) {
        ContentValues contentValues = getContentValues(songInfo, updateAction);
        String whereClause = _id + " = " + songInfo.getId();
        openScoreDatabase();
        if (songDatabase != null) {
            try {
                songDatabase.update(tableName, contentValues, whereClause, null);
            } catch (SQLException ex) {
                Log.d("TAG", "updateOneSongFromPlayList() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }
    }

    public void deleteOneSongFromPlayList(final int id) {

        openScoreDatabase();
        if (songDatabase != null) {
            try {
                String whereClause = _id + " = " + id;
                songDatabase.delete(tableName, whereClause,null);
            } catch (SQLException ex) {
                Log.d("TAG", "deleteOneSongFromPlayList() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }
    }

    public void deleteAllPlayList() {
            openScoreDatabase();
            if (songDatabase != null) {
                try {
                    songDatabase.delete(tableName, null,null);
                } catch (SQLException ex) {
                    Log.d("TAG", "deleteAllPlayList() exception.");
                    ex.printStackTrace();
                }
                closeDatabase();
            }
    }

    public void closeDatabase() {
        if (songDatabase != null) {
            try {
                songDatabase.close();
                songDatabase = null;
            } catch (SQLException ex) {
                Log.d("TAG", "closeDatabase() exception.");
                ex.printStackTrace();
            }
        }
    }
}
