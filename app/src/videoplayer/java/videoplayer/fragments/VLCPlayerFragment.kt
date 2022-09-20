package videoplayer.fragments

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.fragments.PlayerBaseViewFragment
import com.smile.karaokeplayer.presenters.BasePlayerPresenter
import org.videolan.libvlc.util.VLCVideoLayout
import videoplayer.Presenters.VLCPlayerPresenter

private const val TAG: String = "VLCPlayerFragment"

class VLCPlayerFragment : PlayerBaseViewFragment() {
    private val enableSubtitles = true
    private val useTextureView = false
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

        Log.d(TAG, "onViewCreated() is finished.")
    }

    override fun onStart() {
        Log.d(TAG, "onStart() is called.")
        super.onStart()
        videoVLCPlayerView.requestFocus()
        presenter.attachPlayerViews(videoVLCPlayerView, null, enableSubtitles, useTextureView)
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

    override fun setMenuItemsVisibility() {
        val channelMenuItem = mainMenu.findItem(R.id.channel)
        channelMenuItem.isVisible = false
    }

    override fun setSwitchToVocalImageButtonVisibility() {
        switchToVocalImageButton.visibility = View.GONE
    }
    // end of implementing methods of super class
}