package com.smile.karaokeplayer.fragments

import android.content.Intent
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.adapters.OpenFilesRecyclerViewAdapter
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.models.FileDescription
import com.smile.smilelibraries.utilities.ScreenUtil
import java.io.File

private const val TAG : String = "OpenFileFragment"

class OpenFileFragment : Fragment(), OpenFilesRecyclerViewAdapter.OnRecyclerItemClickListener {

    private var fragmentView : View? = null
    private var textFontSize = 0f
    private lateinit var pathTextView: TextView
    private lateinit var filesRecyclerView : RecyclerView
    private lateinit var myRecyclerViewAdapter : OpenFilesRecyclerViewAdapter
    private lateinit var fileList : ArrayList<FileDescription>
    private lateinit var currentPath : String
    private lateinit var selectedFiles : ArrayList<Uri>
    private var broadcastManager: LocalBroadcastManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
        Log.d(TAG, "onCreate() is called")
        currentPath = "/"
        fileList = ArrayList()
        selectedFiles = ArrayList()
        activity?.let {
            broadcastManager = LocalBroadcastManager.getInstance(it)
        }
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
            filesRecyclerView = it.findViewById(R.id.filesRecyclerView)
            pathTextView = it.findViewById(R.id.pathTextView)
            ScreenUtil.resizeTextSize(pathTextView, textFontSize, BaseApplication.FontSize_Scale_Type)
            val backKeyButton: ImageButton = it.findViewById(R.id.backKeyButton)
            var layoutParams: ViewGroup.MarginLayoutParams = backKeyButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            backKeyButton.setOnClickListener {
                currentPath.lastIndexOf('/').let { indexIt ->
                    if (indexIt >= 0) {
                        currentPath = currentPath.substring(0, indexIt).run {
                            this.ifEmpty { "/" }
                        }
                        searchCurrentFolder()
                    }
                }
            }
            val selectAllButton: ImageButton = it.findViewById(R.id.selectAllButton)
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
            val unselectButton: ImageButton = it.findViewById(R.id.unselectButton)
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
            val refreshButton: ImageButton = it.findViewById(R.id.refreshButton)
            layoutParams = refreshButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            refreshButton.setOnClickListener {
                searchCurrentFolder()
            }
            val playSelectedButton: ImageButton = it.findViewById(R.id.playSelectedButton)
            layoutParams = playSelectedButton.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.width = buttonWidth
            layoutParams.height = buttonWidth
            playSelectedButton.setOnClickListener {
                // open the files to play
                val uris = ArrayList<Uri>().apply {
                    for (element in fileList) {
                        element.run {
                            if (selected) {
                                add(file.toUri())
                                selected = false
                            }
                        }
                    }
                }
                if (uris.size == 0) {
                    ScreenUtil.showToast(
                            activity, getString(R.string.noFilesSelectedString), textFontSize,
                            BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT)
                } else {
                    broadcastManager?.apply {
                        sendBroadcast(Intent(PlayerConstants.Play_Songs).apply {
                            putExtra(PlayerConstants.Song_Uri_List, uris)
                        })
                    }
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
        File(currentPath).listFiles()?.let {
            Log.d(TAG, "file.list().size() = ${it.size}")
            for (f in it) {
                if (f.canRead()) {
                    Log.d(TAG, "f.name = ${f.name}, isDirectory = ${f.isDirectory}, " +
                            "fn.path = ${f.path}, fn.canWrite() = ${f.canWrite()}")
                    fileList.add(FileDescription(f, false))
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