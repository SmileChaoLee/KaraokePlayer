package exoplayer.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.lang.Exception
import java.util.ArrayList

object ContentUriAccessUtil {
    private const val TAG = "ContentUriAccessUtil"

    @JvmStatic
    fun createIntentForSelectingFile(isSingleFile: Boolean): Intent {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !isSingleFile)
            type = "*/*"
        }

        return intent
    }

    @JvmStatic
    fun getUrisList(context: Context, data: Intent): ArrayList<Uri> {
        val urisList = ArrayList<Uri>()
        val clipData = data.clipData
        if ( clipData != null) {
            // multiple files
            Log.d(TAG, "getUrisList.multiple files")
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).uri?.let{
                    if (!Uri.EMPTY.equals(it) && getPermissionForContentUri(context, it)) {
                        urisList.add(it)
                    }
                }
            }
        } else {
            // single file
            data.data?.let {
                if (!Uri.EMPTY.equals(it) && getPermissionForContentUri(context, it)) {
                    urisList.add(it)
                    Log.d(TAG, "getUrisList.single file.it = $it")
                }
            }
        }
        return urisList
    }

    private fun getPermissionForContentUri(context: Context, contentUri: Uri): Boolean {
        try {
            context.contentResolver.takePersistableUriPermission(
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }
}