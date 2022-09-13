package com.smile.karaokeplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.smile.karaokeplayer.constants.CommonConstants
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.fragments.OpenFileFragment
import com.smile.karaokeplayer.interfaces.PlaySongs
import com.smile.karaokeplayer.models.SongInfo

private const val TAG: String = "OpenFileActivity"

class OpenFileActivity : AppCompatActivity(), PlaySongs {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_file)

        val isPlayButton = intent.getBooleanExtra(CommonConstants.IsButtonForPlay, true)
        Log.d(TAG, "onCreate.isPlayButton = $isPlayButton")
        val openFragment = OpenFileFragment().also {
            val args = Bundle().apply {
                putBoolean(CommonConstants.IsButtonForPlay, isPlayButton)
            }
            it.arguments = args
        }
        supportFragmentManager.beginTransaction().apply {
            add(R.id.openFileConstraintLayout, openFragment)
            commit()
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressedDispatcher.handleOnBackPressed")
                setResult(RESULT_CANCELED)
                finish()
            }
        })
    }

    // implementing interface PlaySongs
    override fun playSelectedSongList(songs: ArrayList<SongInfo>) {
        Log.d(TAG, "playUriList.songs.size = ${songs.size}")
        setResult(RESULT_OK, Intent().putExtra(PlayerConstants.SongListState, songs))
        finish()
    }
    // Finishes implementing interface PlaySongs
}