package com.smile.karaokeplayer.interfaces

import android.content.ComponentName
import android.content.Intent

interface PlayMyFavorites {
    fun intentForFavoriteListActivity(): Intent
    fun onSavePlayingState(compName : ComponentName?)
    fun restorePlayingState()
    fun switchToOpenFileFragment()
}