package exoplayer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastState
import com.smile.karaokeplayer.PlayerBaseViewFragment
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.presenters.BasePlayerPresenter
import exoplayer.presenters.ExoPlayerPresenter
import exoplayer.presenters.ExoPlayerPresenter.ExoPlayerPresentView

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val TAG: String = "ExoPlayerFragment"

/**
 * A simple [Fragment] subclass.
 * Use the [ExoPlayerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExoPlayerFragment : PlayerBaseViewFragment(), ExoPlayerPresentView {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var presenter: ExoPlayerPresenter
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var playerView: StyledPlayerView
    private lateinit var mediaRouteButton: MediaRouteButton
    private var castPlayer: CastPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        presenter = ExoPlayerPresenter(this, this)

        super.onCreate(savedInstanceState)  // must be after ExoPlayerPresenter(this, this)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        // must be after super.onCreate(savedInstanceState)
        presenter.initExoPlayerAndCastPlayer() // must be before volumeSeekBar settings
        presenter.initMediaSessionCompat()
        exoPlayer = presenter.exoPlayer
        castPlayer = presenter.castPlayer

        Log.d(TAG, "onCreate() is finished")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return super.onCreateView(inflater, container, savedInstanceState)
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
            playerView = StyledPlayerView(it.applicationContext)
            playerView.layoutParams = layoutParams
            playerView.setBackgroundColor(ContextCompat.getColor(it.applicationContext, android.R.color.black))
            playerViewLinearLayout.addView(playerView)

            playerView.visibility = View.VISIBLE
            playerView.player = exoPlayer
            playerView.useArtwork = true
            playerView.useController = false
            playerView.requestFocus()
        }
        val currentProgress = presenter.currentProgressForVolumeSeekBar
        volumeSeekBar.setProgressAndThumb(currentProgress)
        presenter.playTheSongThatWasPlayedBeforeActivityCreated()

        // mPresenter.addBaseCastStateListener();   // moved to onResume() on 2021-03-26
        castPlayer?.let {
            Log.d(TAG, "castPlayer != null && exoPlayer != null")
            presenter.currentPlayer =
                if (it.isCastSessionAvailable) castPlayer else exoPlayer
        }

        Log.d(TAG, "onViewCreated() is finished.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() is called.")
        presenter.setSessionAvailabilityListener()
        presenter.addBaseCastStateListener()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() is called.")
        presenter.releaseSessionAvailabilityListener()
        presenter.removeBaseCastStateListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() is called.")
        presenter.releaseMediaSessionCompat()
        presenter.releaseExoPlayerAndCastPlayer()
        playerView.player = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ExoPlayerFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ExoPlayerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    // implementing methods of ExoPlayerPresenter.ExoPlayerPresentView
    override fun setCurrentPlayerToPlayerView() {
        val currentPlayer = presenter.currentPlayer ?: return
        if (currentPlayer === exoPlayer) {
            Log.d(TAG, "Current player is exoPlayer.")
        } else  /* currentPlayer == castPlayer */ {
            Log.d(TAG, "Current player is castPlayer.")
        }
    }
    // end of implementing methods of ExoPlayerPresenter.ExoPlayerPresentView

    // implement abstract methods of super class
    override fun getPlayerBasePresenter(): BasePlayerPresenter {
        return presenter
    }

    override fun setMediaRouteButtonView(buttonMarginLeft: Int, imageButtonHeight: Int) {
        // MediaRouteButton View
        mediaRouteButton = fragmentView.findViewById(R.id.media_route_button)
        setMediaRouteButtonVisible(presenter.currentCastState != CastState.NO_DEVICES_AVAILABLE)
        activity?.applicationContext?.let { CastButtonFactory.setUpMediaRouteButton(it, mediaRouteButton) }

        val layoutParams: MarginLayoutParams = mediaRouteButton.layoutParams as MarginLayoutParams
        layoutParams.setMargins(buttonMarginLeft, 0, 0, 0)
        val mediaRouteButtonBitmap = BitmapFactory.decodeResource(resources, R.drawable.cast)
        val mediaRouteButtonDrawable: Drawable = BitmapDrawable(
            resources,
            Bitmap.createScaledBitmap(
                mediaRouteButtonBitmap,
                imageButtonHeight,
                imageButtonHeight,
                true
            )
        )
        mediaRouteButton.setRemoteIndicatorDrawable(mediaRouteButtonDrawable)
    }

    override fun setMediaRouteButtonVisible(isVisible: Boolean) {
        mediaRouteButton.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun createIntentForSongListActivity(): Intent {
        return Intent(activity, SongListActivity::class.java)
    }

    override fun setMenuItemsVisibility() {
        // do nothing
    }

    override fun setSwitchToVocalImageButtonVisibility() {
        // do nothing
    }
    // end of implementing methods of super class
}