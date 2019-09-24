package com.smile.karaokeplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.smile.karaokeplayer.Models.PlayListSQLite;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;
import java.util.List;


public class PlayListActivity extends AppCompatActivity {

    private static final String TAG = ".PlayListActivity";
    private static final int ADD_ONE_SONG_TO_PLAY_LIST = 1;
    private static final int EDIT_ONE_SONG_TO_PLAY_LIST = 2;
    private static final int DELETE_ONE_SONG_TO_PLAY_LIST = 3;
    private static final int PLAY_ONE_SONG_IN_PLAY_LIST = 4;

    private PlayListSQLite playListSQLite;
    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    private ArrayList<SongInfo> mPlayList = new ArrayList<>();
    private ListView playListView = null;
    private MyPlayListAdapter myPlayListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);
        toastTextSize = 0.8f * textFontSize;

        playListSQLite = new PlayListSQLite(SmileApplication.AppContext);

        /*
        playListSQLite.deleteAllPlayList();

        String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        String filePath = externalPath + "/Song/perfume_h264.mp4";
        SongInfo songInfo = new SongInfo(1, "香水", filePath, 1, SmileApplication.leftChannel, 1, SmileApplication.rightChannel);
        playListSQLite.addSongToPlayList(songInfo);

        filePath = externalPath + "/Song/saving_all_my_love_for_you.flv";
        songInfo = new SongInfo(2, "Saving All My Love For You", filePath, 1, SmileApplication.leftChannel, 1, SmileApplication.rightChannel);
        playListSQLite.addSongToPlayList(songInfo);
        */

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_list);

        TextView playListStringTextView = findViewById(R.id.playListStringTextView);
        ScreenUtil.resizeTextSize(playListStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        playListStringTextView.setText(getString(R.string.playListString));

        Button addPlaiListButton = (Button)findViewById(R.id.addPlayListButton);
        ScreenUtil.resizeTextSize(addPlaiListButton, textFontSize, SmileApplication.FontSize_Scale_Type);
        addPlaiListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addIntent = new Intent(PlayListActivity.this, SongDataActivity.class);
                addIntent.putExtra(SmileApplication.CrudActionString, SmileApplication.AddActionString);
                startActivityForResult(addIntent, ADD_ONE_SONG_TO_PLAY_LIST);
            }
        });

        Button exitPlayListButton = (Button)findViewById(R.id.exitPlayListButton);
        ScreenUtil.resizeTextSize(exitPlayListButton, textFontSize, SmileApplication.FontSize_Scale_Type);
        exitPlayListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToPrevious();
            }
        });

        mPlayList = playListSQLite.readPlayList();

        myPlayListAdapter = new MyPlayListAdapter(this, R.layout.play_list_item, mPlayList);
        myPlayListAdapter.setNotifyOnChange(false);

        playListView = findViewById(R.id.playListListView);
        playListView.setAdapter(myPlayListAdapter);
        playListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playListSQLite != null) {
            playListSQLite.closeDatabase();
            playListSQLite = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case ADD_ONE_SONG_TO_PLAY_LIST:
                    break;
                case EDIT_ONE_SONG_TO_PLAY_LIST:
                    break;
                case DELETE_ONE_SONG_TO_PLAY_LIST:
                    break;
                case PLAY_ONE_SONG_IN_PLAY_LIST:
                    break;
            }
        }
        mPlayList = playListSQLite.readPlayList();
        myPlayListAdapter.updateData(mPlayList);    // update the UI
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

    private class MyPlayListAdapter extends ArrayAdapter {

        private Context context;
        private int layoutId;
        private ArrayList<SongInfo> mPlayListInAdapter;
        private float itemTextSize = textFontSize * 0.6f;
        private float buttonTextSize = textFontSize * 0.8f;
        private int yellow2Color;
        private int yellow3Color;

        @SuppressWarnings("unchecked")
        MyPlayListAdapter(Context context, int layoutId, ArrayList<SongInfo> _playList) {
            super(context, layoutId, _playList);
            this.context = context;
            this.layoutId = layoutId;
            this.mPlayListInAdapter = _playList;
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
            TextView titleStringTextView = view.findViewById(R.id.titleStringTextView);
            ScreenUtil.resizeTextSize(titleStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            TextView titleNameTextView = view.findViewById(R.id.titleNameTextView);
            ScreenUtil.resizeTextSize(titleNameTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            TextView filePathStringTextView = view.findViewById(R.id.filePathStringTextView);
            ScreenUtil.resizeTextSize(filePathStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            TextView filePathTextView = view.findViewById(R.id.filePathTextView);
            ScreenUtil.resizeTextSize(filePathTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            TextView musicTrackStringTextView = view.findViewById(R.id.musicTrackStringTextView);
            ScreenUtil.resizeTextSize(musicTrackStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            TextView musicTrackTextView = view.findViewById(R.id.musicTrackTextView);
            ScreenUtil.resizeTextSize(musicTrackTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            TextView musicChannelStringTextView = view.findViewById(R.id.musicChannelStringTextView);
            ScreenUtil.resizeTextSize(musicChannelStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            TextView musicChannelTextView = view.findViewById(R.id.musicChannelTextView);
            ScreenUtil.resizeTextSize(musicChannelTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            TextView vocalTrackStringTextView = view.findViewById(R.id.vocalTrackStringTextView);
            ScreenUtil.resizeTextSize(vocalTrackStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            TextView vocalTrackTextView = view.findViewById(R.id.vocalTrackTextView);
            ScreenUtil.resizeTextSize(vocalTrackTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            TextView vocalChannelStringTextView = view.findViewById(R.id.vocalChannelStringTextView);
            ScreenUtil.resizeTextSize(vocalChannelStringTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);
            TextView vocalChannelTextView = view.findViewById(R.id.vocalChannelTextView);
            ScreenUtil.resizeTextSize(vocalChannelTextView, itemTextSize, SmileApplication.FontSize_Scale_Type);

            Button editPlayListButton = view.findViewById(R.id.editPlayListButton);
            ScreenUtil.resizeTextSize(editPlayListButton, buttonTextSize, SmileApplication.FontSize_Scale_Type);

            Button deletePlayListButton = view.findViewById(R.id.deletePlayListButton);
            ScreenUtil.resizeTextSize(deletePlayListButton, buttonTextSize, SmileApplication.FontSize_Scale_Type);

            int songListSize = 0;
            if (mPlayListInAdapter != null) {
                songListSize = mPlayListInAdapter.size();
            }
            if (songListSize > 0) {
                final SongInfo songInfo = mPlayListInAdapter.get(position);

                titleStringTextView.setText(getString(R.string.titleWithColonString));
                titleNameTextView.setText(songInfo.getSongName());

                filePathStringTextView.setText(getString(R.string.filePathWithColonString));
                filePathTextView.setText(songInfo.getFilePath());

                musicTrackStringTextView.setText(getString(R.string.musicTrackWithColonString));
                musicTrackTextView.setText(String.valueOf(songInfo.getMusicTrackNo()));

                musicChannelStringTextView.setText(getString(R.string.musicChannelWithColonString));
                musicChannelTextView.setText(SmileApplication.audioChannelMap.get(songInfo.getMusicChannel()));

                vocalTrackStringTextView.setText(getString(R.string.vocalTrackWithColonString));
                vocalTrackTextView.setText(String.valueOf(songInfo.getVocalTrackNo()));

                vocalChannelStringTextView.setText(getString(R.string.vocalChannelWithColonString));
                vocalChannelTextView.setText(SmileApplication.audioChannelMap.get(songInfo.getVocalChannel()));

                editPlayListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent editIntent = new Intent(context, SongDataActivity.class);
                        editIntent.putExtra(SmileApplication.CrudActionString, SmileApplication.EditActionString);
                        editIntent.putExtra("SongInfo", songInfo);
                        startActivityForResult(editIntent, EDIT_ONE_SONG_TO_PLAY_LIST);
                    }
                });

                deletePlayListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent deleteIntent = new Intent(context, SongDataActivity.class);
                        deleteIntent.putExtra(SmileApplication.CrudActionString, SmileApplication.DeleteActionString);
                        deleteIntent.putExtra("SongInfo", songInfo);
                        startActivityForResult(deleteIntent, DELETE_ONE_SONG_TO_PLAY_LIST);
                    }
                });
            }

            return view;
        }

        public void updateData(List newData) {
            clear();
            addAll(newData);
            notifyDataSetChanged();
        }
    }
}
