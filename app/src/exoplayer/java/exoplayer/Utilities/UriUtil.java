package exoplayer.Utilities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.smile.karaokeplayer.Utilities.ContentUriAccessUtil;
import java.util.ArrayList;

public class UriUtil {
    private UriUtil() {};

    public static ArrayList<Uri> getUrisListFromIntent(Context context, Intent data) {
        return ContentUriAccessUtil.getUrisList(context, data);
    }
}
