package exoplayer

import android.os.Bundle
import android.util.Log
import com.smile.karaokeplayer.SongPlayerActivity
import exoplayer.fragments.ExoPlayerFragment

private const val TAG = "ExoSongPlayerActivity"

class ExoSongPlayerActivity : SongPlayerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
    }

    override fun getFragment() = ExoPlayerFragment()
}