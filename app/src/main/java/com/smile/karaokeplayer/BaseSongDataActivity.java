package com.smile.karaokeplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.smile.karaokeplayer.ArrayAdapters.SpinnerAdapter;
import com.smile.karaokeplayer.Constants.CommonConstants;
import com.smile.karaokeplayer.Constants.PlayerConstants;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;

public abstract class BaseSongDataActivity extends AppCompatActivity {

    private static final String TAG = "BaseSongDataActivity";
    private static final int SELECT_ONE_FILE = 1;

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;

    private SpinnerAdapter audioMusicTrackAdapter;
    private SpinnerAdapter audioMusicChannelAdapter;
    private SpinnerAdapter audioVocalTrackAdapter;
    private SpinnerAdapter audioVocalChannelAdapter;

    private EditText edit_titleNameEditText;
    protected EditText edit_filePathEditText;
    private ImageButton edit_selectOneFilePathButton;
    private Spinner edit_musicTrackSpinner;
    private Spinner edit_musicChannelSpinner;
    private Spinner edit_vocalTrackSpinner;
    private Spinner edit_vocalChannelSpinner;
    private CheckBox edit_includedPlaylistCheckBox;
    protected LinearLayout karaokeSettingLayout;

    private String actionButtonString;
    private String crudAction;;
    private SongInfo mSongInfo;

    public abstract Intent createSelectOneFileToOpenIntent();
    public abstract ArrayList<Uri> getUrisListFromIntentSongData(Intent data);
    public abstract void setKaraokeSettingLayoutVisibility();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() is called.");

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        textFontSize *= 0.8f;
        toastTextSize = 0.9f * textFontSize;

        Intent callingIntent = getIntent();
        if (savedInstanceState == null) {
            crudAction = callingIntent.getStringExtra(CommonConstants.CrudActionString);
            switch (crudAction.toUpperCase()) {
                case CommonConstants.AddActionString:
                    // add one record
                    mSongInfo = new SongInfo();
                    actionButtonString = getString(R.string.addOneString);
                    break;
                case CommonConstants.EditActionString:
                    // = "EDIT". Edit one record
                    mSongInfo = callingIntent.getParcelableExtra(PlayerConstants.SongInfoState);
                    actionButtonString = getString(R.string.saveString);
                    break;
                case CommonConstants.DeleteActionString:
                    // = "DELETE". Delete one record
                    mSongInfo = callingIntent.getParcelableExtra(PlayerConstants.SongInfoState);
                    actionButtonString = getString(R.string.deleteString);
                    break;
                default:
                    returnToPrevious();
                    return;
            }
            Log.d(TAG, "savedInstanceState is null.");
        } else {
            // not null, has savedInstanceState
            actionButtonString = savedInstanceState.getString("ActionButtonString");
            crudAction = savedInstanceState.getString(CommonConstants.CrudActionString);
            mSongInfo = savedInstanceState.getParcelable(PlayerConstants.SongInfoState);
            Log.d(TAG, "savedInstanceState is not null.");
        }

        if (crudAction == null) {
            returnToPrevious();
            return;
        }
        if (mSongInfo == null) {
            returnToPrevious();
            return;
        }

        setContentView(R.layout.activity_song_data);

