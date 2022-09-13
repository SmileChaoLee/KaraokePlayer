package com.smile.karaokeplayer.interfaces

import com.smile.karaokeplayer.models.SongInfo

interface PlaySongs {
    fun playSelectedSongList(songs: ArrayList<SongInfo>)
}