package com.smile.karaokeplayer.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.adapters.MyListRecyclerViewAdapter
import com.smile.karaokeplayer.models.SongInfo
import com.smile.karaokeplayer.utilities.DatabaseAccessUtil
import com.smile.smilelibraries.utilities.ScreenUtil

private const val TAG : String = "MyListFragment"

class MyListFragment : Fragment(), MyListRecyclerViewAdapter.OnRecyclerItemClickListener {
    interface PlayMyList {
        fun playSongList(songs: ArrayList<SongInfo>)
    }
    private var fragmentView : View? = null
    private var textFontSize = 0f
    private lateinit var playMyList: PlayMyList
    private lateinit var myListRecyclerView : RecyclerView
    private lateinit var myRecyclerViewAdapter : MyListRecyclerViewAdapter
    private lateinit var songInfoList : ArrayList<SongInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
        Log.d(TAG, "onCreate() is called")

        playMyList = (activity as PlayMyList)
        Log.d(TAG, "onCreate.playMyList = $playMyList")

        songInfoList = ArrayList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView() is called")
        fragmentView = inflater.inflate(R.layout.fragment_my_list, container, false)

        val defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(activity,
                BaseApplication.FontSize_Scale_Type, null)
        textFontSize = ScreenUtil.suitableFontSize(activity, defaultTextFontSize,
                BaseApplication.FontSize_Scale_Type,0.0f)

        val buttonWidth = (textFontSize*1.5f).toInt()

        fragmentView?.let {
            myListRecyclerView = it.findViewById(R.id.myListRecyclerView)
            val selectAllButton: ImageButton = it.findViewById(R.id.myListSelectAllButton)
            var layoutParams: ViewGroup.MarginLayoutParams = selectAllButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            selectAllButton.setOnClickListener {
                for (i in 0 until songInfoList.size) {
                    songInfoList[i].run {
                        included = "1"
                        myRecyclerViewAdapter.notifyItemChanged(i)
                    }
                }
            }
            val unselectButton: ImageButton = it.findViewById(R.id.myListUnselectButton)
            layoutParams = unselectButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            unselectButton.setOnClickListener {
                for (i in 0 until songInfoList.size) {
                    songInfoList[i].run {
                        included = "0"
                        myRecyclerViewAdapter.notifyItemChanged(i)
                    }
                }
            }
            val refreshButton: ImageButton = it.findViewById(R.id.myListRefreshButton)
            layoutParams = refreshButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            refreshButton.setOnClickListener {
                searchCurrentFolder()
            }
            val playSelectedButton: ImageButton = it.findViewById(R.id.myListPlaySelectedButton)
            layoutParams = playSelectedButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            playSelectedButton.setOnClickListener {
                // open the files to play
                val songs = ArrayList<SongInfo>().also { uriIt ->
                    for (i in 0 until songInfoList.size) {
                        songInfoList[i].run {
                            if (included == "1") {
                                uriIt.add(this)
                            }
                        }
                    }
                }
                if (songs.size == 0) {
                    ScreenUtil.showToast(
                            activity, getString(R.string.noFilesSelectedString), textFontSize,
                            BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                } else {
                    playMyList.playSongList(songs)
                }
            }
        }

        initFilesRecyclerView()
        searchCurrentFolder()

        return fragmentView
    }

    override fun onRecyclerItemClick(v: View?, position: Int) {
        Log.d(TAG, "onRecyclerItemClick.position = $position")
        ScreenUtil.showToast(
                activity, songInfoList[position].songName, textFontSize,
                BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
        songInfoList[position].apply {
            included = if (included == "1") "0" else "1"
            myRecyclerViewAdapter.notifyItemChanged(position)
        }
    }

    private fun searchCurrentFolder() {
        Log.d(TAG, "searchCurrentFolder() is called")
        val listSize = songInfoList.size
        songInfoList.clear()
        myRecyclerViewAdapter.notifyItemRangeRemoved(0, listSize)
        // get the all list
        DatabaseAccessUtil.readSavedSongList(activity, false)?.let {
            for (element in it) {
                element.included = "0"
                songInfoList.add(element)
            }
        }
        myRecyclerViewAdapter.notifyItemRangeInserted(0, songInfoList.size)
    }

    private fun initFilesRecyclerView() {
        Log.d(TAG, "initFilesRecyclerView() is called")
        activity?.let {
            myRecyclerViewAdapter = MyListRecyclerViewAdapter(
                    it, this, textFontSize, songInfoList)
            myListRecyclerView.adapter = myRecyclerViewAdapter
            myListRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }
}