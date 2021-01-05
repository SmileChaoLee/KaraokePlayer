package videoplayer.utilities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

public class UriUtil {
    private UriUtil() {};

    private static final String TAG = "UriUtil";

    public static ArrayList<Uri> getUrisListFromIntent(Context context, Intent data) {
        ArrayList<Uri> urisList = new ArrayList<>();

        if (data.getData() != null) {
            // com.aditya.filebrowser.Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal();
            Log.d(TAG, "com.aditya.filebrowser.Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal()");
            urisList.add(data.getData());
        } else {
            // com.aditya.filebrowser.Constants.SELECTION_MODES.MULTIPLE_SELECTION.ordinal()
            Log.d(TAG, "com.aditya.filebrowser.Constants.SELECTION_MODES.MULTIPLE_SELECTION.ordinal()");
            urisList = data.getParcelableArrayListExtra(com.aditya.filebrowser.Constants.SELECTED_ITEMS);
        }

        return urisList;
    }
}
