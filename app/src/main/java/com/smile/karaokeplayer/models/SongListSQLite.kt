package com.smile.karaokeplayer.models

import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.util.Log
import java.lang.Exception
import java.util.ArrayList

class SongListSQLite(myContext: Context) : SQLiteOpenHelper(
    myContext, dbName, null, dbVersion) {
    private var songDatabase: SQLiteDatabase?
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(createTable)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // database.execSQL("DROP TABLE IF EXISTS " + tableName);
        // onCreate(database);
        Log.d("TAG", "onUpgrade() is called.")
        try {
            val oldTableName = "playList"
            var sqlString = "ALTER TABLE $oldTableName RENAME TO $tableName"
            database.execSQL(sqlString)
            if (isColumnExist(database, included)) {
                Log.d(TAG, "included exists")
            } else {
                Log.d(TAG, "included does not exist")
                sqlString =
                    "ALTER TABLE $tableName ADD COLUMN $included TEXT DEFAULT '1' NOT NULL "
                database.execSQL(sqlString)
            }
        } catch (ex: Exception) {
            Log.d("TAG", "Exception in onUpgrade().")
            ex.printStackTrace()
        }
    }

    private fun openScoreDatabase() {
        if (songDatabase != null) {
            // already opened
            return
        }
        try {
            songDatabase = writableDatabase
        } catch (ex: SQLException) {
            Log.d("TAG", "Open database exception.")
            ex.printStackTrace()
        }
    }

    private fun getContentValues(songInfo: SongInfo?, crudAction: Int): ContentValues? {
        if (songInfo == null) {
            return null
        }
        val contentValues = ContentValues()
        if (crudAction != createAction) {
            contentValues.put(mId, songInfo.id)
        }
        contentValues.put(songName, songInfo.songName)
        contentValues.put(filePath, songInfo.filePath)
        contentValues.put(musicTrackNo, songInfo.musicTrackNo)
        contentValues.put(musicChannel, songInfo.musicChannel)
        contentValues.put(vocalTrackNo, songInfo.vocalTrackNo)
        contentValues.put(vocalChannel, songInfo.vocalChannel)
        contentValues.put(included, songInfo.included)
        return contentValues
    }

    private fun isColumnExist(database: SQLiteDatabase?, columnName: String): Boolean {
        if (database == null) {
            return false
        }
        var isExist = false
        try {
            // get only one record
            val cur = database.query(tableName, null, null, null, null, null, null, "1")
            if (cur != null) {
                val index = cur.getColumnIndex(columnName)
                if (index != -1) {
                    // exist
                    isExist = true
                }
                cur.close()
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        return isExist
    }

    private fun getSongInfoFromCursor(cur: Cursor?): ArrayList<SongInfo> {
        val songList = ArrayList<SongInfo>()
        if (cur != null) {
            try {
                if (cur.moveToFirst()) {
                    do {
                        val id = cur.getInt(0)
                        val songName = cur.getString(1)
                        val filePath = cur.getString(2)
                        val musicTrackNo = cur.getInt(3)
                        val musicChannel = cur.getInt(4)
                        val vocalTrackNo = cur.getInt(5)
                        val vocalChannel = cur.getInt(6)
                        val included = cur.getString(7)
                        val songInfo = SongInfo(
                            id,
                            songName,
                            filePath,
                            musicTrackNo,
                            musicChannel,
                            vocalTrackNo,
                            vocalChannel,
                            included
                        )
                        songList.add(songInfo)
                    } while (cur.moveToNext())
                }
                cur.close()
            } catch (ex: SQLException) {
                Log.d("TAG", "getSongInfoFromCursor() exception.")
                ex.printStackTrace()
            }
        }
        return songList
    }

    fun readSongList(): ArrayList<SongInfo> {
        Log.d("TAG", "readSongList() is called.")
        var songList = ArrayList<SongInfo>()
        openScoreDatabase()
        if (songDatabase != null) {
            try {
                // String sql = "select * from " + tableName + " order by " + _id + " asc";
                // Cursor cur = songDatabase.rawQuery(sql, new String[]{});
                val cur =
                    songDatabase!!.query(tableName, null, null, null, null, null, "$mId asc")
                songList = getSongInfoFromCursor(cur)
            } catch (ex: SQLException) {
                Log.d("TAG", "readSongList() exception.")
                ex.printStackTrace()
            }
            // closeDatabase();
        }
        return songList
    }

    fun readPlaylist(): ArrayList<SongInfo> {
        Log.d("TAG", "readPlaylist() is called.")
        var playlist = ArrayList<SongInfo>()
        openScoreDatabase()
        if (songDatabase != null) {
            try {
                // String sql = "select * from " + tableName + " order by " + _id + " asc";
                // Cursor cur = songDatabase.rawQuery(sql, new String[]{});
                val cur = songDatabase!!.query(
                    tableName,
                    null,
                    "$included = ?",
                    arrayOf("1"),
                    null,
                    null,
                    "$mId asc"
                )
                playlist = getSongInfoFromCursor(cur)
            } catch (ex: SQLException) {
                Log.d("TAG", "readPlaylist() exception.")
                ex.printStackTrace()
            }
            // closeDatabase();
        }
        return playlist
    }

    fun addSongToSongList(songInfo: SongInfo?): Long {
        var result: Long = -1
        if (songInfo == null) {
            return result
        }
        val contentValues = getContentValues(songInfo, createAction)
        openScoreDatabase()
        if (songDatabase != null) {
            try {
                result = songDatabase!!.insert(tableName, null, contentValues)
            } catch (ex: SQLException) {
                Log.d("TAG", "addSongToSongList() exception.")
                ex.printStackTrace()
            }
            // closeDatabase();
        }
        return result
    }

    fun updateOneSongFromSongList(songInfo: SongInfo?): Long {
        var result: Long = -1
        if (songInfo == null) {
            return result
        }
        val contentValues = getContentValues(songInfo, updateAction)
        val whereClause = mId + " = " + songInfo.id
        openScoreDatabase()
        if (songDatabase != null) {
            try {
                result = songDatabase!!.update(tableName, contentValues, whereClause, null).toLong()
            } catch (ex: SQLException) {
                Log.d("TAG", "updateOneSongFromSongList() exception.")
                ex.printStackTrace()
            }
            // closeDatabase();
        }
        return result
    }

    fun deleteOneSongFromSongList(songInfo: SongInfo?): Long {
        var result: Long = -1
        if (songInfo == null) {
            return result
        }
        openScoreDatabase()
        if (songDatabase != null) {
            try {
                val id = songInfo.id
                val whereClause = "$mId = $id"
                result = songDatabase!!.delete(tableName, whereClause, null).toLong()
            } catch (ex: SQLException) {
                Log.d("TAG", "deleteOneSongFromSongList() exception.")
                ex.printStackTrace()
            }
            // closeDatabase();
        }
        return result
    }

    fun deleteAllSongList() {
        openScoreDatabase()
        if (songDatabase != null) {
            try {
                songDatabase!!.delete(tableName, null, null)
            } catch (ex: SQLException) {
                Log.d("TAG", "deleteAllSongList() exception.")
                ex.printStackTrace()
            }
            // closeDatabase();
        }
    }

    fun findOneSongByUriString(uriString: String?): SongInfo? {
        var songInfo: SongInfo? = null
        if (uriString == null || uriString.isEmpty()) {
            return songInfo
        }
        openScoreDatabase()
        if (songDatabase != null) {
            try {
                val whereClause = "$filePath = \"$uriString\""
                val cur = songDatabase!!.query(tableName, null, whereClause, null, null, null, null)
                if (cur != null) {
                    if (cur.moveToFirst()) {
                        val id = cur.getInt(0)
                        val songName = cur.getString(1)
                        val filePath = cur.getString(2)
                        val musicTrackNo = cur.getInt(3)
                        val musicChannel = cur.getInt(4)
                        val vocalTrackNo = cur.getInt(5)
                        val vocalChannel = cur.getInt(6)
                        val included = cur.getString(7)
                        songInfo = SongInfo(
                            id,
                            songName,
                            filePath,
                            musicTrackNo,
                            musicChannel,
                            vocalTrackNo,
                            vocalChannel,
                            included
                        )
                    }
                    cur.close()
                }
            } catch (ex: SQLException) {
                Log.d("TAG", "findOneSongByContentUri() exception.")
                ex.printStackTrace()
            }
            // closeDatabase();
        }
        return songInfo
    }

    fun closeDatabase() {
        if (songDatabase != null) {
            try {
                songDatabase!!.close()
                songDatabase = null
            } catch (ex: SQLException) {
                Log.d("TAG", "closeDatabase() exception.")
                ex.printStackTrace()
            }
        }
    }

    companion object {
        private const val TAG = "SongListSQLite"
        private const val mId: String = "id"
        private const val songName: String = "songName"
        private const val filePath: String = "filePath"
        private const val musicTrackNo: String = "musicTrackNo"
        private const val musicChannel: String = "musicChannel"
        private const val vocalTrackNo: String = "vocalTrackNo"
        private const val vocalChannel: String = "vocalChannel"
        private const val included: String = "included"
        private const val dbName: String = "songDatabase.db"
        private const val tableName: String = "songList"
        private const val createTable = ("create table if not exists " + tableName + " ("
                + mId + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + songName + " TEXT NOT NULL , "
                + filePath + " TEXT NOT NULL , "
                + musicTrackNo + " INTEGER , "
                + musicChannel + " INTEGER , "
                + vocalTrackNo + " INTEGER , "
                + vocalChannel + " INTEGER , "
                + included + " TEXT NOT NULL "
                + ");")
        private const val dbVersion = 4
        private const val createAction = 0
        private const val readAction = 1
        private const val updateAction = 2
        private const val deleteAction = 3
    }

    init {
        // the following statements is to create or update the database
        songDatabase = writableDatabase
        // closeDatabase();
    }
}