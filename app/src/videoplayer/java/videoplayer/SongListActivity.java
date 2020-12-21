package videoplayer;

import android.content.Intent;
import android.net.Uri;

import com.smile.karaokeplayer.BaseSongListActivity;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.SongInfo;

import java.util.ArrayList;

import videoplayer.utilities.FileSelectUtil;
import videoplayer.utilities.UriUtil;

public class SongListActivity extends BaseSongListActivity {

    @Override
    public void selectOneFileToAddSongList(int requestCode) {
        Intent addIntent = new Intent(this, SongDataActivity.class);
        addIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.AddActionString);
        startActivityForResult(addIntent, requestCode);
    }

    @Override
    public void selectMultipleFileToAddSongList(int requestCode) {
        FileSelectUtil.selectFileToOpen(this, requestCode, false);
    }

    @Override
    public ArrayList<Uri> getUrisListFromIntentSongList(Intent data) {
        return UriUtil.getUrisListFromIntent(this, data);
    }

    @Override
    public void editOneSongFromSongList(SongInfo singleSongInfo, int requestCode) {
        Intent editIntent = new Intent(this, SongDataActivity.class);
        editIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.EditActionString);
        editIntent.putExtra(PlayerConstants.SongInfoState, singleSongInfo);
        startActivityForResult(editIntent, requestCode);
    }

    @Override
    public void deleteOneSongFromSongList(SongInfo singleSongInfo, int requestCode) {
        Intent deleteIntent = new Intent(this, SongDataActivity.class);
        deleteIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.DeleteActionString);
        deleteIntent.putExtra(PlayerConstants.SongInfoState, singleSongInfo);
        startActivityForResult(deleteIntent, requestCode);
    }
}
