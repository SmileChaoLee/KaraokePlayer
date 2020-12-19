package videoplayer.utilities;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

public final class ExternalStorageUtil {

    private static String TAG = "ExternalStorageUtil";

    private ExternalStorageUtil() {}

    /* Checks if external storage is available for read and write */
    /** @deprecated */
    @Deprecated
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    /** @deprecated */
    @Deprecated
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /* Get uri related content real local file path. */
    /** @deprecated */
    @Deprecated
    public static String getUriRealPath(Context ctx, Uri uri) {
        String ret = "";
        if ( isAboveKitKat() ) {
            // Android OS above sdk version 19.
            ret = getUriRealPathAboveKitkat(ctx, uri);
        } else {
            // Android OS below sdk version 19
            ret = getImageRealPath(ctx.getContentResolver(), uri, null);
        }

        return ret;
    }

    @SuppressLint("NewApi")
    private static String getUriRealPathAboveKitkat(Context ctx, Uri uri) {
        String ret = "";
        Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat() is called");
        if (ctx != null && uri != null) {
            Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> ctx and uri are not null");
            if (isDocumentUri(ctx, uri)) {
                Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> isDocumentUri()");
                // Get uri related document id.
                String documentId = DocumentsContract.getDocumentId(uri);

                // Get uri authority.
                String uriAuthority = uri.getAuthority();

                if (isMediaDoc(uriAuthority)) {
                    String idArr[] = documentId.split(":");
                    if (idArr.length == 2) {
                        // First item is document type.
                        String docType = idArr[0];

                        // Second item is document real id.
                        String realDocId = idArr[1];

                        // Get content uri by document type.
                        Uri mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        if ("image".equals(docType)) {
                            mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        } else if("video".equals(docType)) {
                            mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        } else if("audio".equals(docType)) {
                            mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        }

                        // Get where clause with real document id.
                        String whereClause = MediaStore.Images.Media._ID + " = " + realDocId;

                        ret = getImageRealPath(ctx.getContentResolver(), mediaContentUri, whereClause);
                    }
                } else if (isDownloadDoc(uriAuthority)) {
                    Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> isDownloadDoc()");
                    // Build download uri.
                    Uri downloadUri = Uri.parse("content://downloads/public_downloads");

                    // Append download document id at uri end.
                    try {
                        Uri downloadUriAppendId = ContentUris.withAppendedId(downloadUri, Long.valueOf(documentId));
                        ret = getImageRealPath(ctx.getContentResolver(), downloadUriAppendId, null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if(isExternalStoreDoc(uriAuthority)) {
                    Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> isExternalStoreDoc()");
                    String idArr[] = documentId.split(":");
                    if (idArr.length == 2) {
                        Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> idArr.length == 2");
                        String type = idArr[0];
                        Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> type = " + type);
                        String realDocId = idArr[1];
                        String relativePath = "/" + realDocId;
                        Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> realDocId = " + realDocId);
                        if ("primary".equalsIgnoreCase(type)) {
                            Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> primary");
                            ret = Environment.getExternalStorageDirectory() + relativePath;
                            // ret = Environment.getExternalStorageDirectory().getAbsolutePath() + relativePath;
                        } else {
                            int pos = -1;
                            String retTemp = System.getenv("SECONDARY_STORAGE");
                            if (retTemp != null) {
                                pos = retTemp.indexOf(':');
                            }
                            if (pos<0) {
                                // Third storage
                                retTemp = System.getenv("THIRD_STORAGE");
                                if (retTemp != null) {
                                    pos = retTemp.indexOf(':');
                                }
                            }
                            if (pos>=0) {
                                ret = retTemp.substring(0, pos) + relativePath;
                                Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> SECONDARY_STORAGE = " + ret);
                            } else {
                                // mnt directory
                                ret = Environment.getExternalStorageDirectory() + relativePath;
                                Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> not primary -->getExternalStorageDirectory() = " + ret);
                                if (!fileExists(ret)) {
                                    ret = "/mnt/sdcard" + relativePath;
                                    if (!fileExists(ret)) {
                                        // secondary storage
                                        ret = "/mnt/sdcard1" + relativePath;
                                    }
                                    Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> mnt-->mnt = " + ret);
                                }
                            }
                        }
                    }
                }
            } else if (isContentUri(uri)) {
                Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> isContentUri()");
                if (isGooglePhotoDoc(uri.getAuthority())) {
                    Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> isGooglePhotoDoc()");
                    ret = uri.getLastPathSegment();
                } else {
                    Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> not isGooglePhotoDoc()");
                    ret = getImageRealPath(ctx.getContentResolver(), uri, null);
                }
            } else if (isFileUri(uri)) {
                Log.d(TAG, "ExternalStorageUtil.getUriRealPathAboveKitkat()--> isFileUri()");
                ret = uri.getPath();
            }
        }

        return ret;
    }

    /* Check whether current android os version is bigger than kitkat or not. */
    private static boolean isAboveKitKat() {
        boolean ret = false;
        ret = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        return ret;
    }

    /* Check whether this uri represent a document or not. */
    @SuppressLint("NewApi")
    private static boolean isDocumentUri(Context ctx, Uri uri) {
        boolean ret = false;
        if(ctx != null && uri != null) {
            ret = DocumentsContract.isDocumentUri(ctx, uri);
        }
        return ret;
    }

    /* Check whether this uri is a content uri or not.
     *  content uri like content://media/external/images/media/1302716
     *  */
    private static boolean isContentUri(Uri uri) {
        boolean ret = false;
        if (uri != null) {
            String uriSchema = uri.getScheme();
            if("content".equalsIgnoreCase(uriSchema)) {
                ret = true;
            }
        }
        return ret;
    }

    /* Check whether this uri is a file uri or not.
     *  file uri like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
     * */
    private static boolean isFileUri(Uri uri) {
        boolean ret = false;
        if (uri != null) {
            String uriSchema = uri.getScheme();
            if("file".equalsIgnoreCase(uriSchema))
            {
                ret = true;
            }
        }
        return ret;
    }


    /* Check whether this document is provided by ExternalStorageProvider. */
    private static boolean isExternalStoreDoc(String uriAuthority) {
        boolean ret = false;

        if ("com.android.externalstorage.documents".equals(uriAuthority)) {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by DownloadsProvider. */
    private static boolean isDownloadDoc(String uriAuthority) {
        boolean ret = false;

        if ("com.android.providers.downloads.documents".equals(uriAuthority)) {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by MediaProvider. */
    private static boolean isMediaDoc(String uriAuthority) {
        boolean ret = false;

        if ("com.android.providers.media.documents".equals(uriAuthority)) {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by google photos. */
    private static boolean isGooglePhotoDoc(String uriAuthority) {
        boolean ret = false;

        if ("com.google.android.apps.photos.content".equals(uriAuthority)) {
            ret = true;
        }

        return ret;
    }

    /* Return uri represented document file real local path.*/
    private static String getImageRealPath(ContentResolver contentResolver, Uri uri, String whereClause) {
        String ret = "";

        try {
            // Query the uri with condition.
            Cursor cursor = contentResolver.query(uri, null, whereClause, null, null);

            if (cursor != null) {
                boolean moveToFirst = cursor.moveToFirst();
                if (moveToFirst) {

                    // Get columns name by uri type.
                    String columnName = MediaStore.Images.Media.DATA;

                    String uriType = contentResolver.getType(uri);
                    if (uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                        columnName = MediaStore.Images.Media.DATA;
                    } else if (uri == MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) {
                        columnName = MediaStore.Audio.Media.DATA;
                    } else if (uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
                        columnName = MediaStore.Video.Media.DATA;
                    }

                    // Get column index.
                    int imageColumnIndex = cursor.getColumnIndex(columnName);

                    // Get column value which is the uri related file local path.
                    ret = cursor.getString(imageColumnIndex);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return ret;
    }

    private static boolean fileExists(String filePath) {
        File file = new File(filePath);

        return file.exists();
    }
}
