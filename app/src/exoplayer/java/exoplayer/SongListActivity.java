package exoplayer;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;

import com.smile.karaokeplayer.BaseSongListActivity;
import com.smile.smilelibraries.utilities.ContentUriAccessUtil;

import java.util.ArrayList;
import exoplayer.Utilities.UriUtil;

public class SongListActivity extends BaseSongListActivity {

    @Override
    public Intent createIntentFromSongDataActivity() {
        return new Intent(this, SongDataActivity.class);
    }

    @Override
    public Intent createSelectFilesToOpenIntent() {
        // selecting multiple files. Can be single
        return ContentUriAccessUtil.createIntentForSelectingFile(false);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentSongList(Intent data) {
        return UriUtil.getUrisListFromIntent(this, data);
    }

    @Override
    public void setAudioLinearLayoutVisibility(LinearLayout linearLayout) {
        linearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public Intent createPlayerActivityIntent() {
        return new Intent(this, ExoPlayerActivity.class);
    }
}
