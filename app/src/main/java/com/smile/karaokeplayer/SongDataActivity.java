package com.smile.karaokeplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.smile.karaokeplayer.ArrayAdapters.SpinnerAdapter;
import com.smile.karaokeplayer.Models.SongInfo;
import com.smile.karaokeplayer.Models.SongListSQLite;
// import com.smile.karaokeplayer.Utilities.ExternalStorageUtil;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;

public class SongDataActivity extends AppCompatActivity {

    private static final String TAG = new String(".SongDataActivity");
    private static final int SELECT_ONE_ONE_FILE_PATH = 1;

    private float textFontSize;
    private float fontScale;
    private float toastTextSize;

    private SpinnerAdapter audioMusicTrackAdapter;
    private SpinnerAdapter audioMusicChannelAdapter;
    private SpinnerAdapter audioVocalTrackAdapter;
    private SpinnerAdapter audioVocalChannelAdapter;

    private EditText edit_titleNameEditText;
    private EditText edit_filePathEditText;
    private ImageButton edit_selectFilePathButton;
    private Spinner edit_musicTrackSpinner;
    private Spinner edit_musicChannelSpinner;
    private Spinner edit_vocalTrackSpinner;
    private Spinner edit_vocalChannelSpinner;
    private CheckBox edit_includedPlaylistCheckBox;

