package com.smile.karaokeplayer.models

object FileDesList {
    private val tempList : ArrayList<FileDescription> = ArrayList()
    private val tempRootSet : java.util.HashSet<String> = HashSet()
    val fileList = tempList
    val rootPathSet = tempRootSet
    var currentPath = "/"
}