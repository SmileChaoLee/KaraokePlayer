package com.smile.karaokeplayer.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.adapters.FavoriteRecyclerViewAdapter
import com.smile.karaokeplayer.interfaces.PlayMyFavorites
import com.smile.karaokeplayer.interfaces.PlaySongs
import com.smile.karaokeplayer.models.MySingleTon
import com.smile.karaokeplayer.models.SongInfo
import com.smile.karaokeplayer.utilities.DatabaseAccessUtil
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "MyFavoritesFragment"

class MyFavoritesFragment : Fragment(), FavoriteRecyclerViewAdapter.OnRecyclerItemClickListener {

    private var textFontSize = 0f
    private var fontScale = 0f
    private var playSongs: PlaySongs? = null
    private var playMyFavorites: PlayMyFavorites? = null
    private var myListRecyclerView : RecyclerView? = null
    private var myRecyclerViewAdapter : FavoriteRecyclerViewAdapter? = null
    private lateinit var editSongsActivityLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
        arguments?.let {
        }

        val defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(activity,
                BaseApplication.FontSize_Scale_Type, null)
        textFontSize = ScreenUtil.suitableFontSize(activity, defaultTextFontSize,
                BaseApplication.FontSize_Scale_Type,0.0f)
        fontScale = ScreenUtil.suitableFontScale(activity, ScreenUtil.FontSize_Pixel_Type, 0.0f)

        activity?.let {
            if (it is PlaySongs) playSongs = it
            Log.d(TAG, "onCreate.playSongs = $playSongs")
            if (it is PlayMyFavorites) playMyFavorites = it
            Log.d(TAG, "onCreate.playMyFavorites = $playMyFavorites")
        }

        editSongsActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            playMyFavorites?.restorePlayingState()
            searchFavorites()
        } // update the UI }

        Log.d(TAG, "onCreate.FavoriteSingleTon.favoriteList.size = ${MySingleTon.favorites.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView() is called")
        return inflater.inflate(R.layout.fragment_my_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated() is called.")
        super.onViewCreated(view, savedInstanceState)

        view.let {
            val buttonWidth = (textFontSize * 1.5f).toInt()
            myListRecyclerView = it.findViewById(R.id.myListRecyclerView)
            myListRecyclerView?.setHasFixedSize(true)
            val selectAllButton: ImageButton = it.findViewById(R.id.favoriteSelectAllButton)
            var layoutParams: ViewGroup.MarginLayoutParams = selectAllButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            selectAllButton.setOnClickListener {
                for (i in 0 until MySingleTon.favorites.size) {
                    MySingleTon.favorites[i].run {
                        included = "1"
                        myRecyclerViewAdapter?.notifyItemChanged(i)
                    }
                }
            }
            val unselectButton: ImageButton = it.findViewById(R.id.favoriteUnselectButton)
            layoutParams = unselectButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            unselectButton.setOnClickListener {
                for (i in 0 until MySingleTon.favorites.size) {
                    MySingleTon.favorites[i].run {
                        included = "0"
                        myRecyclerViewAdapter?.notifyItemChanged(i)
                    }
                }
            }
            val refreshButton: ImageButton = it.findViewById(R.id.favoriteRefreshButton)
            layoutParams = refreshButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            refreshButton.setOnClickListener {
                searchFavorites()
            }
            val playSelectedButton: ImageButton = it.findViewById(R.id.favoritePlaySelectedButton)
            layoutParams = playSelectedButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            playSelectedButton.setOnClickListener {
                // open the files to play
                val songs = ArrayList<SongInfo>().also { songIt ->
                    var index = 0
                    for (i in 0 until MySingleTon.favorites.size) {
                        if (MySingleTon.favorites[i].included == "1") {
                            songIt.add(MySingleTon.favorites[i])
                            index++
                            if (index >= MySingleTon.maxSongs) {
                                // excess the max
                                ScreenUtil.showToast(
                                        activity, getString(R.string.excess_max) +
                                        " ${MySingleTon.maxSongs}", textFontSize,
                                        BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                                break
                            }
                        }
                    }
                }
                if (songs.size == 0) {
                    ScreenUtil.showToast(
                            activity, getString(R.string.noFilesSelectedString), textFontSize,
                            BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                } else {
                    MySingleTon.orderedSongs.clear()
                    MySingleTon.orderedSongs.addAll(songs)
                    playSongs?.playSelectedSongList()
                }
            }
            val editButton: ImageButton = it.findViewById(R.id.favoriteEditButton)
            layoutParams = editButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            editButton.setOnClickListener {
                ArrayList<SongInfo>().also {listIt ->
                    for (element in MySingleTon.favorites) {
                        if (element.included == "1") listIt.add(element)
                    }
                    if (listIt.size > 0) {
                        playMyFavorites?.let {playIt ->
                            playIt.intentForFavoriteListActivity().apply {
                                Log.d(TAG, "editButton.listIt.size = ${listIt.size}")
                                playIt.onSavePlayingState(component)
                                // putExtra(PlayerConstants.MyFavoriteListState, listIt)
                                MySingleTon.selectedFavorites.clear()
                                MySingleTon.selectedFavorites.addAll(listIt)
                                Runtime.getRuntime().gc()
                                editSongsActivityLauncher.launch(this)
                            }
                        }
                    } else {
                        ScreenUtil.showToast(
                                activity, getString(R.string.noFilesSelectedString), textFontSize,
                                BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                    }
                }
            }
            val addButton: ImageButton = it.findViewById(R.id.favoriteAddButton)
            layoutParams = addButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            addButton.setOnClickListener {
                // Switching to OpenFileFragment
                playMyFavorites?.switchToOpenFileFragment()
            }
        }

        initFavoriteRecyclerView()
    }

    override fun onStart() {
        Log.d(TAG, "onStart() is called")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        searchFavorites()   // has to be in onResume()
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        clearFavoriteList()
        super.onPause()
    }

    override fun onRecyclerItemClick(v: View?, position: Int) {
        Log.d(TAG, "onRecyclerItemClick.position = $position")
        MySingleTon.favorites[position].apply {
            included = if (included == "1") "0" else "1"
            myRecyclerViewAdapter?.notifyItemChanged(position)
        }
    }

    fun clearFavoriteList() {
        MySingleTon.favorites.clear()
        myRecyclerViewAdapter?.notifyDataSetChanged()
    }

    fun searchFavorites() {
        Log.d(TAG, "searchFavorites() is called")
        val tempList: ArrayList<SongInfo> = ArrayList(MySingleTon.maxSongs)
        activity?.let {
            DatabaseAccessUtil.readSavedSongList(it, false)?.also {sqlIt ->
                var index = 0
                for (element in sqlIt) {
                    element.included = "0"
                    tempList.add(element)
                    index++
                    if (index >= MySingleTon.maxSongs) {
                        // excess the max
                        ScreenUtil.showToast(
                                activity, getString(R.string.excess_max) +
                                " ${MySingleTon.maxSongs}", textFontSize,
                                BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                        break
                    }
                }
            }
        }
        MySingleTon.favorites.clear()
        MySingleTon.favorites.addAll(tempList)
        myRecyclerViewAdapter?.notifyDataSetChanged()
    }

    private fun initFavoriteRecyclerView() {
        Log.d(TAG, "initFavoriteRecyclerView() is called")
        activity?.let {
            val yellow = ContextCompat.getColor(it, R.color.yellow)
            val transparentLightGray = ContextCompat.getColor(it, R.color.transparentLightGray)

            myRecyclerViewAdapter = FavoriteRecyclerViewAdapter.getInstance(
                    this, textFontSize, MySingleTon.favorites,
                    yellow, transparentLightGray)

            myListRecyclerView?.adapter = myRecyclerViewAdapter
            myListRecyclerView?.layoutManager = object : LinearLayoutManager(context) {
                override fun isAutoMeasureEnabled(): Boolean {
                    return false
                }
            }
        }
    }
}