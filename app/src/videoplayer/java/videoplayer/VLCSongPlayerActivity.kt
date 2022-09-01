package videoplayer

import android.os.Bundle
import android.util.Log
import com.smile.karaokeplayer.SongPlayerActivity
import videoplayer.fragments.VLCPlayerFragment

private const val TAG = "VLCSongPlayerActivity"

class VLCSongPlayerActivity : SongPlayerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
    }

    override fun getFragment() = VLCPlayerFragment()
}