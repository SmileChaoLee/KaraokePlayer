package com.smile.karaokeplayer.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
class FileDescription(var file: File, var selected: Boolean): Parcelable