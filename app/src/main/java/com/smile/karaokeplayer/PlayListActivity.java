package com.smile.karaokeplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
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


public class PlayListActivity extends AppCompatActivity {

    private static final String TAG = ".PlayListActivity";
    private PlayListSQLite playListSQLite;
    private float textFontSize;
    private float fontScale;
    private float toastTextSize;
    private ArrayList<SongInfo> songList = new ArrayList<>();
    private ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);
        toastTextSize = 0.8f * textFontSize;

        playListSQLite = new PlayListSQLite(this);
        playListSQLite.deleteAllPlayList();

        String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        String filePath = externalPath + "/Song/perfume_h264.mp4";
        SongInfo songInfo = new SongInfo("000001", "香水", filePath, 0, SmileApplication.leftChannel, 0, SmileApplication.rightChannel);
        playListSQLite.addSongToPlayList(songInfo);

        filePath = externalPath + "/Song/saving_all_my_love_for_you.flv";
        songInfo = new SongInfo("000002", "Saving All My Love For You", filePath, 0, SmileApplication.leftChannel, 0, SmileApplication.rightChannel);
        playListSQLite.addSongToPlayList(songInfo);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_list);

        TextView playListStringTextView = findViewById(R.id.playListStringTextView);
        ScreenUtil.resizeTextSize(playListStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        playListStringTextView.setText(getString(R.string.playListString));

        Button okButton = (Button)findViewById(R.id.playListOkButton);
        ScreenUtil.resizeTextSize(okButton, textFontSize, SmileApplication.FontSize_Scale_Type);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToPrevious();
            }
        });

        songList = playListSQLite.readPlayList();

        listView = findViewById(R.id.playListListView);
        listView.setAdapter(new myListAdapter(this, R.layout.play_list_item, songList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
    public void onBackPressed() {
        returnToPrevious();
    }

    private void returnToPrevious() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    private class myListAdapter extends ArrayAdapter {  // changed name to MyListAdapter from myListAdapter

        private Context context;
        private int layoutId;
        private ArrayList<SongInfo> mSongList;
        private float listViewTextSize = textFontSize * 0.5f;
        private int yellow2Color;
        private int yellow3Color;

        @SuppressWarnings("unchecked")
        myListAdapter(Context context, int layoutId, ArrayList<SongInfo> _songList) {
            super(context, layoutId, _songList);
            this.context = context;
            this.layoutId = layoutId;
            this.mSongList = _songList;
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

            int listViewHeight = parent.getHeight();
            int itemNum = 5;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                itemNum = 3;
            }
            int itemHeight = listViewHeight / itemNum;    // items for one screen
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = itemHeight;
            // view.setLayoutParams(layoutParams);  // no needed

            if ( (position % 2) == 0) {
                view.setBackgroundColor(yellow2Color);
            } else {
                view.setBackgroundColor(yellow3Color);
            }
            TextView titleStringTextView = view.findViewById(R.id.titleStringTextView);
            ScreenUtil.resizeTextSize(titleStringTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);
            TextView titleNameTextView = view.findViewById(R.id.titleNameTextView);
            ScreenUtil.resizeTextSize(titleNameTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);

            TextView filePathStringTextView = view.findViewById(R.id.filePathStringTextView);
            ScreenUtil.resizeTextSize(filePathStringTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);
            TextView filePathTextView = view.findViewById(R.id.filePathTextView);
            ScreenUtil.resizeTextSize(filePathTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);

            TextView musicTrackStringTextView = view.findViewById(R.id.musicTrackStringTextView);
            ScreenUtil.resizeTextSize(musicTrackStringTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);
            TextView musicTrackTextView = view.findViewById(R.id.musicTrackTextView);
            ScreenUtil.resizeTextSize(musicTrackTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);

            TextView musicChannelStringTextView = view.findViewById(R.id.musicChannelStringTextView);
            ScreenUtil.resizeTextSize(musicChannelStringTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);
            TextView musicChannelTextView = view.findViewById(R.id.musicChannelTextView);
            ScreenUtil.resizeTextSize(musicChannelTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);

            TextView vocalTrackStringTextView = view.findViewById(R.id.vocalTrackStringTextView);
            ScreenUtil.resizeTextSize(vocalTrackStringTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);
            TextView vocalTrackTextView = view.findViewById(R.id.vocalTrackTextView);
            ScreenUtil.resizeTextSize(vocalTrackTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);

            TextView vocalChannelStringTextView = view.findViewById(R.id.vocalChannelStringTextView);
            ScreenUtil.resizeTextSize(vocalChannelStringTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);
            TextView vocalChannelTextView = view.findViewById(R.id.vocalChannelTextView);
            ScreenUtil.resizeTextSize(vocalChannelTextView, listViewTextSize, SmileApplication.FontSize_Scale_Type);

            int songListSize = 0;
            if (mSongList != null) {
                songListSize = mSongList.size();
            }
            if (songListSize > 0) {
                SongInfo songInfo = mSongList.get(position);

                titleStringTextView.setText(getString(R.string.titleWithColonString));
                titleNameTextView.setText(songInfo.getSongName());

                filePathStringTextView.setText(getString(R.string.filePathWithColonString));
                filePathTextView.setText(songInfo.getFilePath());

                musicTrackStringTextView.setText(getString(R.string.musicTrackWithColonString));
                musicTrackTextView.setText(String.valueOf(songInfo.getMusicTrackNo()));

                musicChannelStringTextView.setText(getString(R.string.musicChannelWithColonString));
                musicChannelTextView.setText(String.valueOf(songInfo.getMusicChannel()));

                vocalTrackStringTextView.setText(getString(R.string.vocalTrackWithColonString));
                vocalTrackTextView.setText(String.valueOf(songInfo.getVocalTrackNo()));

                vocalChannelStringTextView.setText(getString(R.string.vocalChannelWithColonString));
                vocalChannelTextView.setText(String.valueOf(songInfo.getVocalChannel()));
            }

            return view;
        }
    }
}
