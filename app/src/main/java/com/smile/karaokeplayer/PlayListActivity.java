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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;


public class PlayListActivity extends AppCompatActivity {

    private static final String TAG = ".PlayListActivity";
    private ArrayList<SongInfo> songList = new ArrayList<>();
    private float textFontSize;
    private float fontScale;
    private ListView listView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);

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

        songList = new ArrayList<>();

        listView = findViewById(R.id.playListListView);
        listView.setAdapter(new myListAdapter(this, R.layout.play_list_item, songList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {

            }
        });
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

        private int layoutId;
        private ArrayList<SongInfo> mSongList;

        @SuppressWarnings("unchecked")
        myListAdapter(Context context, int layoutId, ArrayList<SongInfo> _songList) {
            super(context, layoutId, _songList);
            this.layoutId = layoutId;
            this.mSongList = _songList;
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

            TextView titleStringTextView = view.findViewById(R.id.titleStringTextView);
            ScreenUtil.resizeTextSize(titleStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
            TextView titleNameTextView = view.findViewById(R.id.titleNameTextView);
            ScreenUtil.resizeTextSize(titleNameTextView, textFontSize, SmileApplication.FontSize_Scale_Type);

            TextView filePathStringTextView = view.findViewById(R.id.filePathStringTextView);
            ScreenUtil.resizeTextSize(filePathStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
            TextView filePathTextView = view.findViewById(R.id.filePathTextView);
            ScreenUtil.resizeTextSize(filePathTextView, textFontSize, SmileApplication.FontSize_Scale_Type);

            TextView musicTrackStringTextView = view.findViewById(R.id.musicTrackStringTextView);
            ScreenUtil.resizeTextSize(musicTrackStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
            TextView musicTrackTextView = view.findViewById(R.id.musicTrackTextView);
            ScreenUtil.resizeTextSize(musicTrackTextView, textFontSize, SmileApplication.FontSize_Scale_Type);

            TextView musicChannelStringTextView = view.findViewById(R.id.musicChannelStringTextView);
            ScreenUtil.resizeTextSize(musicChannelStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
            TextView musicChannelTextView = view.findViewById(R.id.musicChannelTextView);
            ScreenUtil.resizeTextSize(musicChannelTextView, textFontSize, SmileApplication.FontSize_Scale_Type);

            TextView vocalTrackStringTextView = view.findViewById(R.id.vocalTrackStringTextView);
            ScreenUtil.resizeTextSize(vocalTrackStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
            TextView vocalTrackTextView = view.findViewById(R.id.vocalTrackTextView);
            ScreenUtil.resizeTextSize(vocalTrackTextView, textFontSize, SmileApplication.FontSize_Scale_Type);

            TextView vocalChannelStringTextView = view.findViewById(R.id.vocalChannelStringTextView);
            ScreenUtil.resizeTextSize(vocalChannelStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
            TextView vocalChannelTextView = view.findViewById(R.id.vocalChannelTextView);
            ScreenUtil.resizeTextSize(vocalChannelTextView, textFontSize, SmileApplication.FontSize_Scale_Type);

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
                musicTrackTextView.setText(songInfo.getMusicTrackNo());

                musicChannelStringTextView.setText(getString(R.string.musicChannelWithColonString));
                musicChannelTextView.setText(songInfo.getMusicChannel());

                vocalTrackStringTextView.setText(getString(R.string.vocalTrackWithColonString));
                vocalTrackTextView.setText(songInfo.getVocalTrackNo());

                vocalChannelStringTextView.setText(getString(R.string.vocalChannelWithColonString));
                vocalChannelTextView.setText(songInfo.getVocalChannel());
            }

            return view;
        }
    }
}
