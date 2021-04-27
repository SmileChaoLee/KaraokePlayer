package videoplayer;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import java.util.ArrayList;
import com.smile.karaokeplayer.BaseSongListActivity;

import videoplayer.utilities.FileSelectUtil;
import videoplayer.utilities.UriUtil;

public class SongListActivity extends BaseSongListActivity {

    @Override
    public Intent createIntentFromSongDataActivity() {
        return new Intent(this, SongDataActivity.class);
    }

    @Override
    public Intent createSelectFilesToOpenIntent() {
        return FileSelectUtil.selectFileToOpenIntent(this, false);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentSongList(Intent data) {
        return UriUtil.getUrisListFromIntent(this, data);
    }

    @Override
    public void setAudioLinearLayoutVisibility(LinearLayout linearLayout) {
        linearLayout.setVisibility(View.GONE);
    }

    @Override
    public Intent createPlayerActivityIntent() {
        return new Intent(this, VLCPlayerActivity.class);
    }
}
