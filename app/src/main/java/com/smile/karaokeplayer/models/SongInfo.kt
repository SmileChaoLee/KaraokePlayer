package com.smile.karaokeplayer.models

import android.os.Parcelable
import com.smile.karaokeplayer.constants.CommonConstants
import kotlinx.parcelize.Parcelize

@Parcelize
class SongInfo constructor(var id : Int, var songName: String?, var filePath: String?,
                           var musicTrackNo : Int, var musicChannel : Int,
                           var vocalTrackNo : Int, var vocalChannel : Int,
                           var included : String?) : Parcelable{
    constructor() : this(0, "", "", 1,
        CommonConstants.RightChannel, 1, CommonConstants.LeftChannel,
        "1")
}