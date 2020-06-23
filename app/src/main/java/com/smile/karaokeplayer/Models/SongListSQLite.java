package com.smile.karaokeplayer.Models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by lee on 09/13/2019.
 */
public class SongListSQLite extends SQLiteOpenHelper {

    private static final String TAG = "SongListSQLite";

    private static final String _id = new String("id");
    private static final String songName = new String("songName");
    private static final String filePath = new String("filePath");
    private static final String musicTrackNo = new String("musicTrackNo");
    private static final String musicChannel = new String("musicChannel");
    private static final String vocalTrackNo = new String("vocalTrackNo");
    private static final String vocalChannel = new String("vocalChannel");
    private static final String included = new String("included");

    private static final String dbName = new String("songDatabase.db");
    private static final String tableName = new String("songList");
    private static final String createTable = "create table if not exists " + tableName + " ("
            + _id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + songName + " TEXT NOT NULL , "
            + filePath + " TEXT NOT NULL , "
            + musicTrackNo + " INTEGER , "
            + musicChannel + " INTEGER , "
            + vocalTrackNo + " INTEGER , "
            + vocalChannel + " INTEGER , "
            + included + " TEXT NOT NULL "
            + ");";

    private static final int dbVersion = 4;

    private static final int createAction = 0;
    private static final int readAction = 1;
    private static final int updateAction = 2;
    private static final int deleteAction = 3;

    private Context myContext;
    private SQLiteDatabase songDatabase;

    public SongListSQLite(Context context) {
        super(context, dbName,null,dbVersion);
        myContext = context;
        // the following statements is to create or update the database
        songDatabase = getWritableDatabase();
        closeDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database , int oldVersion , int newVersion) {
        // database.execSQL("DROP TABLE IF EXISTS " + tableName);
        // onCreate(database);

        Log.d("TAG", "onUpgrade() is called.");

        try {
            String oldTableName = "playList";
            String sqlString = "ALTER TABLE " + oldTableName + " RENAME TO " + tableName;
            database.execSQL(sqlString);

            if (isColumnExist(database, included)) {
                Log.d(TAG, "included exists");
            } else {
                Log.d(TAG, "included does not exist");

                sqlString = "ALTER TABLE " + tableName + " ADD COLUMN " + included + " TEXT DEFAULT '1' NOT NULL ";
                database.execSQL(sqlString);
            }
        } catch (Exception ex) {
            Log.d("TAG", "Exception in onUpgrade().");
            ex.printStackTrace();
        }
    }

    private void openScoreDatabase() {

        if (songDatabase != null) {
            // already opened
            return;
        }
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
        contentValues.put(included, songInfo.getIncluded());

        return contentValues;
    }

