package com.smile.karaokeplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.smilelibraries.utilities.ScreenUtil;

public class SongDataActivity extends AppCompatActivity {

    private static final String TAG = new String(".SongDataActivity");
    private float textFontSize;
    private float fontScale;
    private float toastTextSize;

    private EditText edit_titleNameEditText;
    private EditText edit_filePathEditText;
    private EditText edit_musicTrackEditText;
    private EditText edit_musicChannelEditText;
    private EditText edit_vocalTrackEditText;
    private EditText vocalChannelEditText;

    private String actionStrong;
    private String action;;
    private SongInfo songInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() is called.");

        if (savedInstanceState == null) {
            Intent callingIntent = getIntent();
            action = callingIntent.getStringExtra("Action");
            switch (action.toUpperCase()) {
                case "ADD":
                    // add one record
                    songInfo = new SongInfo();
                    break;
                case "EDIT":
                    // = "EDIT". Edit one record
                    songInfo = callingIntent.getParcelableExtra("SongInfo");
                    break;
                case "DELETE":
                    // = "DELETE". Delete one record
                    songInfo = callingIntent.getParcelableExtra("SongInfo");
                    break;
            }
            Log.d(TAG, "savedInstanceState is null.");
        } else {
            // not null, has savedInstanceState
            action = savedInstanceState.getString("Action");
            songInfo = savedInstanceState.getParcelable("SongInfo");
            Log.d(TAG, "savedInstanceState is not null.");
        }

        if (action == null) {
            returnToPrevious();
            return;
        }
        switch (action.toUpperCase()) {
            case "ADD":
                // add one record
                actionStrong = getString(R.string.addString);
                break;
            case "EDIT":
                // = "EDIT". Edit one record
                actionStrong = getString(R.string.saveString);
                break;
            case "DELETE":
                // = "DELETE". Delete one record
                actionStrong = getString(R.string.deleteString);
                break;
            default:
                returnToPrevious();
                return;
        }
        if (songInfo == null) {
            returnToPrevious();
            return;
        }

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);
        textFontSize *= 0.8f;
        toastTextSize = 0.9f * textFontSize;

        setContentView(R.layout.activity_song_data);

        TextView edit_titleStringTextView = findViewById(R.id.edit_titleStringTextView);
        ScreenUtil.resizeTextSize(edit_titleStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_titleNameEditText = findViewById(R.id.edit_titleNameEditText);
        ScreenUtil.resizeTextSize(edit_titleNameEditText, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_titleNameEditText.setText(songInfo.getSongName());

        TextView edit_filePathStringTextView = findViewById(R.id.edit_filePathStringTextView);
        ScreenUtil.resizeTextSize(edit_filePathStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_filePathEditText = findViewById(R.id.edit_filePathEditText);
        ScreenUtil.resizeTextSize(edit_filePathEditText, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_filePathEditText.setText(songInfo.getFilePath());

        TextView edit_musicTrackStringTextView = findViewById(R.id.edit_musicTrackStringTextView);
        ScreenUtil.resizeTextSize(edit_musicTrackStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_musicTrackEditText = findViewById(R.id.edit_musicTrackEditText);
        ScreenUtil.resizeTextSize(edit_musicTrackEditText, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_musicTrackEditText.setText(String.valueOf(songInfo.getMusicTrackNo()));

        TextView edit_musicChannelStringTextView = findViewById(R.id.edit_musicChannelStringTextView);
        ScreenUtil.resizeTextSize(edit_musicChannelStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_musicChannelEditText = findViewById(R.id.edit_musicChannelEditText);
        ScreenUtil.resizeTextSize(edit_musicChannelEditText, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_musicChannelEditText.setText(String.valueOf(songInfo.getMusicChannel()));

        TextView edit_vocalTrackStringTextView = findViewById(R.id.edit_vocalTrackStringTextView);
        ScreenUtil.resizeTextSize(edit_vocalTrackStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_vocalTrackEditText = findViewById(R.id.edit_vocalTrackEditText);
        ScreenUtil.resizeTextSize(edit_vocalTrackEditText, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_vocalTrackEditText.setText(String.valueOf(songInfo.getVocalTrackNo()));

        TextView edit_vocalChannelStringTextView = findViewById(R.id.edit_vocalChannelStringTextView);
        ScreenUtil.resizeTextSize(edit_vocalChannelStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        vocalChannelEditText = findViewById(R.id.vocalChannelEditText);
        ScreenUtil.resizeTextSize(vocalChannelEditText, textFontSize, SmileApplication.FontSize_Scale_Type);
        vocalChannelEditText.setText(String.valueOf(songInfo.getVocalChannel()));

        final Button saveOneSongButton = findViewById(R.id.saveOneSongButton);
        ScreenUtil.resizeTextSize(saveOneSongButton, textFontSize, SmileApplication.FontSize_Scale_Type);
        saveOneSongButton.setText(actionStrong);

        final Button exitEditSongButton = findViewById(R.id.exitEditSongButton);
        ScreenUtil.resizeTextSize(exitEditSongButton, textFontSize, SmileApplication.FontSize_Scale_Type);
        exitEditSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnToPrevious();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() is called.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() is called.");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        String title = edit_titleNameEditText.getText().toString();
        String filePath = edit_filePathEditText.getText().toString();
        String musicTrack = edit_musicTrackEditText.getText().toString();
        String musicChannel = edit_musicChannelEditText.getText().toString();
        String vocalTrack = edit_vocalTrackEditText.getText().toString();
        String vocalChannel = vocalChannelEditText.getText().toString();
        songInfo.setSongName(title);
        songInfo.setFilePath(filePath);
        songInfo.setMusicTrackNo(Integer.valueOf(musicTrack));
        songInfo.setMusicChannel(Integer.valueOf(musicChannel));
        songInfo.setVocalTrackNo(Integer.valueOf(vocalTrack));
        songInfo.setVocalChannel(Integer.valueOf(vocalChannel));
        outState.putParcelable("SongInfo", songInfo);

        outState.putString("Action", action);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        songInfo = null;
    }

    @Override
    public void onBackPressed() {
        returnToPrevious();
    }

    private void returnToPrevious() {

        Intent returnIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putParcelable("SongInfo", songInfo);
        returnIntent.putExtras(extras);

        int resultYn = Activity.RESULT_OK;
        setResult(resultYn, returnIntent);    // can bundle some data to previous activity

        finish();
    }
}
