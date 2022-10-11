package exoplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.smile.karaokeplayer.BaseActivity
import exoplayer.fragments.ExoPlayerFragment

private const val TAG : String = "ExoPlayerActivity"

class ExoPlayerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
    }

    override fun getFragment() = ExoPlayerFragment()
    override fun comeBackFromFavorite(playData : Bundle?) {
        onReceiveFunc(isSingleSong = false, needPlay = true, intent = null, pData = playData)
    }

    // implementing interface PlayMyFavorites
    override fun intentForFavoriteListActivity(): Intent {
        return Intent(this, FavoriteListActivity::class.java)
    }
    // Finishes implementing interface PlayMyFavorites
}