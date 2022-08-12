package videoplayer.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.fragments.PlayerBaseViewFragment
import com.smile.karaokeplayer.presenters.BasePlayerPresenter
import com.smile.smilelibraries.utilities.ScreenUtil
import org.videolan.libvlc.util.VLCVideoLayout
import videoplayer.Presenters.VLCPlayerPresenter
import videoplayer.SongListActivity

private const val TAG: String = "ExoPlayerFragment"
class VLCPlayerFragment : PlayerBaseViewFragment() {
    private val ENABLE_SUBTITLES = true
    private val USE_TEXTURE_VIEW = false

    private val PERMISSION_REQUEST_CODE = 0x11
    private var hasPermissionForExternalStorage = false

    private lateinit var presenter: VLCPlayerPresenter
    private lateinit var videoVLCPlayerView: VLCVideoLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        presenter = VLCPlayerPresenter(this, this)

        super.onCreate(savedInstanceState)  // must be after ExoPlayerPresenter(this, this)
        arguments?.let {
        }

        // must be after super.onCreate(savedInstanceState)
        // must be before volumeSeekBar settings
        presenter.initVLCPlayer() // must be before volumeSeekBar settings
        presenter.initMediaSessionCompat()

        Log.d(TAG, "onCreate() is finished")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated() is called.")
        super.onViewCreated(view, savedInstanceState)

        // Video player view
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.gravity = Gravity.CENTER
        activity?.let {
            val context = it.applicationContext
            videoVLCPlayerView = VLCVideoLayout(context)
            videoVLCPlayerView.layoutParams = layoutParams
            videoVLCPlayerView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
            playerViewLinearLayout.addView(videoVLCPlayerView)
            videoVLCPlayerView.visibility = View.VISIBLE
        }

        val currentProgress = presenter.currentProgressForVolumeSeekBar
        volumeSeekBar.setProgressAndThumb(currentProgress)

        presenter.playTheSongThatWasPlayedBeforeActivityCreated()

        activity?.let {
            hasPermissionForExternalStorage =
                (ActivityCompat.checkSelfPermission(it.applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!hasPermissionForExternalStorage) {
                    val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ActivityCompat.requestPermissions(it, permissions, PERMISSION_REQUEST_CODE)
                }
            } else {
                if (!hasPermissionForExternalStorage) {
                    ScreenUtil.showToast(it, "Permission Denied", 60f,
                        ScreenUtil.FontSize_Pixel_Type,
                        Toast.LENGTH_LONG)
                    it.finish()
                }
            }
        }

        Log.d(TAG, "onViewCreated() is finished.")
    }

    override fun onStart() {
        Log.d(TAG, "onStart() is called.")
        super.onStart()
        videoVLCPlayerView.requestFocus()
        presenter.attachPlayerViews(videoVLCPlayerView, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW)
    }

    override fun onStop() {
        Log.d(TAG, "onStop() is called.")
        super.onStop()
        presenter.detachPlayerViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() is called.")
        presenter.releaseMediaSessionCompat()
        presenter.releaseVLCPlayer()
    }

    // implement abstract methods of super class
    override fun getPlayerBasePresenter(): BasePlayerPresenter {
        return presenter
    }

    override fun setMediaRouteButtonView(buttonMarginLeft: Int, imageButtonHeight: Int) {}

    override fun setMediaRouteButtonVisible(isVisible: Boolean) {}

    override fun createIntentForSongListActivity(): Intent {
        return Intent(activity, SongListActivity::class.java)
    }

    override fun setMenuItemsVisibility() {
        val channelMenuItem = mainMenu.findItem(R.id.channel)
        channelMenuItem.isVisible = false
    }

    override fun setSwitchToVocalImageButtonVisibility() {
        switchToVocalImageButton.visibility = View.GONE
    }
    // end of implementing methods of super class
}