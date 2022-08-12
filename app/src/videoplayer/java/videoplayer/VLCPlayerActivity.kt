package videoplayer

import android.os.Bundle
import android.util.Log
import com.smile.karaokeplayer.BaseActivity
import videoplayer.fragments.VLCPlayerFragment

private const val TAG : String = "VLCPlayerActivity"

class VLCPlayerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
    }

    override fun getFragment() = VLCPlayerFragment()
}