package com.smile.karaokeplayer.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.OpenFileActivity
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.adapters.FavoriteRecyclerViewAdapter
import com.smile.karaokeplayer.constants.CommonConstants
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.interfaces.PlaySongs
import com.smile.karaokeplayer.models.SongInfo
import com.smile.karaokeplayer.models.SongListSQLite
import com.smile.karaokeplayer.utilities.DatabaseAccessUtil
import com.smile.karaokeplayer.utilities.SelectFavoritesUtil
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "MyFavoritesFragment"

class MyFavoritesFragment : Fragment(), FavoriteRecyclerViewAdapter.OnRecyclerItemClickListener {
    interface PlayMyFavorites {
        fun intentForFavoriteListActivity():Intent
    }
    private lateinit var fragmentView: View
    private var textFontSize = 0f
    private var fontScale = 0f
    private lateinit var playSongs: PlaySongs
    private lateinit var playMyFavorites: PlayMyFavorites
    private lateinit var myListRecyclerView : RecyclerView
    private lateinit var myRecyclerViewAdapter : FavoriteRecyclerViewAdapter
    private lateinit var favoriteList : ArrayList<SongInfo>
    private lateinit var editSongsActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectSongsActivityLauncher: ActivityResultLauncher<Intent>

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

        playSongs = (activity as PlaySongs)
        Log.d(TAG, "onCreate.playSongs = $playSongs")
        playMyFavorites = (activity as PlayMyFavorites)
        Log.d(TAG, "onCreate.playMyFavorites = $playMyFavorites")

        favoriteList = ArrayList()

        editSongsActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
        ) { searchFavorites() } // update the UI }
        selectSongsActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
                ActivityResultCallback { result: ActivityResult? ->
                    if (result == null) return@ActivityResultCallback
                    if (result.resultCode == Activity.RESULT_OK) {
                        result.data?.let {
                            context?.let {contextIt ->
                                val songListSQLite = SongListSQLite(contextIt)
                                SelectFavoritesUtil.addDataToFavoriteList(it, songListSQLite)
                                songListSQLite.closeDatabase()
                                searchFavorites() // update the UI
                            }
                        }
                    }
                })
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

        fragmentView = view

        fragmentView.let {
            val buttonWidth = (textFontSize * 1.5f).toInt()
            myListRecyclerView = it.findViewById(R.id.myListRecyclerView)
            val selectAllButton: ImageButton = it.findViewById(R.id.favoriteSelectAllButton)
            var layoutParams: ViewGroup.MarginLayoutParams = selectAllButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            selectAllButton.setOnClickListener {
                for (i in 0 until favoriteList.size) {
                    favoriteList[i].run {
                        included = "1"
                        myRecyclerViewAdapter.notifyItemChanged(i)
                    }
                }
            }
            val unselectButton: ImageButton = it.findViewById(R.id.favoriteUnselectButton)
            layoutParams = unselectButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            unselectButton.setOnClickListener {
                for (i in 0 until favoriteList.size) {
                    favoriteList[i].run {
                        included = "0"
                        myRecyclerViewAdapter.notifyItemChanged(i)
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
                    for (i in 0 until favoriteList.size) {
                        favoriteList[i].run {
                            if (included == "1") {
                                songIt.add(this)
                            }
                        }
                    }
                }
                if (songs.size == 0) {
                    ScreenUtil.showToast(
                            activity, getString(R.string.noFilesSelectedString), textFontSize,
                            BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                } else {
                    playSongs.playSelectedSongList(songs)
                }
            }
            val editButton: ImageButton = it.findViewById(R.id.favoriteEditButton)
            layoutParams = editButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            editButton.setOnClickListener {
                val favoriteIntent = playMyFavorites.intentForFavoriteListActivity()
                ArrayList<SongInfo>().apply {
                    for (element in favoriteList) {
                        if (element.included == "1") add(element)
                    }
                    if (size > 0) {
                        favoriteIntent.putExtra(PlayerConstants.MyFavoriteListState, this)
                        editSongsActivityLauncher.launch(favoriteIntent)
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
                activity?.let { activityIt ->
                    Intent(activityIt, OpenFileActivity::class.java).apply {
                        putExtra(CommonConstants.IsButtonForPlay, false)
                        selectSongsActivityLauncher.launch(this)
                    }
                }
            }
        }

        initFavoriteRecyclerView()
        searchFavorites()
    }

    override fun onStart() {
        Log.d(TAG, "onStart() is called")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume() is called")
        super.onResume()
    }

    override fun onRecyclerItemClick(v: View?, position: Int) {
        Log.d(TAG, "onRecyclerItemClick.position = $position")
        favoriteList[position].apply {
            included = if (included == "1") "0" else "1"
            myRecyclerViewAdapter.notifyItemChanged(position)
        }
    }

    private fun searchFavorites() {
        Log.d(TAG, "searchFavorites() is called")
        favoriteList.clear()
        activity?.let {
            DatabaseAccessUtil.readSavedSongList(it, false)?.let {sqlIt ->
                for (element in sqlIt) {
                    element.apply {
                        included = "0"
                        favoriteList.add(this)
                    }
                }
            }
        }
        myRecyclerViewAdapter.notifyDataSetChanged()
    }

    private fun initFavoriteRecyclerView() {
        Log.d(TAG, "initFavoriteRecyclerView() is called")
        activity?.let {
            myRecyclerViewAdapter = FavoriteRecyclerViewAdapter(
                    it, this, textFontSize, favoriteList)
            myListRecyclerView.adapter = myRecyclerViewAdapter
            myListRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }
}