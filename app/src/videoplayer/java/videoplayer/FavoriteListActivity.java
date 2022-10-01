package videoplayer;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import com.smile.karaokeplayer.BaseFavoriteListActivity;

public class FavoriteListActivity extends BaseFavoriteListActivity {

    private static final String TAG = "FavoriteListActivity";
    @Override
    public Intent createIntentFromSongDataActivity() {
        Log.d(TAG, "createIntentFromSongDataActivity() is called");
        return new Intent(this, SongDataActivity.class);
    }

    @Override
    public void setAudioLinearLayoutVisibility(LinearLayout linearLayout) {
        linearLayout.setVisibility(View.GONE);
    }
}
