package exoplayer.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.lang.Exception
import java.util.ArrayList

object ContentUriAccessUtil {
    private const val TAG = "ContentUriAccessUtil"

    @JvmStatic
    fun selectFileToOpen(activity: Activity, requestCode: Int, isSingleFile: Boolean) {
        val intent = createIntentForSelectingFile(isSingleFile)
        activity.startActivityForResult(intent, requestCode)
    }

    @JvmStatic
    fun createIntentForSelectingFile(isSingleFile: Boolean): Intent {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !isSingleFile)
        intent.type = "*/*"
        return intent
    }

    @JvmStatic
    fun getUrisList(context: Context, data: Intent?): ArrayList<Uri?> {
        val urisList = ArrayList<Uri?>()
        var contentUri: Uri?
        if (data != null) {
            var isPermitted: Boolean
            if (data.clipData != null) {
                // multiple files
                for (i in 0 until data.clipData!!.itemCount) {
                    try {
                        contentUri = data.clipData!!.getItemAt(i).uri
                        if (contentUri != null && Uri.EMPTY != contentUri) {
                            isPermitted = getPermissionForContentUri(context, contentUri)
                            if (isPermitted) {
                                urisList.add(contentUri)
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "data.getClipData exception: ")
                        e.printStackTrace()
                    }
                }
            } else {
                // single file
                contentUri = data.data
                if (contentUri != null && Uri.EMPTY != contentUri) {
                    isPermitted = getPermissionForContentUri(context, contentUri)
                    if (isPermitted) {
                        urisList.add(data.data)
                    }
                }
            }
        }
        return urisList
    }

    private fun getPermissionForContentUri(context: Context, contentUri: Uri?): Boolean {
        var isPermitted = true
        try {
            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            Log.d(TAG, "getPermissionForContentUri.takeFlags = $takeFlags")
            context.contentResolver.takePersistableUriPermission(
                contentUri!!,
                takeFlags
            )
        } catch (e: Exception) {
            e.printStackTrace()
            isPermitted = false
        }
        return isPermitted
    }
}