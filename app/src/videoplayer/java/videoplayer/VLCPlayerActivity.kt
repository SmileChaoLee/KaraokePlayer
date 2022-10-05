package videoplayer

import android.content.Intent
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
    override fun comeBackFromFavorite(playData : Bundle?) {
        onReceiveFunc(false, false, null, playData)
    }

    // implementing interface PlayMyFavorites
    override fun intentForFavoriteListActivity(): Intent {
        return Intent(this, FavoriteListActivity::class.java)
    }
    // Finishes implementing interface PlayMyFavorites
}