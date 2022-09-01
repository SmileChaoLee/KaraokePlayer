package videoplayer;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import com.smile.karaokeplayer.BaseFavoriteListActivity;

public class FavoriteListActivity extends BaseFavoriteListActivity {

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
        return new Intent(this, VLCSongPlayerActivity.class);
    }
}