    private String actionButtonString;
    private String crudAction;;
    private SongInfo mSongInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() is called.");

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, SmileApplication.FontSize_Scale_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, SmileApplication.FontSize_Scale_Type, 0.0f);
        fontScale = ScreenUtil.suitableFontScale(this, SmileApplication.FontSize_Scale_Type, 0.0f);
        textFontSize *= 0.8f;
        toastTextSize = 0.9f * textFontSize;

        Intent callingIntent = getIntent();
        if (savedInstanceState == null) {
            crudAction = callingIntent.getStringExtra(SmileApplication.CrudActionString);
            switch (crudAction.toUpperCase()) {
                case SmileApplication.AddActionString:
                    // add one record
                    mSongInfo = new SongInfo();
                    actionButtonString = getString(R.string.addString);
                    break;
                case SmileApplication.EditActionString:
                    // = "EDIT". Edit one record
                    mSongInfo = callingIntent.getParcelableExtra("SongInfo");
                    actionButtonString = getString(R.string.saveString);
                    break;
                case SmileApplication.DeleteActionString:
                    // = "DELETE". Delete one record
                    mSongInfo = callingIntent.getParcelableExtra("SongInfo");
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
            crudAction = savedInstanceState.getString(SmileApplication.CrudActionString);
            mSongInfo = savedInstanceState.getParcelable("SongInfo");
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
        audioMusicTrackAdapter = new SpinnerAdapter(this, R.layout.spinner_item_layout, R.id.spinnerTextView, numList, textFontSize, SmileApplication.FontSize_Scale_Type);
        audioVocalTrackAdapter = new SpinnerAdapter(this, R.layout.spinner_item_layout, R.id.spinnerTextView, numList, textFontSize, SmileApplication.FontSize_Scale_Type);
        ArrayList<String> aList = new ArrayList<>(SmileApplication.audioChannelMap.values());
        audioMusicChannelAdapter = new SpinnerAdapter(this, R.layout.spinner_item_layout, R.id.spinnerTextView, aList, textFontSize, SmileApplication.FontSize_Scale_Type);
        audioVocalChannelAdapter = new SpinnerAdapter(this, R.layout.spinner_item_layout, R.id.spinnerTextView, aList, textFontSize, SmileApplication.FontSize_Scale_Type);
        // audioVocalChannelAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout);

        TextView edit_titleStringTextView = findViewById(R.id.edit_titleStringTextView);
        ScreenUtil.resizeTextSize(edit_titleStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_titleNameEditText = findViewById(R.id.edit_titleNameEditText);
        ScreenUtil.resizeTextSize(edit_titleNameEditText, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_titleNameEditText.setText(mSongInfo.getSongName());

        TextView edit_filePathStringTextView = findViewById(R.id.edit_filePathStringTextView);
        ScreenUtil.resizeTextSize(edit_filePathStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_filePathEditText = findViewById(R.id.edit_filePathEditText);
        ScreenUtil.resizeTextSize(edit_filePathEditText, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_filePathEditText.setText(mSongInfo.getFilePath());

        edit_selectFilePathButton = findViewById(R.id.edit_selectFilePathButton);
        edit_selectFilePathButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFilePath();
            }
        });

        TextView edit_musicTrackStringTextView = findViewById(R.id.edit_musicTrackStringTextView);
        ScreenUtil.resizeTextSize(edit_musicTrackStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_musicTrackSpinner = findViewById(R.id.edit_musicTrackSpinner);
        edit_musicTrackSpinner.setAdapter(audioMusicTrackAdapter);
        edit_musicTrackSpinner.setSelection(mSongInfo.getMusicTrackNo() - 1);

        TextView edit_musicChannelStringTextView = findViewById(R.id.edit_musicChannelStringTextView);
        ScreenUtil.resizeTextSize(edit_musicChannelStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_musicChannelSpinner = findViewById(R.id.edit_musicChannelSpinner);
        edit_musicChannelSpinner.setAdapter(audioMusicChannelAdapter);
        edit_musicChannelSpinner.setSelection(mSongInfo.getMusicChannel());

        TextView edit_vocalTrackStringTextView = findViewById(R.id.edit_vocalTrackStringTextView);
        ScreenUtil.resizeTextSize(edit_vocalTrackStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_vocalTrackSpinner = findViewById(R.id.edit_vocalTrackSpinner);
        edit_vocalTrackSpinner.setAdapter(audioVocalTrackAdapter);
        edit_vocalTrackSpinner.setSelection(mSongInfo.getVocalTrackNo() - 1);

        TextView edit_vocalChannelStringTextView = findViewById(R.id.edit_vocalChannelStringTextView);
        ScreenUtil.resizeTextSize(edit_vocalChannelStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_vocalChannelSpinner = findViewById(R.id.edit_vocalChannelSpinner);
        edit_vocalChannelSpinner.setAdapter(audioVocalChannelAdapter);
        edit_vocalChannelSpinner.setSelection(mSongInfo.getVocalChannel());

        TextView edit_includedPlaylistStringTextView = findViewById(R.id.edit_includedPlaylistStringTextView);
        ScreenUtil.resizeTextSize(edit_includedPlaylistStringTextView, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_includedPlaylistCheckBox = findViewById(R.id.edit_includedPlaylistCheckBox);
        ScreenUtil.resizeTextSize(edit_includedPlaylistCheckBox, textFontSize, SmileApplication.FontSize_Scale_Type);
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
        ScreenUtil.resizeTextSize(edit_saveOneSongButton, textFontSize, SmileApplication.FontSize_Scale_Type);
        edit_saveOneSongButton.setText(actionButtonString);
        edit_saveOneSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean isValid = setSongInfoFromInput(true);
                SongListSQLite songListSQLite = new SongListSQLite(SmileApplication.AppContext);
                long databaseResult = -1;
                switch (crudAction.toUpperCase()) {
                    case SmileApplication.AddActionString:
                        // add one record
                        if (isValid) {
                            databaseResult = songListSQLite.addSongToSongList(mSongInfo);
                        }
                        break;
                    case SmileApplication.EditActionString:
                        // = "EDIT". Edit one record
                        if (isValid) {
                            databaseResult = songListSQLite.updateOneSongFromSongList(mSongInfo);
                        }
                        break;
                    case SmileApplication.DeleteActionString:
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
        ScreenUtil.resizeTextSize(edit_exitEditSongButton, textFontSize, SmileApplication.FontSize_Scale_Type);
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

        outState.putParcelable("SongInfo", mSongInfo);
        outState.putString(SmileApplication.CrudActionString, crudAction);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == SELECT_ONE_ONE_FILE_PATH && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri filePathUri = null;
            if (data != null) {
                filePathUri = data.getData();
                Log.i(TAG, "Filepath Uri: " + filePathUri.toString());

                if ( (filePathUri == null) || (Uri.EMPTY.equals(filePathUri)) ) {
                    return;
                }

                String filePathString = filePathUri.toString();
                Uri uri = Uri.parse(filePathString);
                Log.i(TAG, "Uri from filePathString: " + uri.toString());

                edit_filePathEditText.setText(filePathString);
            }
            return;
        }
    }

    private void returnToPrevious() {

        Intent returnIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putParcelable("SongInfo", mSongInfo);
        returnIntent.putExtras(extras);

        int resultYn = Activity.RESULT_OK;
        setResult(resultYn, returnIntent);    // can bundle some data to previous activity

        finish();
    }

    private void selectFilePath() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, SELECT_ONE_ONE_FILE_PATH);
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
        mSongInfo.setMusicChannel(SmileApplication.audioChannelReverseMap.get(musicChannel));
        mSongInfo.setVocalTrackNo(Integer.valueOf(vocalTrack));
        mSongInfo.setVocalChannel(SmileApplication.audioChannelReverseMap.get(vocalChannel));
        mSongInfo.setIncluded(included);

        if (title.isEmpty()) {
            isValid = false;
            if (hasMessage) {
                ScreenUtil.showToast(getApplicationContext(), getString(R.string.titileEmptyString),
                        toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
            }
        }
        if (filePath.isEmpty()) {
            isValid = false;
            if (hasMessage) {
                ScreenUtil.showToast(getApplicationContext(), getString(R.string.filepathEmptyString),
                        toastTextSize, SmileApplication.FontSize_Scale_Type, Toast.LENGTH_SHORT);
            }
        }

        return isValid;
    }
}
