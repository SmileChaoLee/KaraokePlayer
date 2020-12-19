package com.smile.karaokeplayer.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;

public final class ContentUriAccessUtil {

    private static final String TAG = "ContentUriAccessUtil";

    private ContentUriAccessUtil() {}

    public static void selectFileToOpen(Activity activity, int requestCode, boolean isSingleFile) {
        Intent intent = createIntentForSelectingFile(isSingleFile);
        activity.startActivityForResult(intent, requestCode);
    }

    public static Intent createIntentForSelectingFile(boolean isSingleFile) {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !isSingleFile);
        intent.setType("*/*");

        return intent;
    }

    public static ArrayList<Uri> getUrisList(Context context, Intent data) {
        ArrayList<Uri> urisList = new ArrayList<>();
        Uri contentUri;
        if (data != null) {
            boolean isPermitted;
            if (data.getClipData() != null) {
                // multiple files
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    try {
                        contentUri = data.getClipData().getItemAt(i).getUri();
                        if ((contentUri != null) && (!Uri.EMPTY.equals(contentUri))) {
                            isPermitted = getPermissionForContentUri(context, data, contentUri);
                            if (isPermitted) {
                                urisList.add(contentUri);
                            }
                        }
                    } catch(Exception e) {
                        Log.d(TAG, "data.getClipData exception: ");
                        e.printStackTrace();
                    }
                }
            } else {
                // single file
                contentUri = data.getData();
                if ( (contentUri != null) && (!Uri.EMPTY.equals(contentUri)) ) {
                    isPermitted = getPermissionForContentUri(context, data, contentUri);
                    if (isPermitted) {
                        urisList.add(data.getData());
                    }
                }
            }
        }

        return urisList;
    }

    public static boolean getPermissionForContentUri(Context context, Intent data, Uri contentUri) {
        boolean isPermitted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                context.getContentResolver().takePersistableUriPermission(contentUri, takeFlags);
            } catch (Exception e) {
                e.printStackTrace();
                isPermitted = false;
            }
        }

        return isPermitted;
    }
}