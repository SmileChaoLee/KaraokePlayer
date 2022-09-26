package com.smile.karaokeplayer.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.adapters.OpenFilesRecyclerViewAdapter
import com.smile.karaokeplayer.constants.CommonConstants
import com.smile.karaokeplayer.interfaces.PlaySongs
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
    private lateinit var filesRecyclerView : RecyclerView
    private lateinit var myRecyclerViewAdapter : OpenFilesRecyclerViewAdapter
    private lateinit var fileList : ArrayList<FileDescription>
    private var currentPath : String = "/"
    private var isPlayButton: Boolean = true
    private var rootPathSet: java.util.HashSet<String> = HashSet()

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

        // currentPath = Environment.getExternalStorageDirectory().toString()
        currentPath = "/"
        activity?.applicationContext?.externalCacheDirs?.let {
            Log.d(TAG, "externalCacheDirs = $it, externalCacheDirs.size = ${it.size}")
            for (element in it) {
                Log.d(TAG, "externalCacheDirs.element = $element")
                element?.absolutePath?.let { pathIt ->
                    pathIt.indexOf("/Android/data").let {indexIt ->
                        if (indexIt >= 0) {
                            pathIt.substring(0, indexIt).let {subIt ->
                                Log.d(TAG, "element.substring(0, indexIt) = $subIt")
                                rootPathSet.add(subIt)
                            }
                        }
                    }
                }
            }
        }

        fileList = ArrayList()
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
            pathTextView = it.findViewById(R.id.pathTextView)
            ScreenUtil.resizeTextSize(pathTextView, textFontSize, BaseApplication.FontSize_Scale_Type)
            val backKeyButton: ImageButton = it.findViewById(R.id.openFileBackKeyButton)
            var layoutParams: ViewGroup.MarginLayoutParams = backKeyButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            backKeyButton.setOnClickListener {
                if (currentPath == "/") return@setOnClickListener
                if (!rootPathSet.contains(currentPath)) {
                    val index = currentPath.lastIndexOf('/')
                    currentPath = if (index >= 0 ) currentPath.substring(0, index) else "/"
                }
                if (rootPathSet.contains(currentPath) || currentPath.isEmpty()) currentPath = "/"
                searchCurrentFolder()
            }
            val selectAllButton: ImageButton = it.findViewById(R.id.openFileSelectAllButton)
            layoutParams = selectAllButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            selectAllButton.setOnClickListener {
                for (i in 0 until fileList.size) {
                    fileList[i].run {
                        if (!file.isDirectory && !selected) {
                            selected = true
                            myRecyclerViewAdapter.notifyItemChanged(i)
                        }
                    }
                }
            }
            val unselectButton: ImageButton = it.findViewById(R.id.openFileUnselectButton)
            layoutParams = unselectButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            unselectButton.setOnClickListener {
                for (i in 0 until fileList.size) {
                    fileList[i].run {
                        if (!file.isDirectory && selected) {
                            selected = false
                            myRecyclerViewAdapter.notifyItemChanged(i)
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
                    val songs = ArrayList<SongInfo>().also {songIt ->
                        for (i in 0 until fileList.size) {
                            fileList[i].run {
                                if (selected) {
                                    Log.d(TAG, "playSelectedButton.activityIt.file.path = ${file.path}")
                                    Log.d(TAG, "playSelectedButton.activityIt.file.toUri() = ${file.toUri()}")
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
                                        Log.d(TAG, "playSelectedButton.activityIt.found")
                                        included = "1"
                                        song = this
                                    }
                                    songIt.add(song)
                                }
                            }
                        }
                    }
                    songListSQLite.closeDatabase()
                    if (songs.size == 0) {
                        ScreenUtil.showToast(
                                activity, getString(R.string.noFilesSelectedString), textFontSize,
                                BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                    } else {
                        playSongs.playSelectedSongList(songs)
                    }
                }
            }
        }

        initFilesRecyclerView()
        searchCurrentFolder()
    }

    override fun onResume() {
        Log.d(TAG, "onResume() is called")
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause() is called")
        super.onPause()
    }

    override fun onRecyclerItemClick(v: View?, position: Int) {
        Log.d(TAG, "onRecyclerItemClick.position = $position")
        if (position < 0) return
        if (fileList[position].file.isFile) {
            fileList[position].selected = !fileList[position].selected
            myRecyclerViewAdapter.notifyItemChanged(position)
            return
        }
        currentPath = fileList[position].file.path
        searchCurrentFolder()
    }

    private fun searchCurrentFolder() {
        Log.d(TAG, "searchCurrentFolder() is called")
        val tempList: ArrayList<FileDescription> = ArrayList()
        currentPath.let {
            if (it == "/") {
                for (element in rootPathSet) {
                    Log.d(TAG, "searchCurrentFolder.element = $element")
                    FileDescription(File(element), false).apply {
                        Log.d(TAG, "searchCurrentFolder. = ${file.path}, ${file.absolutePath}")
                        tempList.add(this)
                    }
                }
            } else {
                val fList = File(it).listFiles()
                Log.d(TAG, "fList = $fList")
                fList?.let { fIt ->
                    Log.d(TAG, "file.list().size() = ${fIt.size}")
                    for (f in fIt) {
                        if (f.canRead()) {
                            Log.d(TAG, "f.name = ${f.name}, isDirectory = ${f.isDirectory}, " +
                                    "fn.path = ${f.path}, fn.canWrite() = ${f.canWrite()}")
                            tempList.add(FileDescription(f, false))
                        }
                    }
                }
            }
        }
        pathTextView.text = currentPath
        fileList.clear()
        fileList.addAll(tempList)
        myRecyclerViewAdapter.notifyDataSetChanged()
    }

    private fun initFilesRecyclerView() {
        Log.d(TAG, "initFilesRecyclerView() is called")
        activity?.let {
            myRecyclerViewAdapter = OpenFilesRecyclerViewAdapter(
                it, this, textFontSize, fileList)
            filesRecyclerView.adapter = myRecyclerViewAdapter
            filesRecyclerView.layoutManager = object : LinearLayoutManager(context) {
                override fun isAutoMeasureEnabled(): Boolean {
                    return false
                }
            }
        }
    }
}