    private boolean isColumnExist(SQLiteDatabase database, String columnName) {
        boolean isExist = false;
        if (database != null) {
            try {
                // get only one record
                Cursor cur = database.query(tableName, null, null, null, null, null, null, "1");
                if (cur != null) {
                    int index = cur.getColumnIndex(columnName);
                    if (index != -1) {
                        // exist
                        isExist = true;
                    }
                }
                cur.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return isExist;
    }

    private ArrayList<SongInfo> getSongInfoFromCursor(Cursor cur) {
        ArrayList<SongInfo> songList = new ArrayList<>();
        if (cur != null) {
            try {
                if (cur.moveToFirst()) {
                    do {
                        Integer id = cur.getInt(0);
                        String songName = cur.getString(1);
                        String filePath = cur.getString(2);
                        int musicTrackNo = cur.getInt(3);
                        int musicChannel = cur.getInt(4);
                        int vocalTrackNo = cur.getInt(5);
                        int vocalChannel = cur.getInt(6);
                        String included = cur.getString(7);
                        SongInfo songInfo = new SongInfo(id, songName, filePath, musicTrackNo, musicChannel, vocalTrackNo, vocalChannel, included);
                        songList.add(songInfo);
                    } while (cur.moveToNext());
                }
                cur.close();
            } catch (SQLException ex) {
                Log.d("TAG", "getSongInfoFromCursor() exception.");
                ex.printStackTrace();
            }
        }

        return songList;
    }

    public ArrayList<SongInfo> readSongList() {

        Log.d("TAG", "readSongList() is called.");

        ArrayList<SongInfo> songList = new ArrayList<>();

        openScoreDatabase();
        if (songDatabase != null) {
            try {
                // String sql = "select * from " + tableName + " order by " + _id + " asc";
                // Cursor cur = songDatabase.rawQuery(sql, new String[]{});
                Cursor cur = songDatabase.query(tableName, null, null, null, null, null, _id+" asc");
                songList = getSongInfoFromCursor(cur);
            } catch (SQLException ex) {
                Log.d("TAG", "readSongList() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }

        return songList;
    }

    public ArrayList<SongInfo> readPlaylist() {

        Log.d("TAG", "readPlaylist() is called.");

        ArrayList<SongInfo> playlist = new ArrayList<>();

        openScoreDatabase();
        if (songDatabase != null) {
            try {
                // String sql = "select * from " + tableName + " order by " + _id + " asc";
                // Cursor cur = songDatabase.rawQuery(sql, new String[]{});
                Cursor cur = songDatabase.query(tableName, null, included + " = ?", new String[] {"1"}, null, null, _id+" asc");
                playlist = getSongInfoFromCursor(cur);
            } catch (SQLException ex) {
                Log.d("TAG", "readPlaylist() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }

        return playlist;
    }

    public long addSongToSongList(final SongInfo songInfo) {

        long result = -1;

        if (songInfo == null) {
            return result;
        }

        ContentValues contentValues = getContentValues(songInfo, createAction);
        openScoreDatabase();
        if (songDatabase != null) {
            try {
                result = songDatabase.insert(tableName, null, contentValues);
            } catch (SQLException ex) {
                Log.d("TAG", "addSongToSongList() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }

        return result;
    }

    public long updateOneSongFromSongList(final SongInfo songInfo) {

        long result = -1;

        if (songInfo == null) {
            return result;
        }

        ContentValues contentValues = getContentValues(songInfo, updateAction);
        String whereClause = _id + " = " + songInfo.getId();
        openScoreDatabase();
        if (songDatabase != null) {
            try {
                result = songDatabase.update(tableName, contentValues, whereClause, null);
            } catch (SQLException ex) {
                Log.d("TAG", "updateOneSongFromSongList() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }

        return result;
    }

    public long deleteOneSongFromSongList(final SongInfo songInfo) {

        long result = -1;

        if (songInfo == null) {
            return result;
        }

        openScoreDatabase();
        if (songDatabase != null) {
            try {
                int id = songInfo.getId();
                String whereClause = _id + " = " + id;
                result = songDatabase.delete(tableName, whereClause,null);
            } catch (SQLException ex) {
                Log.d("TAG", "deleteOneSongFromSongList() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }

        return result;
    }

    public void deleteAllSongList() {
            openScoreDatabase();
            if (songDatabase != null) {
                try {
                    songDatabase.delete(tableName, null,null);
                } catch (SQLException ex) {
                    Log.d("TAG", "deleteAllSongList() exception.");
                    ex.printStackTrace();
                }
                closeDatabase();
            }
    }

    public SongInfo findOneSongByContentUri(Uri contentUri) {
        SongInfo songInfo = null;
        String songFilePath = "";
        if (contentUri == null) {
            return songInfo;
        }
        songFilePath = contentUri.toString();

        openScoreDatabase();
        if (songDatabase != null) {
            try {
                String whereClause = filePath + " = " + '"' + songFilePath + '"';
                Cursor cur = songDatabase.query(tableName, null, whereClause, null, null, null, null);
                if (cur != null) {
                    if (cur.moveToFirst()) {
                        Integer id = cur.getInt(0);
                        String songName = cur.getString(1);
                        String filePath = cur.getString(2);
                        int musicTrackNo = cur.getInt(3);
                        int musicChannel = cur.getInt(4);
                        int vocalTrackNo = cur.getInt(5);
                        int vocalChannel = cur.getInt(6);
                        String included = cur.getString(7);
                        songInfo = new SongInfo(id, songName, filePath, musicTrackNo, musicChannel, vocalTrackNo, vocalChannel, included);
                    }
                    cur.close();
                }
            } catch (SQLException ex) {
                Log.d("TAG", "findOneSongByContentUri() exception.");
                ex.printStackTrace();
            }
            closeDatabase();
        }

        return songInfo;
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
