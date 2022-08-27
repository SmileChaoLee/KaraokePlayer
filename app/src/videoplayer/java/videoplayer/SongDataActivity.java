package videoplayer;

import android.view.View;

import com.smile.karaokeplayer.BaseSongDataActivity;

public class SongDataActivity extends BaseSongDataActivity {

    private static final String TAG = "SongDataActivity";

    @Override
    public void setKaraokeSettingLayoutVisibility() {
        karaokeSettingLayout.setVisibility(View.GONE);
    }

}
