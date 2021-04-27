package videoplayer;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.smile.karaokeplayer.BaseSongDataActivity;

import java.util.ArrayList;

import videoplayer.utilities.FileSelectUtil;
import videoplayer.utilities.UriUtil;

public class SongDataActivity extends BaseSongDataActivity {

    private static final String TAG = "SongDataActivity";

    @Override
    public Intent createSelectOneFileToOpenIntent() {
        Log.d(TAG, "createSelectFileToOpenIntent() is called.");
        return FileSelectUtil.selectFileToOpenIntent(this, true);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentSongData(Intent data) {
        return UriUtil.getUrisListFromIntent(this, data);
    }

    @Override
    public void setKaraokeSettingLayoutVisibility() {
        karaokeSettingLayout.setVisibility(View.GONE);
    }

}
