package com.smile.karaokeplayer.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.adapters.OpenFilesRecyclerViewAdapter
import com.smile.karaokeplayer.constants.CommonConstants
import com.smile.karaokeplayer.interfaces.PlaySongs
import com.smile.karaokeplayer.models.MySingleTon
import com.smile.karaokeplayer.models.FileDesList
import com.smile.karaokeplayer.models.FileDescription
import com.smile.karaokeplayer.models.SongInfo
import com.smile.karaokeplayer.models.SongListSQLite
import com.smile.smilelibraries.utilities.ScreenUtil
import java.io.File

private const val TAG : String = "OpenFileFragment"

class OpenFileFragment : Fragment(), OpenFilesRecyclerViewAdapter.OnRecyclerItemClickListener {

    private lateinit var fragmentView : View
    private var textFontSize = 0f
    private lateinit var playSongs: PlaySongs
    private lateinit var pathTextView: TextView
    private var filesRecyclerView : RecyclerView? = null
    private var myRecyclerViewAdapter : OpenFilesRecyclerViewAdapter? = null
    private var isPlayButton: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate() is called")
        super.onCreate(savedInstanceState)
        arguments?.let {
            isPlayButton = it.getBoolean(CommonConstants.IsButtonForPlay, true)
            Log.d(TAG, "onCreate.isPlayButton = $isPlayButton")
        }

        val defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(activity,
                BaseApplication.FontSize_Scale_Type, null)
        textFontSize = ScreenUtil.suitableFontSize(activity, defaultTextFontSize,
                BaseApplication.FontSize_Scale_Type,0.0f)

        playSongs = (activity as PlaySongs)
        Log.d(TAG, "onCreate.playSongs = $playSongs")

        // FileDesList.currentPath = Environment.getExternalStorageDirectory().toString()
        Log.d(TAG, "onCreate.FileDesList.currentPath = ${FileDesList.currentPath}")

