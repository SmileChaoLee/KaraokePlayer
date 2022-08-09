package exoplayer

import android.os.Bundle
import android.view.Menu
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.smile.karaokeplayer.BaseActivity
import com.smile.karaokeplayer.R

private const val TAG : String = "ExoPlayerActivity"

class ExoPlayerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed() is called")
        val exoPlayerFragment = playerFragment as ExoPlayerFragment
        exoPlayerFragment.onBackPressed()
    }

    override fun getFragment() = ExoPlayerFragment.newInstance("1", "2")
}