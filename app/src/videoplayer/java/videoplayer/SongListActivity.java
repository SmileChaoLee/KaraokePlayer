package videoplayer;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import com.smile.karaokeplayer.BaseSongListActivity;

public class SongListActivity extends BaseSongListActivity {

    @Override
    public Intent createIntentFromSongDataActivity() {
        return new Intent(this, SongDataActivity.class);
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