        // ArrayAdapters for spinners
        ArrayList<String> numList = new ArrayList<>();
        numList.add("1");
        numList.add("2");
        numList.add("3");
        numList.add("4");
        numList.add("5");
        numList.add("6");
        numList.add("7");
        numList.add("8");
        audioMusicTrackAdapter = new SpinnerAdapter(this, R.layout.spinner_item_layout, R.id.spinnerTextView, numList, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        audioVocalTrackAdapter = new SpinnerAdapter(this, R.layout.spinner_item_layout, R.id.spinnerTextView, numList, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        ArrayList<String> aList = new ArrayList<>(BaseApplication.audioChannelMap.values());
        audioMusicChannelAdapter = new SpinnerAdapter(this, R.layout.spinner_item_layout, R.id.spinnerTextView, aList, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        audioVocalChannelAdapter = new SpinnerAdapter(this, R.layout.spinner_item_layout, R.id.spinnerTextView, aList, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        // audioVocalChannelAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout);

        TextView edit_titleStringTextView = findViewById(R.id.edit_titleStringTextView);
        ScreenUtil.resizeTextSize(edit_titleStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_titleNameEditText = findViewById(R.id.edit_titleNameEditText);
        ScreenUtil.resizeTextSize(edit_titleNameEditText, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_titleNameEditText.setText(mSongInfo.getSongName());

        TextView edit_filePathStringTextView = findViewById(R.id.edit_filePathStringTextView);
        ScreenUtil.resizeTextSize(edit_filePathStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_filePathEditText = findViewById(R.id.edit_filePathEditText);
        edit_filePathEditText.setEnabled(false);
        ScreenUtil.resizeTextSize(edit_filePathEditText, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_filePathEditText.setText(mSongInfo.getFilePath());

        edit_selectOneFilePathButton = findViewById(R.id.edit_selectOneFilePathButton);
        edit_selectOneFilePathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectOneFilePathSongData();
            }
        });

        karaokeSettingLayout = findViewById(R.id.karaokeSettingLayout);
        //
        TextView edit_musicTrackStringTextView = findViewById(R.id.edit_musicTrackStringTextView);
        ScreenUtil.resizeTextSize(edit_musicTrackStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_musicTrackSpinner = findViewById(R.id.edit_musicTrackSpinner);
        edit_musicTrackSpinner.setAdapter(audioMusicTrackAdapter);
        edit_musicTrackSpinner.setSelection(mSongInfo.getMusicTrackNo() - 1);

        TextView edit_musicChannelStringTextView = findViewById(R.id.edit_musicChannelStringTextView);
        ScreenUtil.resizeTextSize(edit_musicChannelStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_musicChannelSpinner = findViewById(R.id.edit_musicChannelSpinner);
        edit_musicChannelSpinner.setAdapter(audioMusicChannelAdapter);
        edit_musicChannelSpinner.setSelection(mSongInfo.getMusicChannel());

        TextView edit_vocalTrackStringTextView = findViewById(R.id.edit_vocalTrackStringTextView);
        ScreenUtil.resizeTextSize(edit_vocalTrackStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_vocalTrackSpinner = findViewById(R.id.edit_vocalTrackSpinner);
        edit_vocalTrackSpinner.setAdapter(audioVocalTrackAdapter);
        edit_vocalTrackSpinner.setSelection(mSongInfo.getVocalTrackNo() - 1);

        TextView edit_vocalChannelStringTextView = findViewById(R.id.edit_vocalChannelStringTextView);
        ScreenUtil.resizeTextSize(edit_vocalChannelStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_vocalChannelSpinner = findViewById(R.id.edit_vocalChannelSpinner);
        edit_vocalChannelSpinner.setAdapter(audioVocalChannelAdapter);
        edit_vocalChannelSpinner.setSelection(mSongInfo.getVocalChannel());

        //
        setKaraokeSettingLayoutVisibility();    // abstract method
        //

        TextView edit_includedPlaylistStringTextView = findViewById(R.id.edit_includedPlaylistStringTextView);
        ScreenUtil.resizeTextSize(edit_includedPlaylistStringTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_includedPlaylistCheckBox = findViewById(R.id.edit_includedPlaylistCheckBox);
        ScreenUtil.resizeTextSize(edit_includedPlaylistCheckBox, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        boolean isChecked = (mSongInfo.getIncluded().equals("1")) ? true : false;
        edit_includedPlaylistCheckBox.setChecked(isChecked);
        edit_includedPlaylistCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                edit_includedPlaylistCheckBox.setChecked(isChecked);
                edit_includedPlaylistCheckBox.jumpDrawablesToCurrentState();
            }
        });

        final Button edit_saveOneSongButton = findViewById(R.id.edit_saveOneSongButton);
        ScreenUtil.resizeTextSize(edit_saveOneSongButton, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_saveOneSongButton.setText(actionButtonString);
        edit_saveOneSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean isValid = setSongInfoFromInput(true);
                SongListSQLite songListSQLite = new SongListSQLite(getApplicationContext());
                SongInfo songInfo;
                long databaseResult = -1;
                switch (crudAction.toUpperCase()) {
                    case CommonConstants.AddActionString:
                        // add one record
                        if (isValid) {
                            // check if this file is already in database
                            songInfo = songListSQLite.findOneSongByUriString(mSongInfo.getFilePath());
                            if (songInfo == null) {
                                databaseResult = songListSQLite.addSongToSongList(mSongInfo);
                            } else {
                                ScreenUtil.showToast(BaseSongDataActivity.this, getString(R.string.duplicate_in_database),
                                        toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_LONG);
                            }
                        }
                        break;
                    case CommonConstants.EditActionString:
                        // = "EDIT". Edit one record
                        if (isValid) {
                            songInfo = songListSQLite.findOneSongByUriString(mSongInfo.getFilePath());
                            if (songInfo == null) {
                                // not in the database
                                databaseResult = songListSQLite.updateOneSongFromSongList(mSongInfo);
                            } else {
                                // already in the database
                                // if (songInfo.getFilePath() == mSongInfo.getFilePath()) {
                                if (songInfo.getId() == mSongInfo.getId()) {
                                    // same record because same id so update
                                    databaseResult = songListSQLite.updateOneSongFromSongList(mSongInfo);
                                } else {
                                    // different id then duplicate
                                    ScreenUtil.showToast(BaseSongDataActivity.this, getString(R.string.duplicate_in_database),
                                            toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_LONG);
                                }
                            }
                        }
                        break;
                    case CommonConstants.DeleteActionString:
                        // = "DELETE". Delete one record
                        databaseResult = songListSQLite.deleteOneSongFromSongList(mSongInfo);
                        break;
                }
                songListSQLite.closeDatabase();

                if (databaseResult != -1) {
                    returnToPrevious();
                }
            }
        });

        final Button edit_exitEditSongButton = findViewById(R.id.edit_exitEditSongButton);
        ScreenUtil.resizeTextSize(edit_exitEditSongButton, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        edit_exitEditSongButton.setOnClickListener(new View.OnClickListener() {
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

        setSongInfoFromInput(false);

        outState.putParcelable(PlayerConstants.SongInfoState, mSongInfo);
        outState.putString(CommonConstants.CrudActionString, crudAction);
        outState.putString("ActionButtonString", actionButtonString);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSongInfo = null;
    }

    @Override
    public void onBackPressed() {
        returnToPrevious();
    }

    private void returnToPrevious() {

        Intent returnIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putParcelable(PlayerConstants.SongInfoState, mSongInfo);
        returnIntent.putExtras(extras);

        int resultYn = Activity.RESULT_OK;
        setResult(resultYn, returnIntent);    // can bundle some data to previous activity

        finish();
    }

    private boolean setSongInfoFromInput(boolean hasMessage) {

        boolean isValid = true;

        String title = "";
        Editable text = edit_titleNameEditText.getText();
        if (text != null) {
            title = text.toString().trim();
        }
        text = edit_filePathEditText.getText();
        String filePath = "";
        if (text != null) {
            filePath = text.toString().trim();
        }

        String musicTrack = edit_musicTrackSpinner.getSelectedItem().toString();
        String musicChannel = edit_musicChannelSpinner.getSelectedItem().toString();
        String vocalTrack = edit_vocalTrackSpinner.getSelectedItem().toString();
        String vocalChannel = edit_vocalChannelSpinner.getSelectedItem().toString();
        String included = edit_includedPlaylistCheckBox.isChecked() ? "1" : "0";

        mSongInfo.setSongName(title);
        mSongInfo.setFilePath(filePath);
        mSongInfo.setMusicTrackNo(Integer.valueOf(musicTrack));
        mSongInfo.setMusicChannel(BaseApplication.audioChannelReverseMap.get(musicChannel));
        mSongInfo.setVocalTrackNo(Integer.valueOf(vocalTrack));
        mSongInfo.setVocalChannel(BaseApplication.audioChannelReverseMap.get(vocalChannel));
        mSongInfo.setIncluded(included);

        if (filePath.isEmpty()) {
            isValid = false;
            if (hasMessage) {
                ScreenUtil.showToast(this, getString(R.string.filepathEmptyString),
                        toastTextSize, ScreenUtil.FontSize_Pixel_Type, Toast.LENGTH_SHORT);
            }
        }

        return isValid;
    }

    private void selectOneFilePathSongData() {
        Log.d(TAG, "selectOneFilePathSongData() is called.");
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent data = result.getData();
                        ArrayList<Uri> uris = getUrisListFromIntentSongData(data);
                        // this activity allows only one file selected
                        switch (uris.size()) {
                            case 0:
                                // no files selected
                                break;
                            case 1:
                                // single file selected
                                edit_filePathEditText.setText(uris.get(0).toString());
                                break;
                        }
                    }
                });

        Intent selectOneFileIntent = createSelectOneFileToOpenIntent();
        activityResultLauncher.launch(selectOneFileIntent);
    }
}
