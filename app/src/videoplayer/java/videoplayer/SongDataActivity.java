package videoplayer;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.smile.karaokeplayer.BaseSongDataActivity;

import java.util.ArrayList;

import videoplayer.utilities.FileSelectUtil;
import videoplayer.utilities.UriUtil;

public class SongDataActivity extends BaseSongDataActivity {

    @Override
    public void selectOneFilePathSongData(int requestCode) {
        FileSelectUtil.selectFileToOpen(this, requestCode, true);
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
