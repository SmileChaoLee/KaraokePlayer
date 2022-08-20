package videoplayer.utilities

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.aditya.filebrowser.Constants
import com.aditya.filebrowser.FileChooser

private const val TAG = "FileSelectUtil"

object FileSelectUtil {

    @JvmStatic
    fun selectFileToOpenIntent(activity: Activity?, isSingle: Boolean): Intent {
        Log.d(TAG, "onCreateView() is called")
        val intent = Intent(activity, FileChooser::class.java)
        var selectMode = Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal
        if (!isSingle) {
            // select multiple files
            selectMode = Constants.SELECTION_MODES.MULTIPLE_SELECTION.ordinal
        }
        intent.putExtra(Constants.SELECTION_MODE, selectMode)
        return intent
    }
}