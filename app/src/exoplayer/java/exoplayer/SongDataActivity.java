package exoplayer;

import android.content.Intent;
import android.net.Uri;
import com.smile.karaokeplayer.BaseSongDataActivity;
import com.smile.karaokeplayer.Utilities.ContentUriAccessUtil;
import java.util.ArrayList;
import exoplayer.Utilities.UriUtil;

public class SongDataActivity extends BaseSongDataActivity {

    @Override
    public void selectOneFilePathSongData(int requestCode) {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        ContentUriAccessUtil.selectFileToOpen(this, requestCode, true);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentSongData(Intent data) {
        return UriUtil.getUrisListFromIntent(this, data);
    }

}
