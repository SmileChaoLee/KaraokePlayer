package com.smile.karaokeplayer.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.smile.karaokeplayer.BaseActivity
import com.smile.karaokeplayer.R

private const val TAG : String = "OpenFileFragment"

class OpenFileFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
        Log.d(TAG, "onCreate() is called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView() is called")
        activity?.let {
            Log.d(TAG, "selectFilesToOpen()")
            val baseActivity = it as BaseActivity
            baseActivity.playerFragment.selectFilesToOpen()
        }
        Log.d(TAG, "activity = $activity")
        return inflater.inflate(R.layout.fragment_open_file, container, false)
    }
}