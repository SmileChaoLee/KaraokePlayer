package com.smile.karaokeplayer.models

object FavoriteSingleTon {
    const val maxFavorites : Int = 100;
    val favoriteList : ArrayList<SongInfo> = ArrayList(maxFavorites)
    val selectedList : ArrayList<SongInfo> = ArrayList(maxFavorites)
}