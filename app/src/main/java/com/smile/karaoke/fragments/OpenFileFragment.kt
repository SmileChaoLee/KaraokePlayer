package com.smile.karaoke.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.smile.karaoke.R
import com.smile.karaoke.adapters.MyLayoutManager
import com.smile.karaoke.adapters.OpenFilesRecyclerViewAdapter
import com.smile.karaoke.interfaces.RecyclerItemListener
import com.smile.karaoke.models.MySingleton
import com.smile.karaoke.ui_intents.OpenFileUiIntent
import com.smile.karaoke.ui_states.OpenFileUiState
import com.smile.karaoke.utilities.LogUtil
import com.smile.karaoke.viewmodels.OpenFileViewModel
import com.smile.smilelibraries.utilities.ScreenUtil
import kotlinx.coroutines.launch

abstract class OpenFileFragment : ComOpenFragment(), RecyclerItemListener {

    companion object {
        private const val TAG : String = "OpenFileFragment"
    }

    abstract fun decoderButtonVisibility(): Int

    private var pathTextView: TextView? = null
    var fileSearchEditText: EditText? = null
    private var filesRecyclerView : RecyclerView? = null
    private var loadingMsgTextView: TextView? = null
    private var myRecyclerViewAdapter : OpenFilesRecyclerViewAdapter? = null
    private var backKeyButton: ImageButton? = null
    private var selectAllButton: ImageButton? = null
    private var unselectButton: ImageButton? = null
    private var switchDecoderButton: ImageButton? = null
    private var playSelectedButton: ImageButton? = null
    private var addToFavoriteButton: ImageButton? = null

    /**
     * Do not use viewModels() because the viewModelScope will be killed
     * when switching tab away from this fragment because viewModelScope
     * depending on the lifecycle of the fragment. The viewModelScope is not active
     * when switching tab back to this fragment
     * Important:
     * Use activityViewModels() to share the same viewModel and viewModelScope with the activity
     * because the lifecycle of the activity is longer than the fragment
     */
    private val viewModel: OpenFileViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtil.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        LogUtil.d(TAG, "onCreate.FileDesList.currentPath = ${MySingleton.currentPath}")
        activity?.applicationContext?.externalCacheDirs?.let {
            LogUtil.d(TAG, "externalCacheDirs = $it, externalCacheDirs.size = ${it.size}")
            MySingleton.rootPathSet.clear()
            for (element in it) {
                LogUtil.d(TAG, "externalCacheDirs.element = $element")
                element?.absolutePath?.let { pathIt ->
                    pathIt.indexOf("/Android/data").let {indexIt ->
                        if (indexIt >= 0) {
                            pathIt.substring(0, indexIt).let {subIt ->
                                LogUtil.d(TAG, "element.substring(0, indexIt) = $subIt")
                                MySingleton.rootPathSet.add(subIt)
                            }
                        }
                    }
                }
            }
        }
        LogUtil.i(TAG, "onCreate.FileDesList.fileList.size = ${MySingleton.fileList.size}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LogUtil.i(TAG, "onCreateView")
        return inflater.inflate(R.layout.fragment_open_file,
            container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        LogUtil.i(TAG, "onViewCreated")
        view.let {
            filesRecyclerView = it.findViewById(R.id.openFilesRecyclerView)
            filesRecyclerView?.setHasFixedSize(true)
            filesRecyclerView?.visibility = View.GONE
            loadingMsgTextView = it.findViewById(R.id.loadingMsgTextView)
            ScreenUtil.resizeTextSize(loadingMsgTextView, textFontSize * 2f)
            loadingMsgTextView?.visibility = View.GONE
            pathTextView = it.findViewById(R.id.pathTextView)
            ScreenUtil.resizeTextSize(pathTextView, textFontSize)
            backKeyButton = it.findViewById(R.id.openFileBackKeyButton)
            selectAllButton = it.findViewById(R.id.openFileSelectAllButton)
            unselectButton = it.findViewById(R.id.openFileUnselectButton)
            switchDecoderButton = it.findViewById(R.id.openFileSwitchDecoderButton)
            setupSwitchDecoderButton()
            playSelectedButton = it.findViewById(R.id.openFilePlaySelectedButton)
            addToFavoriteButton = it.findViewById(R.id.addToFavoriteButton)
            showVideoButton = it.findViewById(R.id.showVideoImageButton)
            showVideoButton?.visibility = View.VISIBLE
            exitImageButton = it.findViewById(R.id.exitImageButton)
            exitImageButton?.visibility = View.VISIBLE
            fileSearchEditText = it.findViewById(R.id.fileSearchEditText)
            fileSearchEditText?.let { sEt ->
                ScreenUtil.resizeTextSize(sEt, textFontSize)
                sEt.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
                    override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
                    override fun afterTextChanged(editable: Editable) {
                        LogUtil.d(TAG, "addTextChangedListener.afterTextChanged")
                        if (!sEt.hasFocus()) {
                            LogUtil.d(TAG, "addTextChangedListener.no focus")
                            return
                        }
                        val searchStr = editable.toString().trim()
                        LogUtil.d(TAG, "addTextChangedListener.searchStr = $searchStr")
                        if (searchStr.isEmpty()) {
                            viewModel.handleIntent(
                                OpenFileUiIntent.SearchCurrentFolder(
                                    activity, videoThumbNailsWidth, videoThumbNailsHeight
                                )
                            )
                        } else {
                            viewModel.handleIntent(OpenFileUiIntent.SearchFiles(activity, searchStr))
                        }
                    }
                })
            }
        }

