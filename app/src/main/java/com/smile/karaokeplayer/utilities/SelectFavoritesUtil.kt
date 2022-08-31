package com.smile.karaokeplayer.utilities

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.smile.karaokeplayer.constants.CommonConstants
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.models.SongInfo
import com.smile.karaokeplayer.models.SongListSQLite

object SelectFavoritesUtil {

    /*
    @JvmStatic
    fun selectSongsActivityLauncher(fragment: Fragment, updateFavorite: UpdateFavorites)
    : ActivityResultLauncher<Intent> {
        val intent = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
                ActivityResultCallback { result: ActivityResult? ->
                    if (result == null) return@ActivityResultCallback
                    if (result.resultCode == Activity.RESULT_OK) {
                        result.data?.let {
                            fragment.context?.let {contextIt ->
                                val songListSQLite = SongListSQLite(contextIt)
                                addSongsToFavoriteList(it, songListSQLite)
                                songListSQLite.closeDatabase()
                                updateFavorite.updateFavoriteList()
                            }
                        }
                    }
                })
        return intent
    }
    */

    @JvmStatic
    fun addDataToFavoriteList(data: Intent, songListSQLite: SongListSQLite) {
        val uris: ArrayList<Uri>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableArrayListExtra(PlayerConstants.Uri_List, Uri::class.java)
        } else data.getParcelableArrayListExtra(PlayerConstants.Uri_List)
        uris?.let {
            addUrisToFavoriteList(it, songListSQLite)
        }
    }

    @JvmStatic
    fun addUrisToFavoriteList(uris: ArrayList<Uri>, songListSQLite: SongListSQLite) {
        if (uris.size > 0) {
            // There are files selected
            var song: SongInfo
            var uriString: String
            for (i in uris.indices) {
                val uri = uris[i]
                if (Uri.EMPTY != uri) {
                    uriString = uri.toString()
                    // check if this file is already in database
                    if (songListSQLite.findOneSongByUriString(uriString) == null) {
                        // not exist
                        song = SongInfo()
                        song.songName = ""
                        song.filePath = uriString
                        song.musicTrackNo = 1 // guess
                        song.musicChannel = CommonConstants.StereoChannel // guess
                        song.vocalTrackNo = 2 // guess
                        song.vocalChannel = CommonConstants.StereoChannel // guess
                        song.included = "1" // guess
                        songListSQLite.addSongToSongList(song)
                    }
                }
            }
        }
    }
}