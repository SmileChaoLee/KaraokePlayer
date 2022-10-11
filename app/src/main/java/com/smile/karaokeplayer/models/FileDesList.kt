package com.smile.karaokeplayer.models

object FileDesList {
    const val maxFiles : Int = 500;
    val fileList : ArrayList<FileDescription> = ArrayList(maxFiles)
    val rootPathSet : java.util.HashSet<String> = HashSet()
    var currentPath = "/"
}