        // work with viewmodel
        // Launch a coroutine scoped to the Fragment's View lifecycle
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle suspends until the Lifecycle hits the STARTED state,
            // and automatically cancels collection when it drops below STARTED.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update your RecyclerView adapter or UI here
                    LogUtil.d(TAG, "viewLifecycleOwner.lifecycleScope.updateMviScreen(state)")
                    updateMviScreen(state)
                }
            }
        }

        fileSearchEditText?.post {
            LogUtil.d(TAG, "fileSearchEditText.setText()")
            fileSearchEditText?.setText("")
        }
        initFilesRecyclerView()
        viewModel.handleIntent(
            OpenFileUiIntent.SearchCurrentFolder(
                activity, videoThumbNailsWidth, videoThumbNailsHeight
            )
        )

        super.onViewCreated(view, savedInstanceState)
    }

    private fun updateMviScreen(state: OpenFileUiState) {
        val logStr = "updateMviScreen"
        LogUtil.d(TAG, logStr)
        when (state) {
            is OpenFileUiState.Initial -> {
                LogUtil.d(TAG, "$logStr.OpenFileUiState.Initial")
                // Handle the initial state if needed
            }
            is OpenFileUiState.StartLoading -> {
                LogUtil.d(TAG, "$logStr.OpenFileUiState.StartLoading")
                searchingStartView()
            }
            is OpenFileUiState.StopLoading -> {
                LogUtil.d(TAG, "$logStr.OpenFileUiState.StopLoading")
                searchingFinishView()
            }
            is OpenFileUiState.FinishLoading -> {
                LogUtil.d(TAG, "$logStr.OpenFileUiState.FinishLoading")
                pathTextView?.text = MySingleton.currentPath
                state.fileList?.let { fileList ->
                    MySingleton.fileList.clear()
                    MySingleton.fileList.addAll(fileList)
                }
                updateRecyclerView()
            }
            is OpenFileUiState.ShowToast -> {
                // Handle the toast event
                LogUtil.d(TAG, "$logStr.OpenFileUiState.ShowToast")
                when (state.event) {
                    OpenFileUiState.EXCESS_MAX -> {
                        ScreenUtil.showToast(
                            activity, getString(R.string.excess_max) +
                                    " ${MySingleton.MAX_FILES}", textFontSize,
                            Toast.LENGTH_SHORT
                        )
                    }
                    OpenFileUiState.NO_FILES_SELECTED -> {
                        ScreenUtil.showToast(
                            activity, getString(R.string.noFilesSelectedString),
                            textFontSize, Toast.LENGTH_SHORT
                        )
                    }
                    OpenFileUiState.ADD_TO_FAVORITES -> {
                        ScreenUtil.showToast(
                            activity, getString(R.string.add_to_favorites),
                            textFontSize, Toast.LENGTH_SHORT
                        )
                    }
                }
            }
            is OpenFileUiState.UpdateSelectedSong -> {
                LogUtil.d(TAG, "$logStr.OpenFileUiState.UpdateSelectedSong")
                myRecyclerViewAdapter?.myNotifyItemChanged(state.position)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        LogUtil.i(TAG, "onStart")
        setupSwitchDecoderButton()
    }

    override fun onResume() {
        super.onResume()
        LogUtil.i(TAG, "onResume")
        setProperFocus()
    }

    override fun onPause() {
        super.onPause()
        LogUtil.i(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        LogUtil.i(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.i(TAG, "onDestroy")
        clearFileList()
        mediaRetriever.release()
    }

    private fun setProperFocus() {
        if (MySingleton.fileList.isEmpty()) {
            filesRecyclerView?.visibility = View.GONE
            backKeyButton?.post { backKeyButton?.requestFocus() }
        } else {
            filesRecyclerView?.visibility = View.VISIBLE
            // filesRecyclerView?.post { filesRecyclerView?.requestFocus() }
            // filesRecyclerView?.clearFocus()
            fileSearchEditText?.postDelayed(
                { fileSearchEditText?.requestFocus() },
                50
            )
        }
    }

    private fun updateRecyclerView() {
        searchingFinishView()
        myRecyclerViewAdapter?.myNotifyDataSetChanged()
        setProperFocus()
    }

    override fun onItemClick(v: View?, position: Int) {
        LogUtil.i(TAG, "onItemClick.position = $position")
        if (position < 0) return
        val fileDes = MySingleton.fileList[position]
        if (fileDes.file.isFile) {
            LogUtil.i(TAG, "onItemClick.position = $position isFile")
            viewModel.handleIntent(OpenFileUiIntent.SongOnClicked(position))
        } else {
            LogUtil.d(TAG, "onItemClick.fileDes.file is not file")
            MySingleton.currentPath = fileDes.file.path
            fileSearchEditText?.setText("")
            viewModel.handleIntent(
                OpenFileUiIntent.SearchCurrentFolder(
                    activity, videoThumbNailsWidth, videoThumbNailsHeight
                )
            )
        }
    }

    private fun clearFileList() {
        MySingleton.fileList.clear()
        myRecyclerViewAdapter?.myNotifyDataSetChanged()
        filesRecyclerView?.visibility = View.GONE
        viewModel.handleIntent(OpenFileUiIntent.ClearSelectedSongs)
    }

    private fun searchingStartView() {
        searchCompleted = false
        filesRecyclerView?.visibility = View.GONE
        loadingMsgTextView?.visibility = View.VISIBLE
    }
    private fun searchingFinishView() {
        searchCompleted = true
        filesRecyclerView?.visibility = View.VISIBLE
        loadingMsgTextView?.visibility = View.GONE
        setProperFocus()
    }

    // overriding the methods of ItemsBaseFragment
    override fun setClickListeners() {
        backKeyButton?.setOnClickListener {
            val logStr = "backKeyButton.setOnClickListener"
            LogUtil.d(TAG, "$logStr.searchCompleted = $searchCompleted")
            if (!searchCompleted) return@setOnClickListener // searching
            if (MySingleton.currentPath == "/") return@setOnClickListener
            MySingleton.currentPath =
                if (MySingleton.rootPathSet.contains(MySingleton.currentPath)) "/"
                else {
                    val index = MySingleton.currentPath.lastIndexOf('/')
                    if (index >= 0 ) MySingleton.currentPath.substring(0, index) else "/"
                }
            if (MySingleton.currentPath.isEmpty()) MySingleton.currentPath = "/"
            fileSearchEditText?.setText("")
            viewModel.handleIntent(
                OpenFileUiIntent.SearchCurrentFolder(
                    activity, videoThumbNailsWidth, videoThumbNailsHeight
                )
            )
        }
        selectAllButton?.setOnClickListener {
            val logStr = "selectAllButton.setOnClickListener"
            LogUtil.d(TAG, "$logStr.searchCompleted = $searchCompleted")
            if (!searchCompleted) return@setOnClickListener // searching
            for (i in 0 until MySingleton.fileList.size) {
                MySingleton.fileList[i].run {
                    if (!file.isDirectory && !selected) {
                        selected = true
                        myRecyclerViewAdapter?.myNotifyItemChanged(i)
                    }
                }
            }
        }
        unselectButton?.setOnClickListener {
            val logStr = "unselectButton.setOnClickListener"
            LogUtil.d(TAG, "$logStr.searchCompleted = $searchCompleted")
            if (!searchCompleted) return@setOnClickListener // searching
            for (i in 0 until MySingleton.fileList.size) {
                MySingleton.fileList[i].run {
                    if (!file.isDirectory && selected) {
                        selected = false
                        myRecyclerViewAdapter?.myNotifyItemChanged(i)
                    }
                }
            }
        }
        switchDecoderButton?.let {switchIt ->
            val logStr = "switchDecoderButton.setOnClickListener"
            LogUtil.d(TAG, "$logStr.searchCompleted = $searchCompleted")
            switchIt.setOnClickListener {
                if (!searchCompleted) return@setOnClickListener // searching
                playSongs?.switchBetweenSoftAndHardDecoder()
                setupSwitchDecoderButton()
            }
        }
        playSelectedButton?.setOnClickListener {
            val logStr = "playSelectedButton.setOnClickListener"
            LogUtil.d(TAG, "$logStr.searchCompleted = $searchCompleted")
            if (!searchCompleted) return@setOnClickListener // searching
            viewModel.handleIntent(OpenFileUiIntent.StartPlaySelectedSong(activity, playSongs))
        }
        addToFavoriteButton?.setOnClickListener {
            val logStr = "addToFavoriteButton.setOnClickListener"
            LogUtil.d(TAG, "$logStr.searchCompleted = $searchCompleted")
            if (!searchCompleted) return@setOnClickListener // searching
            viewModel.handleIntent(OpenFileUiIntent.AddToFavorites(activity))
        }

        super.setClickListeners()
    }

    override fun setButtonsSize() {
        buttonLayout = fragmentView?.findViewById(R.id.openFileButtonLayout)
        super.setButtonsSize()
        backKeyButton?.layoutParams = buttonParam
        selectAllButton?.layoutParams = buttonParam
        unselectButton?.layoutParams = buttonParam
        switchDecoderButton?.layoutParams = buttonParam
        playSelectedButton?.layoutParams = buttonParam
        addToFavoriteButton?.layoutParams = buttonParam
    }
    // end of overriding the methods of ItemsBaseFragment

    private fun initFilesRecyclerView() {
        LogUtil.i(TAG, "initFilesRecyclerView")
        activity?.let {
            myRecyclerViewAdapter = OpenFilesRecyclerViewAdapter(
                this, MySingleton.fileList, textFontSize)
            filesRecyclerView?.itemAnimator = null
            filesRecyclerView?.adapter = myRecyclerViewAdapter
            filesRecyclerView?.layoutManager = MyLayoutManager(context, gridSpanCount())
            updateRecyclerView()
        }
    }

    private fun setupSwitchDecoderButton() {
        LogUtil.i(TAG, "setupSwitchDecoderButton")
        switchDecoderButton?.apply {
            visibility = decoderButtonVisibility()
            playSongs?.let {
                setImageResource(
                    if (it.isSoftDecoderFirst()) R.drawable.soft_decoder
                    else R.drawable.hard_decoder
                )
            }
        }
    }
}