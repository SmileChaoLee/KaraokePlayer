package com.smile.karaokeplayer.utilities

import android.content.Intent
import android.os.Build
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.models.SongInfo
import com.smile.karaokeplayer.models.SongListSQLite

object SelectFavoritesUtil {
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun addDataToFavoriteList(data: Intent, songListSQLite: SongListSQLite) {
        val songs: ArrayList<SongInfo>? = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getSerializableExtra(PlayerConstants.SongListState, ArrayList::class.java)
        } else data.getSerializableExtra(PlayerConstants.SongListState)) as ArrayList<SongInfo>?

        songs?.let {
            addUrisToFavoriteList(it, songListSQLite)
        }
    }

    @JvmStatic
    fun addUrisToFavoriteList(songs: ArrayList<SongInfo>, songListSQLite: SongListSQLite) {
        songs.let {
            for (song in it) {
                if (songListSQLite.findOneSongByUriString(song.filePath) == null) {
                    song.included = "1"
                    songListSQLite.addSongToSongList(song)
                }
            }
        }
    }
}