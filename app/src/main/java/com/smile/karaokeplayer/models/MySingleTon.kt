package com.smile.karaokeplayer.models

object MySingleTon {
    const val maxSongs : Int = 100;
    val favorites : ArrayList<SongInfo> = ArrayList(maxSongs)
    val selectedFavorites : ArrayList<SongInfo> = ArrayList(maxSongs)
    val orderedSongs : ArrayList<SongInfo> = ArrayList(maxSongs)
}