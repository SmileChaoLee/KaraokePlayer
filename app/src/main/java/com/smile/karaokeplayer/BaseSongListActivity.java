package com.smile.karaokeplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BaseSongListActivity extends AppCompatActivity {

    private static final String TAG = "BaseSongListActivity";

    private SongListSQLite songListSQLite;
    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    private ArrayList<SongInfo> mSongList = new ArrayList<>();
    private ListView songListView = null;
    private MySongListAdapter mySongListAdapter;

    public abstract Intent createIntentFromSongDataActivity();
    public abstract Intent createSelectFilesToOpenIntent();
    public abstract ArrayList<Uri> getUrisListFromIntentSongList(Intent data);
    public abstract void setAudioLinearLayoutVisibility(LinearLayout linearLayout);
    public abstract Intent createPlayerActivityIntent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.8f * textFontSize;

        songListSQLite = new SongListSQLite(getApplicationContext());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_song_list);

        TextView songListStringTextView = findViewById(R.id.songListStringTextView);
        ScreenUtil.resizeTextSize(songListStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);

        Button addOneSongListButton = findViewById(R.id.addOneSongListButton);
        ScreenUtil.resizeTextSize(addOneSongListButton, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        addOneSongListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOneSongToAddSongList();
            }
        });

        Button addSongsListButton = findViewById(R.id.addSongsListButton);
        ScreenUtil.resizeTextSize(addSongsListButton, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        addSongsListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectMultipleFileToAddSongList();
            }
        });

        Button exitSongListButton = (Button)findViewById(R.id.exitSongListButton);
        ScreenUtil.resizeTextSize(exitSongListButton, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        exitSongListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToPrevious();
            }
        });

        mSongList = songListSQLite.readSongList();

        mySongListAdapter = new MySongListAdapter(this, R.layout.song_list_item, mSongList);
        mySongListAdapter.setNotifyOnChange(false);

        songListView = findViewById(R.id.songListListView);
        songListView.setAdapter(mySongListAdapter);
        songListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {

            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (songListSQLite != null) {
            songListSQLite.closeDatabase();
            songListSQLite = null;
        }
    }

    @Override
    public void onBackPressed() {
        returnToPrevious();
    }

    private void returnToPrevious() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    private void addOneSongToAddSongList() {
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        updateSongList();
                    }
                });

        Intent addIntent = createIntentFromSongDataActivity();
        addIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.AddActionString);
        activityResultLauncher.launch(addIntent);
    }

    private void editOneSongFromSongList(SongInfo singleSongInfo) {
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        updateSongList();
                    }
                });
        Intent editIntent = createIntentFromSongDataActivity();
        editIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.EditActionString);
        editIntent.putExtra(PlayerConstants.SongInfoState, singleSongInfo);
        activityResultLauncher.launch(editIntent);
    }

    private void deleteOneSongFromSongList(SongInfo singleSongInfo) {
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        updateSongList();
                    }
                });
        Intent deleteIntent = createIntentFromSongDataActivity();
        deleteIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.DeleteActionString);
        deleteIntent.putExtra(PlayerConstants.SongInfoState, singleSongInfo);
        activityResultLauncher.launch(deleteIntent);
    }

    private void selectMultipleFileToAddSongList() {
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result == null) {
                            return;
                        }
                        int resultCode = result.getResultCode();
                        if (resultCode == Activity.RESULT_OK) {
                            Intent data= result.getData();
                            addMultipleSongsToPlaylist(data);
                        }
                    }
                });
        Intent selectFileIntent = createSelectFilesToOpenIntent();

        activityResultLauncher.launch(selectFileIntent);
    }

    protected void addMultipleSongsToPlaylist(Intent data) {
        ArrayList<Uri> uris = getUrisListFromIntentSongList(data);
        // this activity allows only one file selected
        if (uris.size()>0) {
            // There are files selected
            SongInfo mSongInfo;
            String uriString;
            for (int i=0; i<uris.size(); i++) {
                Uri uri = uris.get(i);
                if (uri!=null && !Uri.EMPTY.equals(uri)) {
                    uriString = uri.toString();
                    // check if this file is already in database
                    if (songListSQLite.findOneSongByUriString(uriString) == null) {
                        // not exist
                        mSongInfo = new SongInfo();
                        mSongInfo.setSongName("");
                        mSongInfo.setFilePath(uriString);
                        mSongInfo.setMusicTrackNo(1);   // guess
                        mSongInfo.setMusicChannel(CommonConstants.RightChannel);    // guess
                        mSongInfo.setVocalTrackNo(1);   // guess
                        mSongInfo.setVocalChannel(CommonConstants.LeftChannel); // guess
                        mSongInfo.setIncluded("1"); // guess
                        songListSQLite.addSongToSongList(mSongInfo);
                    }
                }
            }
            songListSQLite.closeDatabase();
            ScreenUtil.showToast(this, getString(R.string.guessedAudioTrackValue)
                    , toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_LONG);
        }

        updateSongList();
    }

    protected void updateSongList() {
        Log.d(TAG, "updateSongList()");
        mSongList = songListSQLite.readSongList();
        mySongListAdapter.updateData(mSongList);    // update the UI
    }

    private class MySongListAdapter extends ArrayAdapter {

        private final float itemTextSize = textFontSize * 0.6f;
        private final float buttonTextSize = textFontSize * 0.8f;
        private final int yellow2Color;
        private final int yellow3Color;

        private final Context context;
        private final int layoutId;
        private ArrayList<SongInfo> mSongListInAdapter;

        @SuppressWarnings("unchecked")
        MySongListAdapter(Context context, int layoutId, ArrayList<SongInfo> _songList) {
            super(context, layoutId, _songList);
            this.context = context;
            this.layoutId = layoutId;
            this.mSongListInAdapter = _songList;
            yellow2Color = ContextCompat.getColor(this.context, R.color.yellow2);
            yellow3Color = ContextCompat.getColor(this.context, R.color.yellow3);
        }

        @Nullable
        @Override
        public Object getItem(int position) {
            return super.getItem(position);
        }

        @SuppressWarnings("unchecked")
        @Override
        public int getPosition(@Nullable Object item) {
            return super.getPosition(item);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            View view = getLayoutInflater().inflate(layoutId, parent,false);

            if (getCount() == 0) {
                return view;
            }

            if ( (position % 2) == 0) {
                view.setBackgroundColor(yellow2Color);
            } else {
                view.setBackgroundColor(yellow3Color);
            }

            final TextView titleStringTextView = view.findViewById(R.id.titleStringTextView);
            ScreenUtil.resizeTextSize(titleStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            final TextView titleNameTextView = view.findViewById(R.id.titleNameTextView);
            ScreenUtil.resizeTextSize(titleNameTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);

            final TextView filePathStringTextView = view.findViewById(R.id.filePathStringTextView);
            ScreenUtil.resizeTextSize(filePathStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            final TextView filePathTextView = view.findViewById(R.id.filePathTextView);
            ScreenUtil.resizeTextSize(filePathTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            //
            final LinearLayout audioMusicLinearLayout = view.findViewById(R.id.audioMusicLinearLayout);
            //
            final TextView musicTrackStringTextView = view.findViewById(R.id.musicTrackStringTextView);
            ScreenUtil.resizeTextSize(musicTrackStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            final TextView musicTrackTextView = view.findViewById(R.id.musicTrackTextView);
            ScreenUtil.resizeTextSize(musicTrackTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);

            final TextView musicChannelStringTextView = view.findViewById(R.id.musicChannelStringTextView);
            ScreenUtil.resizeTextSize(musicChannelStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            final TextView musicChannelTextView = view.findViewById(R.id.musicChannelTextView);
            ScreenUtil.resizeTextSize(musicChannelTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            //
            final LinearLayout audioVocalLinearLayout = view.findViewById(R.id.audioVocalLinearLayout);
            //
            final TextView vocalTrackStringTextView = view.findViewById(R.id.vocalTrackStringTextView);
            ScreenUtil.resizeTextSize(vocalTrackStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            final TextView vocalTrackTextView = view.findViewById(R.id.vocalTrackTextView);
            ScreenUtil.resizeTextSize(vocalTrackTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);

            final TextView vocalChannelStringTextView = view.findViewById(R.id.vocalChannelStringTextView);
            ScreenUtil.resizeTextSize(vocalChannelStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            final TextView vocalChannelTextView = view.findViewById(R.id.vocalChannelTextView);
            ScreenUtil.resizeTextSize(vocalChannelTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            //
            setAudioLinearLayoutVisibility(audioMusicLinearLayout); // abstract method
            setAudioLinearLayoutVisibility(audioVocalLinearLayout);
            //
            final TextView includedPlaylistStringTextView = view.findViewById(R.id.includedPlaylistStringTextView);
            ScreenUtil.resizeTextSize(includedPlaylistStringTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            final CheckBox includedPlaylistCheckBox = view.findViewById(R.id.includedPlaylistCheckBox);
            ScreenUtil.resizeTextSize(includedPlaylistCheckBox, itemTextSize, ScreenUtil.FontSize_Pixel_Type);

            final Button editSongListButton = view.findViewById(R.id.editSongListButton);
            ScreenUtil.resizeTextSize(editSongListButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type);

            final Button deleteSongListButton = view.findViewById(R.id.deleteSongListButton);
            ScreenUtil.resizeTextSize(deleteSongListButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type);

            final Button playSongListButton = view.findViewById(R.id.playSongListButton);
            ScreenUtil.resizeTextSize(playSongListButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type);

            int songListSize = 0;
            if (mSongListInAdapter != null) {
                songListSize = mSongListInAdapter.size();
            }
            if (songListSize > 0) {
                final SongInfo singleSongInfo = mSongListInAdapter.get(position);

                titleNameTextView.setText(singleSongInfo.getSongName());
                filePathTextView.setText(singleSongInfo.getFilePath());
                musicTrackTextView.setText(String.valueOf(singleSongInfo.getMusicTrackNo()));
                musicChannelTextView.setText(BaseApplication.audioChannelMap.get(singleSongInfo.getMusicChannel()));
                vocalTrackTextView.setText(String.valueOf(singleSongInfo.getVocalTrackNo()));
                vocalChannelTextView.setText(BaseApplication.audioChannelMap.get(singleSongInfo.getVocalChannel()));

                boolean inPlaylist = (singleSongInfo.getIncluded().equals("1"));
                includedPlaylistCheckBox.setChecked(inPlaylist);
                includedPlaylistCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        includedPlaylistCheckBox.setChecked(isChecked);
                        includedPlaylistCheckBox.jumpDrawablesToCurrentState();
                        String included = isChecked ? "1" : "0";
                        singleSongInfo.setIncluded(included);
                        songListSQLite.updateOneSongFromSongList(singleSongInfo);
                    }
                });

                editSongListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editOneSongFromSongList(singleSongInfo);
                    }
                });

                deleteSongListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteOneSongFromSongList(singleSongInfo);
                    }
                });

                playSongListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // play this item (media file)
                        Intent callingIntent = getIntent(); // from ExoPlayActivity or some Activity (like VLC)
                        Log.d(TAG, "playSongListButton()-->callingIntent = " + callingIntent);
                        if (callingIntent != null) {
                            ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                                    new ActivityResultCallback<ActivityResult>() {
                                        @Override
                                        public void onActivityResult(ActivityResult result) {
                                            Log.d(TAG, "ActivityResultCallback()-->updateSongList()");
                                            updateSongList();
                                        }
                                    });
                            Log.d(TAG, "playSongListButton()-->createPlayerActivityIntent");
                            Intent playerActivityIntent = createPlayerActivityIntent();
                            Bundle extras = new Bundle();
                            extras.putBoolean(PlayerConstants.IsPlaySingleSongState, true);   // play single song
                            extras.putParcelable(PlayerConstants.SongInfoState, singleSongInfo);
                            playerActivityIntent.putExtras(extras);
                            Log.d(TAG, "playSongListButton()-->activityResultLauncher.launch(playerActivityIntent)");
                            activityResultLauncher.launch(playerActivityIntent);
                        }
                    }
                });
            }

            return view;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void addAll(@NonNull Collection collection) {
            super.addAll(collection);
        }

        public void updateData(List newData) {
            clear();
            addAll(newData);
            notifyDataSetChanged();
        }
    }
}
