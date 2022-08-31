package com.smile.karaokeplayer.utilities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.smile.karaokeplayer.OpenFileActivity
import com.smile.karaokeplayer.constants.CommonConstants
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.interfaces.UpdateFavorites
import com.smile.karaokeplayer.models.SongInfo
import com.smile.karaokeplayer.models.SongListSQLite

object SelectFavoritesUtil {

    @JvmStatic
    fun selectSongs(activity: FragmentActivity, intentForResult: ActivityResultLauncher<Intent>) {
        val selectFileIntent = Intent(activity, OpenFileActivity::class.java)
        selectFileIntent.putExtra(CommonConstants.IsButtonForPlay, false)
        intentForResult.launch(selectFileIntent)
    }

    @JvmStatic
    fun selectSongsActivityLauncher(fragment: Fragment, updateFavorite: UpdateFavorites)
    : ActivityResultLauncher<Intent> {
        val intent = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
                ActivityResultCallback { result: ActivityResult? ->
                    if (result == null) return@ActivityResultCallback
                    val resultCode = result.resultCode
                    if (resultCode == Activity.RESULT_OK) {
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

    private fun addSongsToFavoriteList(data: Intent, songListSQLite: SongListSQLite) {
        val uris: ArrayList<Uri>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableArrayListExtra(PlayerConstants.Uri_List, Uri::class.java)
        } else data.getParcelableArrayListExtra(PlayerConstants.Uri_List)
        uris?.let {
            if (uris.size > 0) {
                // There are files selected
                var mSongInfo: SongInfo
                var uriString: String
                for (i in uris.indices) {
                    val uri = uris[i]
                    if (Uri.EMPTY != uri) {
                        uriString = uri.toString()
                        // check if this file is already in database
                        if (songListSQLite.findOneSongByUriString(uriString) == null) {
                            // not exist
                            mSongInfo = SongInfo()
                            mSongInfo.songName = ""
                            mSongInfo.filePath = uriString
                            mSongInfo.musicTrackNo = 1 // guess
                            mSongInfo.musicChannel = CommonConstants.StereoChannel // guess
                            mSongInfo.vocalTrackNo = 2 // guess
                            mSongInfo.vocalChannel = CommonConstants.StereoChannel // guess
                            mSongInfo.included = "1" // guess
                            songListSQLite.addSongToSongList(mSongInfo)
                        }
                    }
                }
            }
        }
    }
}