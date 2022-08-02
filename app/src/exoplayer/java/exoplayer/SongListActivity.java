package exoplayer;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.smile.karaokeplayer.BaseSongListActivity;
import com.smile.smilelibraries.utilities.ContentUriAccessUtil;

import java.util.ArrayList;
import exoplayer.utilities.UriUtil;

public class SongListActivity extends BaseSongListActivity {
    private static final String TAG = "SongListActivity";

    @Override
    public Intent createIntentFromSongDataActivity() {
        Log.d(TAG, "createIntentFromSongDataActivity() is called");
        return new Intent(this, SongDataActivity.class);
    }

    @Override
    public Intent createSelectFilesToOpenIntent() {
        // selecting multiple files. Can be single
        Log.d(TAG, "createSelectFilesToOpenIntent() is called");
        return ContentUriAccessUtil.createIntentForSelectingFile(false);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentSongList(Intent data) {
        Log.d(TAG, "getUrisListFromIntentSongList() is called");
        return UriUtil.getUrisListFromIntent(this, data);
    }

    @Override
    public void setAudioLinearLayoutVisibility(LinearLayout linearLayout) {
        Log.d(TAG, "setAudioLinearLayoutVisibility() is called");
        linearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public Intent createPlayerActivityIntent() {
        Log.d(TAG, "createPlayerActivityIntent() is called");
        return new Intent(this, ExoPlayerActivity.class);
    }
}
