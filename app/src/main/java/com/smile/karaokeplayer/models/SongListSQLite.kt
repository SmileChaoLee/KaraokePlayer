package com.smile.karaokeplayer.models

import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
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
        Log.d(TAG, "onUpgrade() is called.")
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
            Log.e(TAG, "Exception in onUpgrade().")
            ex.printStackTrace()
        }
    }

    private fun openScoreDatabase() {
        songDatabase?.let {
            // already opened
            return
        }
        try {
            songDatabase = writableDatabase
        } catch (ex: SQLException) {
            Log.e(TAG, "Open database exception.")
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
            cur?.run {
                val index = getColumnIndex(columnName)
                if (index != -1) {
                    // exist
                    isExist = true
                }
                close()
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        return isExist
    }

    private fun getSongInfoFromCursor(cur: Cursor?): ArrayList<SongInfo> {
        val songList = ArrayList<SongInfo>()
        cur?.run {
            try {
                if (moveToFirst()) {
                    do {
                        val id = getInt(0)
                        val songName = getString(1)
                        val filePath = getString(2)
                        val musicTrackNo = getInt(3)
                        val musicChannel = getInt(4)
                        val vocalTrackNo = getInt(5)
                        val vocalChannel = getInt(6)
                        val included = getString(7)
                        val songInfo = SongInfo(id, songName, filePath, musicTrackNo, musicChannel,
                                vocalTrackNo, vocalChannel, included)
                        songList.add(songInfo)
                    } while (moveToNext())
                }
                close()
            } catch (ex: SQLException) {
                Log.e(TAG, "getSongInfoFromCursor() exception.")
                ex.printStackTrace()
            }
        }
        return songList
    }

    fun recordsOfPlayList() : Long {
        openScoreDatabase()
        songDatabase?.let {
            return DatabaseUtils.queryNumEntries(it, tableName)
        }
        return 0
    }
    fun readPlayList(): ArrayList<SongInfo> {
        Log.d(TAG, "readSongList() is called.")
        return readPlaylist(false)
    }

    fun readPlaylist(isIncluded: Boolean): ArrayList<SongInfo> {
        Log.d(TAG, "readPlaylist().isIncluded = $isIncluded")
        var playlist = ArrayList<SongInfo>()
        openScoreDatabase()
        songDatabase?.let {
            try {
                // String sql = "select * from " + tableName + " order by " + _id + " asc";
                // Cursor cur = songDatabase.rawQuery(sql, new String[]{});
                var selectionArgs: Array<String>? = null
                var selection: String? = null
                if (isIncluded) {
                    selectionArgs = arrayOf("1")
                    selection = "$included = ?"
                }
                val cur = it.query(tableName, null, selection, selectionArgs,
                        null, null,"$mId asc")
                playlist = getSongInfoFromCursor(cur)
            } catch (ex: SQLException) {
                Log.e(TAG, "readPlaylist() exception.")
                ex.printStackTrace()
            }
        }
        return playlist
    }

    fun addSongToSongList(songInfo: SongInfo?): Long {
        var result: Long = -1
        if (songInfo == null) {
            return result
        }
        openScoreDatabase()
        if (findOneSongByUriString(songInfo.filePath) != null ) return result   // already in database
        val contentValues = getContentValues(songInfo, createAction)
        songDatabase?.let {
            try {
                result = it.insert(tableName, null, contentValues)
            } catch (ex: SQLException) {
                Log.e(TAG, "addSongToSongList() exception.")
                ex.printStackTrace()
            }
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
        songDatabase?.let {
            try {
                result = it.update(tableName, contentValues, whereClause, null).toLong()
            } catch (ex: SQLException) {
                Log.e(TAG, "updateOneSongFromSongList() exception.")
                ex.printStackTrace()
            }
        }
        return result
    }

    fun deleteOneSongFromSongList(songInfo: SongInfo?): Long {
        var result: Long = -1
        if (songInfo == null) {
            return result
        }
        openScoreDatabase()
        songDatabase?.let {
            try {
                val id = songInfo.id
                val whereClause = "$mId = $id"
                result = it.delete(tableName, whereClause, null).toLong()
            } catch (ex: SQLException) {
                Log.e(TAG, "deleteOneSongFromSongList() exception.")
                ex.printStackTrace()
            }
        }
        return result
    }

    fun deleteAllSongList() {
        openScoreDatabase()
        songDatabase?.let {
            try {
                it.delete(tableName, null, null)
            } catch (ex: SQLException) {
                Log.e(TAG, "deleteAllSongList() exception.")
                ex.printStackTrace()
            }
        }
    }

    fun findOneSongByUriString(uriString: String?): SongInfo? {
        if (uriString == null || uriString.isEmpty()) {
            return null
        }
        openScoreDatabase()
        var songInfo: SongInfo? = null
        songDatabase?.let {
            try {
                val whereClause = "$filePath = \"$uriString\""
                val cur = it.query(tableName, null, whereClause, null, null, null, null)
                cur?.run {
                    if (moveToFirst()) {
                        val id = getInt(0)
                        val songName = getString(1)
                        val filePath = getString(2)
                        val musicTrackNo = getInt(3)
                        val musicChannel = getInt(4)
                        val vocalTrackNo = getInt(5)
                        val vocalChannel = getInt(6)
                        val included = getString(7)
                        songInfo = SongInfo(id, songName, filePath, musicTrackNo,
                                musicChannel, vocalTrackNo, vocalChannel, included)
                    }
                    close()
                }
            } catch (ex: SQLException) {
                Log.e(TAG, "findOneSongByContentUri() exception.")
                ex.printStackTrace()
            }
        }
        return songInfo
    }

    fun closeDatabase() {
        songDatabase?.let {
            try {
                it.close()
            } catch (ex: SQLException) {
                Log.e(TAG, "closeDatabase() exception.")
                ex.printStackTrace()
            }
        }
        songDatabase = null
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