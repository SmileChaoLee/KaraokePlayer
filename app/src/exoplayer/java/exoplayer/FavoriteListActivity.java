package exoplayer;

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
        Log.d(TAG, "setAudioLinearLayoutVisibility() is called");
        linearLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public Intent createPlayerActivityIntent() {
        Log.d(TAG, "createPlayerActivityIntent() is called");
        return new Intent(this, ExoSongPlayerActivity.class);
    }
}
