package com.smile.karaokeplayer.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaokeplayer.BaseApplication
import com.smile.karaokeplayer.R
import com.smile.karaokeplayer.adapters.OpenFilesRecyclerViewAdapter
import com.smile.smilelibraries.utilities.ScreenUtil
import java.io.File

private const val TAG : String = "OpenFileFragment"

class OpenFileFragment : Fragment(), OpenFilesRecyclerViewAdapter.OnRecyclerItemClickListener {

    private var fragmentView : View? = null
    private var textFontSize = 0f
    private lateinit var filesRecyclerView : RecyclerView
    private lateinit var myRecyclerViewAdapter : OpenFilesRecyclerViewAdapter
    private lateinit var fileList : ArrayList<File>
    private lateinit var currentFolder : String
    private lateinit var selectedFiles : ArrayList<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
        Log.d(TAG, "onCreate() is called")
        currentFolder = "/"
        selectedFiles = ArrayList()
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

        fragmentView?.let {
            filesRecyclerView = it.findViewById(R.id.filesRecyclerView)
        }

        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fileList = ArrayList()
        File(currentFolder).listFiles()?.let {
            Log.d(TAG, "file.list().size() = ${it.size}")
            for (f in it) {
                if (f.canRead()) {
                    Log.d(TAG, "f.name = ${f.name}, isDirectory = ${f.isDirectory}, " +
                            "fn.path = ${f.path}, fn.canWrite() = ${f.canWrite()}")
                    fileList.add(f)
                }
            }
        }
        initFilesRecyclerView()

        super.onViewCreated(view, savedInstanceState)
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
        Log.d(TAG, "onRecyclerItemClick() is called")
        activity?.let {
            ScreenUtil.showToast(
                it, fileList[position].name, textFontSize,
                BaseApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT
            )
        }
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