        activity?.applicationContext?.externalCacheDirs?.let {
            Log.d(TAG, "externalCacheDirs = $it, externalCacheDirs.size = ${it.size}")
            FileDesList.rootPathSet.clear()
            for (element in it) {
                Log.d(TAG, "externalCacheDirs.element = $element")
                element?.absolutePath?.let { pathIt ->
                    pathIt.indexOf("/Android/data").let {indexIt ->
                        if (indexIt >= 0) {
                            pathIt.substring(0, indexIt).let {subIt ->
                                Log.d(TAG, "element.substring(0, indexIt) = $subIt")
                                FileDesList.rootPathSet.add(subIt)
                            }
                        }
                    }
                }
            }
        }
        Log.d(TAG, "onCreate.FileDesList.fileList.size = ${FileDesList.fileList.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView() is called")
        return inflater.inflate(R.layout.fragment_open_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentView = view

        val buttonWidth = (textFontSize*1.5f).toInt()
        fragmentView.let {
            filesRecyclerView = it.findViewById(R.id.openFilesRecyclerView)
            filesRecyclerView?.setHasFixedSize(true)
            pathTextView = it.findViewById(R.id.pathTextView)
            ScreenUtil.resizeTextSize(pathTextView, textFontSize, BaseApplication.FontSize_Scale_Type)
            val backKeyButton: ImageButton = it.findViewById(R.id.openFileBackKeyButton)
            var layoutParams: ViewGroup.MarginLayoutParams = backKeyButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            backKeyButton.setOnClickListener {
                if (FileDesList.currentPath == "/") return@setOnClickListener
                FileDesList.currentPath = if (FileDesList.rootPathSet.contains(FileDesList.currentPath)) "/"
                else {
                    val index = FileDesList.currentPath.lastIndexOf('/')
                    if (index >= 0 ) FileDesList.currentPath.substring(0, index) else "/"
                }
                if (FileDesList.currentPath.isEmpty()) FileDesList.currentPath = "/"
                searchCurrentFolder()
            }
            val selectAllButton: ImageButton = it.findViewById(R.id.openFileSelectAllButton)
            layoutParams = selectAllButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            selectAllButton.setOnClickListener {
                for (i in 0 until FileDesList.fileList.size) {
                    FileDesList.fileList[i].run {
                        if (!file.isDirectory && !selected) {
                            selected = true
                            myRecyclerViewAdapter?.notifyItemChanged(i)
                        }
                    }
                }
            }
            val unselectButton: ImageButton = it.findViewById(R.id.openFileUnselectButton)
            layoutParams = unselectButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            unselectButton.setOnClickListener {
                for (i in 0 until FileDesList.fileList.size) {
                    FileDesList.fileList[i].run {
                        if (!file.isDirectory && selected) {
                            selected = false
                            myRecyclerViewAdapter?.notifyItemChanged(i)
                        }
                    }
                }
            }
            val refreshButton: ImageButton = it.findViewById(R.id.openFileRefreshButton)
            layoutParams = refreshButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            refreshButton.setOnClickListener {
                searchCurrentFolder()
            }
            val playSelectedButton: ImageButton = it.findViewById(R.id.openFilePlaySelectedButton)
            layoutParams = playSelectedButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            playSelectedButton.setImageResource(
                    if (isPlayButton) R.drawable.play_media_button_image else R.drawable.open_files)
            playSelectedButton.setOnClickListener {
                // open the files to play
                activity?.let {activityIt ->
                    val songListSQLite = SongListSQLite(activityIt)
                    getSongs(songListSQLite, "playSelectedButton").let { songsIt ->
                        if (songsIt.size == 0) {
                            ScreenUtil.showToast(
                                    activity, getString(R.string.noFilesSelectedString), textFontSize,
                                    BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                        } else {
                            MySingleTon.orderedSongs.clear()
                            MySingleTon.orderedSongs.addAll(songsIt)
                            playSongs.playSelectedSongList()
                        }
                    }
                    songListSQLite.closeDatabase()
                }
            }
            val addToFavoriteButton: ImageButton = it.findViewById(R.id.addToFavoriteButton)
            layoutParams = addToFavoriteButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            addToFavoriteButton.setOnClickListener {
                activity?.let {activityIt ->
                    val songListSQLite = SongListSQLite(activityIt)
                    getSongs(songListSQLite, "addToFavoriteButton").also { songsIt ->
                        var toastMsg = getString(R.string.noFilesSelectedString)
                        if (songsIt.size > 0) {
                            for (song in songsIt) {
                                song.included = "1"
                                val numRecords = songListSQLite.recordsOfPlayList()
                                Log.d(TAG, "addToFavoriteButton.recordsOfPlayList() = $numRecords")
                                if (numRecords < MySingleTon.maxSongs) {
                                    songListSQLite.addSongToSongList(song)
                                } else {
                                    // excess max number of favorites
                                    ScreenUtil.showToast(activity,getString(R.string.excess_max) +
                                            " ${MySingleTon.maxSongs}", textFontSize,
                                            BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                                    break
                                }
                            }
                            toastMsg = getString(R.string.add_to_favorites)
                        }
                        ScreenUtil.showToast(activity, toastMsg, textFontSize,
                                BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                    }
                    songListSQLite.closeDatabase()
                }
            }
        }

        initFilesRecyclerView()
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        searchCurrentFolder()   // has to be in onResume()
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        clearFileList()
        super.onPause()
    }

    override fun onRecyclerItemClick(v: View?, position: Int) {
        Log.d(TAG, "onRecyclerItemClick.position = $position")
        if (position < 0) return
        if (FileDesList.fileList[position].file.isFile) {
            FileDesList.fileList[position].selected = !FileDesList.fileList[position].selected
            myRecyclerViewAdapter?.notifyItemChanged(position)
            return
        }
        FileDesList.currentPath = FileDesList.fileList[position].file.path
        searchCurrentFolder()
    }

    fun clearFileList() {
        FileDesList.fileList.clear()
        myRecyclerViewAdapter?.notifyDataSetChanged()
    }

    fun searchCurrentFolder() {
        Log.d(TAG, "searchCurrentFolder() is called")
        val tempList: ArrayList<FileDescription> = ArrayList(FileDesList.maxFiles)
        FileDesList.currentPath.let {
            var index = 0
            if (it == "/") {
                for (element in FileDesList.rootPathSet) {
                    Log.d(TAG, "searchCurrentFolder.element = $element")
                    FileDescription(File(element), false).apply {
                        Log.d(TAG, "searchCurrentFolder. = ${file.path}, ${file.absolutePath}")
                        tempList.add(this)
                        index++
                        if (index >= FileDesList.maxFiles) {
                            ScreenUtil.showToast(activity,
                                    getString(R.string.excess_max) + " ${FileDesList.maxFiles}",
                                    textFontSize, BaseApplication.FontSize_Scale_Type,
                                    Toast.LENGTH_SHORT)
                            return@let
                        }
                    }
                }
            } else {
                val fList = File(it).listFiles()
                Log.d(TAG, "fList = $fList")
                fList?.also { fIt ->
                    Log.d(TAG, "file.list().size() = ${fIt.size}")
                    for (f in fIt) {
                        if (f.canRead()) {
                            Log.d(TAG, "f.name = ${f.name}, isDirectory = ${f.isDirectory}, " +
                                    "fn.path = ${f.path}, fn.canWrite() = ${f.canWrite()}")
                            tempList.add(FileDescription(f, false))
                            index++
                            if (index >= FileDesList.maxFiles) {
                                ScreenUtil.showToast(activity,
                                        getString(R.string.excess_max) + " ${FileDesList.maxFiles}",
                                        textFontSize, BaseApplication.FontSize_Scale_Type,
                                        Toast.LENGTH_SHORT)
                                return@also
                            }
                        }
                    }
                }
            }
        }
        pathTextView.text = FileDesList.currentPath
        FileDesList.fileList.clear()
        FileDesList.fileList.addAll(tempList)
        Log.d(TAG, "searchCurrentFolder.FileDesList.fileList.size = ${FileDesList.fileList.size}" )

        myRecyclerViewAdapter?.notifyDataSetChanged()
    }

    private fun getSongs(songListSQLite : SongListSQLite, msg : String) : ArrayList<SongInfo> {
        ArrayList<SongInfo>().also {songIt ->
            for (i in 0 until FileDesList.fileList.size) {
                FileDesList.fileList[i].run {
                    if (selected) {
                        Log.d(TAG, "$msg.file.path = ${file.path}")
                        Log.d(TAG, "$msg.file.toUri() = ${file.toUri()}")
                        var song = SongInfo().apply {
                            songName = file.name
                            // filePath = file.path
                            filePath = file.toUri().toString()
                            musicTrackNo = 1    // guess
                            musicChannel = CommonConstants.StereoChannel
                            vocalTrackNo = 2    // guess
                            vocalChannel = CommonConstants.StereoChannel
                            included = "0"
                        }
                        songListSQLite.findOneSongByUriString(song.filePath)?.apply {
                            Log.d(TAG, "$msg.found")
                            included = "1"
                            song = this
                        }
                        songIt.add(song)
                    }
                }
            }
            return songIt
        }
    }

    private fun initFilesRecyclerView() {
        Log.d(TAG, "initFilesRecyclerView() is called")
        activity?.let {
            val yellow = ContextCompat.getColor(it, R.color.yellow)
            val transparentLightGray = ContextCompat.getColor(it, R.color.transparentLightGray)

            myRecyclerViewAdapter = OpenFilesRecyclerViewAdapter.getInstance(
                this, textFontSize, FileDesList.fileList, yellow, transparentLightGray)

            filesRecyclerView?.adapter = myRecyclerViewAdapter
            filesRecyclerView?.layoutManager = object : LinearLayoutManager(context) {
                override fun isAutoMeasureEnabled(): Boolean {
                    return false
                }
            }
        }
    }
}