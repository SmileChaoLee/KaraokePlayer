package com.smile.karaokeplayer.fragments

import android.net.Uri
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
import com.smile.karaokeplayer.models.FileDescription
import com.smile.smilelibraries.utilities.ScreenUtil
import java.io.File

private const val TAG : String = "OpenFileFragment"

class OpenFileFragment : Fragment(), OpenFilesRecyclerViewAdapter.OnRecyclerItemClickListener {
    interface PlayOpenFiles {
        fun playUriList(uris: ArrayList<Uri>)
    }
    private var fragmentView : View? = null
    private var textFontSize = 0f
    private lateinit var playOpenFiles: PlayOpenFiles
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

        playOpenFiles = (activity as PlayOpenFiles)
        Log.d(TAG, "onCreate.playOpenFiles = $playOpenFiles")

        // currentPath = Environment.getExternalStorageDirectory().toString()
        currentPath = "/"
        activity?.applicationContext?.externalCacheDirs?.let {
            for (element in it) {
                Log.d(TAG, "element.absolutePath = ${element.absolutePath}")
                element.absolutePath.let { pathIt ->
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

        fragmentView = inflater.inflate(R.layout.fragment_open_file, container, false)

        val defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(activity,
            BaseApplication.FontSize_Scale_Type, null)
        textFontSize = ScreenUtil.suitableFontSize(activity, defaultTextFontSize,
            BaseApplication.FontSize_Scale_Type,0.0f)

        val buttonWidth = (textFontSize*1.5f).toInt()

        fragmentView?.let {
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
                val uris = ArrayList<Uri>().also { uriIt ->
                    for (i in 0 until fileList.size) {
                        fileList[i].run {
                            if (selected) {
                                uriIt.add(file.toUri())
                            }
                        }
                    }
                }
                if (uris.size == 0) {
                    ScreenUtil.showToast(
                            activity, getString(R.string.noFilesSelectedString), textFontSize,
                            BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                } else {
                    playOpenFiles.playUriList(uris)
                }
            }
        }

        initFilesRecyclerView()
        searchCurrentFolder()

        return fragmentView
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
            ScreenUtil.showToast(
                    activity, fileList[position].file.name, textFontSize,
                    BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
        fileList[position].apply {
            if (file.isDirectory) {
                currentPath = file.path
                searchCurrentFolder()
            } else {
                selected = !selected
                myRecyclerViewAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun searchCurrentFolder() {
        Log.d(TAG, "searchCurrentFolder() is called")
        val listSize = fileList.size
        fileList.clear()
        myRecyclerViewAdapter.notifyItemRangeRemoved(0, listSize)

        currentPath.let {
            if (it == "/") {
                for (element in rootPathSet) {
                    Log.d(TAG, "searchCurrentFolder.element = $element")
                    FileDescription(File(element), false).apply {
                        Log.d(TAG, "searchCurrentFolder. = ${file.path}, ${file.absolutePath}")
                        fileList.add(this)
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
                            fileList.add(FileDescription(f, false))
                        }
                    }
                }
            }
        }

        pathTextView.text = currentPath
        // myRecyclerViewAdapter.notifyDataSetChanged()
        myRecyclerViewAdapter.notifyItemRangeInserted(0, fileList.size)
    }

    private fun initFilesRecyclerView() {
        Log.d(TAG, "initFilesRecyclerView() is called")
        activity?.let {
            myRecyclerViewAdapter = OpenFilesRecyclerViewAdapter(
                it, this, textFontSize, fileList)
            filesRecyclerView.adapter = myRecyclerViewAdapter
            filesRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }
}