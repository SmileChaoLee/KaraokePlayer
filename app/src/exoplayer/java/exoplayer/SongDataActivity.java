package exoplayer;

import android.util.Log;
import android.view.View;
import com.smile.karaokeplayer.BaseSongDataActivity;

public class SongDataActivity extends BaseSongDataActivity {

    private static final String TAG = "SongDataActivity";

    @Override
    public void setKaraokeSettingLayoutVisibility() {
        Log.d(TAG, "setKaraokeSettingLayoutVisibility()");
        karaokeSettingLayout.setVisibility(View.VISIBLE);
    }
}
