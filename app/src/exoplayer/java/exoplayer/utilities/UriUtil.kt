package exoplayer.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.smile.smilelibraries.utilities.ContentUriAccessUtil
import java.util.ArrayList

object UriUtil {
    @JvmStatic
    fun getUrisListFromIntent(context: Context?, data: Intent?): ArrayList<Uri> {
        return ContentUriAccessUtil.getUrisList(context, data)
    }
}