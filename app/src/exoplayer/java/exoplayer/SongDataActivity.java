package exoplayer;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.smile.karaokeplayer.BaseSongDataActivity;
import com.smile.smilelibraries.utilities.ContentUriAccessUtil;

import java.util.ArrayList;
import exoplayer.Utilities.UriUtil;

public class SongDataActivity extends BaseSongDataActivity {

    private static final String TAG = "SongDataActivity";

    @Override
    public Intent createSelectOneFileToOpenIntent() {
        Log.d(TAG, "createSelectFileToOpenIntent() is called.");
        return ContentUriAccessUtil.createIntentForSelectingFile(true);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentSongData(Intent data) {
        return UriUtil.getUrisListFromIntent(this, data);
    }

    @Override
    public void setKaraokeSettingLayoutVisibility() {
        karaokeSettingLayout.setVisibility(View.VISIBLE);
    }

}
