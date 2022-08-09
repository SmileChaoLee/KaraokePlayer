package exoplayer

import android.os.Bundle
import android.util.Log
import com.smile.karaokeplayer.BaseActivity

private const val TAG : String = "ExoPlayerActivity"

class ExoPlayerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
    }

    override fun getFragment() = ExoPlayerFragment.newInstance("1", "2")
}