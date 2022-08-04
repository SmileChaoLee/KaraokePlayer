package videoplayer.utilities;

import android.app.Activity;
import android.content.Intent;
import com.aditya.filebrowser.FileChooser;

public final class FileSelectUtil {
    private FileSelectUtil() {};

    public static Intent selectFileToOpenIntent(Activity activity, boolean isSingle) {
        Intent intent = new Intent(activity, FileChooser.class);
        int selectMode = com.aditya.filebrowser.Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal();
        if (!isSingle) {
            // select multiple files
            selectMode = com.aditya.filebrowser.Constants.SELECTION_MODES.MULTIPLE_SELECTION.ordinal();
        }
        intent.putExtra(com.aditya.filebrowser.Constants.SELECTION_MODE, selectMode);

        return intent;
    }
}
