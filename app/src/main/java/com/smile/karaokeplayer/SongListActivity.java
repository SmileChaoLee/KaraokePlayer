package com.smile.karaokeplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;
import com.smile.smilelibraries.showing_instertitial_ads_utility.ShowingInterstitialAdsUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class SongListActivity extends AppCompatActivity {

    private static final String TAG = ".SongListActivity";
    private static final int ADD_ONE_SONG_TO_PLAY_LIST = 1;
    private static final int EDIT_ONE_SONG_TO_PLAY_LIST = 2;
    private static final int DELETE_ONE_SONG_TO_PLAY_LIST = 3;
    private static final int PLAY_ONE_SONG_IN_PLAY_LIST = 4;

    private SongListSQLite songListSQLite;
    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    private ArrayList<SongInfo> mSongList = new ArrayList<>();
    private ListView songListView = null;
    private MySongListAdapter mySongListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);
        toastTextSize = 0.8f * textFontSize;

        Intent callingIntent = getIntent();

        songListSQLite = new SongListSQLite(SmileApplication.AppContext);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_song_list);

        TextView songListStringTextView = findViewById(R.id.songListStringTextView);
        ScreenUtil.resizeTextSize(songListStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);

        Button addSongListButton = (Button)findViewById(R.id.addSongListButton);
        ScreenUtil.resizeTextSize(addSongListButton, textFontSize, SmileApplication.FontSize_Scale_Type);
        addSongListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addIntent = new Intent(SongListActivity.this, SongDataActivity.class);
                addIntent.putExtra(SmileApplication.CrudActionString, SmileApplication.AddActionString);
                startActivityForResult(addIntent, ADD_ONE_SONG_TO_PLAY_LIST);
            }
        });

        Button exitSongListButton = (Button)findViewById(R.id.exitSongListButton);
        ScreenUtil.resizeTextSize(exitSongListButton, textFontSize, SmileApplication.FontSize_Scale_Type);
        exitSongListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdAndExitActivity();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // final SongInfo songInfo = data.getParcelableExtra("SongInfo");
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case ADD_ONE_SONG_TO_PLAY_LIST:
                    break;
                case EDIT_ONE_SONG_TO_PLAY_LIST:
                    break;
                case DELETE_ONE_SONG_TO_PLAY_LIST:
                    break;
                case PLAY_ONE_SONG_IN_PLAY_LIST:
                    if (SmileApplication.InterstitialAd != null) {
                        // free version
                        Log.d(TAG, "onActivityResult() --> Starting to show Ads");
                        int entryPoint = 0; //  no used
                        ShowingInterstitialAdsUtil.ShowAdAsyncTask showAdAsyncTask =
                                SmileApplication.InterstitialAd.new ShowAdAsyncTask(this
                                        , entryPoint
                                        , new ShowingInterstitialAdsUtil.AfterDismissFunctionOfShowAd() {
                                    @Override
                                    public void executeAfterDismissAds(int endPoint) {
                                        // do nothing
                                    }
                                });
                        showAdAsyncTask.execute();
                    }
                    break;
            }
        }
        mSongList = songListSQLite.readSongList();
        mySongListAdapter.updateData(mSongList);    // update the UI
    }


    @Override
    public void onBackPressed() {
        showAdAndExitActivity();
    }

    private void showAdAndExitActivity() {
        if (SmileApplication.InterstitialAd != null) {
            // free version
            Log.d(TAG, "showAdAndExitActivity() --> Starting to show Ads");
            int entryPoint = 0; //  no used
            ShowingInterstitialAdsUtil.ShowAdAsyncTask showAdAsyncTask =
                    SmileApplication.InterstitialAd.new ShowAdAsyncTask(this
                            , entryPoint
                            , new ShowingInterstitialAdsUtil.AfterDismissFunctionOfShowAd() {
                        @Override
                        public void executeAfterDismissAds(int endPoint) {
                            returnToPrevious();
                        }
                    });
            showAdAsyncTask.execute();
        } else {
            returnToPrevious();
        }
    }

    private void returnToPrevious() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
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
            ScreenUtil.resizeTextSize(titleStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            final TextView titleNameTextView = view.findViewById(R.id.titleNameTextView);
            ScreenUtil.resizeTextSize(titleNameTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            final TextView filePathStringTextView = view.findViewById(R.id.filePathStringTextView);
            ScreenUtil.resizeTextSize(filePathStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            final TextView filePathTextView = view.findViewById(R.id.filePathTextView);
            ScreenUtil.resizeTextSize(filePathTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            final TextView musicTrackStringTextView = view.findViewById(R.id.musicTrackStringTextView);
            ScreenUtil.resizeTextSize(musicTrackStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            final TextView musicTrackTextView = view.findViewById(R.id.musicTrackTextView);
            ScreenUtil.resizeTextSize(musicTrackTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            final TextView musicChannelStringTextView = view.findViewById(R.id.musicChannelStringTextView);
            ScreenUtil.resizeTextSize(musicChannelStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            final TextView musicChannelTextView = view.findViewById(R.id.musicChannelTextView);
            ScreenUtil.resizeTextSize(musicChannelTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            final TextView vocalTrackStringTextView = view.findViewById(R.id.vocalTrackStringTextView);
            ScreenUtil.resizeTextSize(vocalTrackStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            final TextView vocalTrackTextView = view.findViewById(R.id.vocalTrackTextView);
            ScreenUtil.resizeTextSize(vocalTrackTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            final TextView vocalChannelStringTextView = view.findViewById(R.id.vocalChannelStringTextView);
            ScreenUtil.resizeTextSize(vocalChannelStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            final TextView vocalChannelTextView = view.findViewById(R.id.vocalChannelTextView);
            ScreenUtil.resizeTextSize(vocalChannelTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            final TextView includedPlaylistStringTextView = view.findViewById(R.id.includedPlaylistStringTextView);
            ScreenUtil.resizeTextSize(includedPlaylistStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            final CheckBox includedPlaylistCheckBox = view.findViewById(R.id.includedPlaylistCheckBox);
            ScreenUtil.resizeTextSize(includedPlaylistCheckBox, itemTextSize, SmileApplication.FontSize_Scale_Type);

            final Button editSongListButton = view.findViewById(R.id.editSongListButton);
            ScreenUtil.resizeTextSize(editSongListButton, buttonTextSize, SmileApplication.FontSize_Scale_Type);

            final Button deleteSongListButton = view.findViewById(R.id.deleteSongListButton);
            ScreenUtil.resizeTextSize(deleteSongListButton, buttonTextSize, SmileApplication.FontSize_Scale_Type);

            final Button playSongListButton = view.findViewById(R.id.playSongListButton);
            ScreenUtil.resizeTextSize(playSongListButton, buttonTextSize, SmileApplication.FontSize_Scale_Type);

            int songListSize = 0;
            if (mSongListInAdapter != null) {
                songListSize = mSongListInAdapter.size();
            }
            if (songListSize > 0) {
                final SongInfo songInfo = mSongListInAdapter.get(position);

                titleNameTextView.setText(songInfo.getSongName());
                filePathTextView.setText(songInfo.getFilePath());
                musicTrackTextView.setText(String.valueOf(songInfo.getMusicTrackNo()));
                musicChannelTextView.setText(SmileApplication.audioChannelMap.get(songInfo.getMusicChannel()));
                vocalTrackTextView.setText(String.valueOf(songInfo.getVocalTrackNo()));
                vocalChannelTextView.setText(SmileApplication.audioChannelMap.get(songInfo.getVocalChannel()));

                boolean inPlaylist = (songInfo.getIncluded().equals("1"));
                includedPlaylistCheckBox.setChecked(inPlaylist);
                includedPlaylistCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        includedPlaylistCheckBox.setChecked(isChecked);
                        includedPlaylistCheckBox.jumpDrawablesToCurrentState();
                        String included = isChecked ? "1" : "0";
                        songInfo.setIncluded(included);
                        songListSQLite.updateOneSongFromSongList(songInfo);
                    }
                });

                editSongListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent editIntent = new Intent(context, SongDataActivity.class);
                        editIntent.putExtra(SmileApplication.CrudActionString, SmileApplication.EditActionString);
                        editIntent.putExtra("SongInfo", songInfo);
                        startActivityForResult(editIntent, EDIT_ONE_SONG_TO_PLAY_LIST);
                    }
                });

                deleteSongListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent deleteIntent = new Intent(context, SongDataActivity.class);
                        deleteIntent.putExtra(SmileApplication.CrudActionString, SmileApplication.DeleteActionString);
                        deleteIntent.putExtra("SongInfo", songInfo);
                        startActivityForResult(deleteIntent, DELETE_ONE_SONG_TO_PLAY_LIST);
                    }
                });

                playSongListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // play this item (media file)
                        Intent playOneSongIntent = new Intent(getApplicationContext(), PlaySingleSongActivity.class);
                        Bundle extras = new Bundle();
                        extras.putParcelable("SongInfo", songInfo);
                        playOneSongIntent.putExtras(extras);
                        startActivityForResult(playOneSongIntent, PLAY_ONE_SONG_IN_PLAY_LIST);
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
