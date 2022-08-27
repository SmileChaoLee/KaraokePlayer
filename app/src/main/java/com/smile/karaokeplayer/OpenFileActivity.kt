package com.smile.karaokeplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.smile.karaokeplayer.constants.PlayerConstants
import com.smile.karaokeplayer.fragments.OpenFileFragment

private const val TAG: String = "OpenFileActivity"

class OpenFileActivity : AppCompatActivity(), OpenFileFragment.PlayOpenFiles {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_file)

        val openFragment = OpenFileFragment()
        supportFragmentManager.beginTransaction().apply {
            add(R.id.openFileConstraintLayout, openFragment)
            commit()
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressedDispatcher.handleOnBackPressed")
                setResult(RESULT_CANCELED)
                finish()
            }
        })
    }

    // implementing interface OpenFileFragment.PlayOpenFiles
    override fun playUriList(uris: ArrayList<Uri>) {
        Log.d(TAG, "playUriList.uris.size = ${uris.size}")
        setResult(RESULT_OK, Intent().putExtra(PlayerConstants.Uri_List, uris))
        finish()
    }
    // Finishes implementing interface OpenFileFragment.PlayOpenFiles
}