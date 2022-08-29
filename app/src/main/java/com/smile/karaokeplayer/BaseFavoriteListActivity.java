package com.smile.karaokeplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;

import com.smile.karaokeplayer.constants.CommonConstants;
import com.smile.karaokeplayer.constants.PlayerConstants;
import com.smile.karaokeplayer.models.SongInfo;
import com.smile.karaokeplayer.models.SongListSQLite;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;
import java.util.Collection;

public abstract class BaseFavoriteListActivity extends AppCompatActivity {

    private static final String TAG = "BFavoriteListActivity";
    private SongListSQLite songListSQLite;
    private float textFontSize;
    private float toastTextSize;
    private ArrayList<SongInfo> mFavoriteList = new ArrayList<>();
    private FavoriteListAdapter favoriteListAdapter;
    private ActivityResultLauncher<Intent> selectOneSongActivityLauncher;
    private ActivityResultLauncher<Intent> selectMultipleSongActivityLauncher;

    public abstract Intent createIntentFromSongDataActivity();
    public abstract void setAudioLinearLayoutVisibility(LinearLayout linearLayout);
    public abstract Intent createPlayerActivityIntent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        // float fontScale = ScreenUtil.suitableFontScale(this, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.8f * textFontSize;

        songListSQLite = new SongListSQLite(getApplicationContext());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_favorite_list);

        TextView myFavoritesTextView = findViewById(R.id.myFavoritesTextView);
        ScreenUtil.resizeTextSize(myFavoritesTextView, textFontSize, ScreenUtil.FontSize_Pixel_Type);

        Button addFavoriteListButton = findViewById(R.id.addFavoriteListButton);
        ScreenUtil.resizeTextSize(addFavoriteListButton, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        addFavoriteListButton.setOnClickListener(v -> selectMultipleSong());

        Button exitFavoriteListButton = findViewById(R.id.exitFavoriteListButton);
        ScreenUtil.resizeTextSize(exitFavoriteListButton, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        exitFavoriteListButton.setOnClickListener(v -> returnToPrevious());

        mFavoriteList = songListSQLite.readSongList();

        favoriteListAdapter = new FavoriteListAdapter(this, R.layout.activity_favorite_list_item, mFavoriteList);
        favoriteListAdapter.setNotifyOnChange(false);

        ListView favoriteListListView = findViewById(R.id.favoriteListListView);
        favoriteListListView.setAdapter(favoriteListAdapter);
        favoriteListListView.setOnItemClickListener((adapterView, view, position, rowId) -> {

        });

        selectOneSongActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> updateFavoriteList());
        selectMultipleSongActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result == null) {
                        return;
                    }
                    int resultCode = result.getResultCode();
                    if (resultCode == Activity.RESULT_OK) {
                        addMultipleSongToFavoriteList(result.getData());
                    }
                });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "getOnBackPressedDispatcher.handleOnBackPressed");
                returnToPrevious();
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

    private void returnToPrevious() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    private void selectMultipleSong() {
        Intent selectFileIntent = new Intent(this, OpenFileActivity.class);
        selectFileIntent.putExtra(CommonConstants.IsButtonForPlay, false);
        selectMultipleSongActivityLauncher.launch(selectFileIntent);
    }

    private void deleteOneSongFromFavoriteList(SongInfo singleSongInfo) {
        Intent deleteIntent = createIntentFromSongDataActivity();
        deleteIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.DeleteActionString);
        deleteIntent.putExtra(PlayerConstants.SongInfoState, singleSongInfo);
        selectOneSongActivityLauncher.launch(deleteIntent);
    }

    private void editOneSongFromFavoriteList(SongInfo singleSongInfo) {
        Intent editIntent = createIntentFromSongDataActivity();
        editIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.EditActionString);
        editIntent.putExtra(PlayerConstants.SongInfoState, singleSongInfo);
        selectOneSongActivityLauncher.launch(editIntent);
    }

    protected void addMultipleSongToFavoriteList(Intent data) {
        ArrayList<Uri> uris;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uris = data.getParcelableArrayListExtra(PlayerConstants.Uri_List, Uri.class);
        } else
            uris = data.getParcelableArrayListExtra(PlayerConstants.Uri_List);

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

        updateFavoriteList();
    }

    protected void updateFavoriteList() {
        Log.d(TAG, "updateFavoriteList()");
        mFavoriteList = songListSQLite.readSongList();
        favoriteListAdapter.updateData(mFavoriteList);    // update the UI
    }

    private class FavoriteListAdapter extends ArrayAdapter {

        private final float itemTextSize = textFontSize * 0.6f;
        private final float buttonTextSize = textFontSize * 0.8f;
        private final int yellow2Color;
        private final int yellow3Color;
        private final int layoutId;
        private final ArrayList<SongInfo> mFavoriteList;

        @SuppressWarnings("unchecked")
        FavoriteListAdapter(Context context, int layoutId, ArrayList<SongInfo> favoriteList) {
            super(context, layoutId, favoriteList);
            this.layoutId = layoutId;
            this.mFavoriteList = favoriteList;
            yellow2Color = ContextCompat.getColor(context, R.color.yellow2);
            yellow3Color = ContextCompat.getColor(context, R.color.yellow3);
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

            @SuppressLint("ViewHolder") View view = getLayoutInflater().inflate(layoutId, parent,false);

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
            final TextView includedPlaylistTextView = view.findViewById(R.id.includedPlaylistTextView);
            ScreenUtil.resizeTextSize(includedPlaylistTextView, itemTextSize, ScreenUtil.FontSize_Pixel_Type);
            final CheckBox includedPlaylistCheckBox = view.findViewById(R.id.includedPlaylistCheckBox);
            ScreenUtil.resizeTextSize(includedPlaylistCheckBox, itemTextSize, ScreenUtil.FontSize_Pixel_Type);

            final Button editSongButton = view.findViewById(R.id.editSongButton);
            ScreenUtil.resizeTextSize(editSongButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type);

            final Button deleteSongButton = view.findViewById(R.id.deleteSongButton);
            ScreenUtil.resizeTextSize(deleteSongButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type);

            final Button playSongButton = view.findViewById(R.id.playSongButton);
            ScreenUtil.resizeTextSize(playSongButton, buttonTextSize, ScreenUtil.FontSize_Pixel_Type);

            int favoriteListSize = 0;
            if (mFavoriteList != null) {
                favoriteListSize = mFavoriteList.size();
            }
            if (favoriteListSize > 0) {
                final SongInfo singleSongInfo = mFavoriteList.get(position);

                titleNameTextView.setText(singleSongInfo.getSongName());
                filePathTextView.setText(singleSongInfo.getFilePath());
                musicTrackTextView.setText(String.valueOf(singleSongInfo.getMusicTrackNo()));
                musicChannelTextView.setText(BaseApplication.audioChannelMap.get(singleSongInfo.getMusicChannel()));
                vocalTrackTextView.setText(String.valueOf(singleSongInfo.getVocalTrackNo()));
                vocalChannelTextView.setText(BaseApplication.audioChannelMap.get(singleSongInfo.getVocalChannel()));

                boolean inPlaylist = (singleSongInfo.getIncluded().equals("1"));
                includedPlaylistCheckBox.setChecked(inPlaylist);
                includedPlaylistCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    includedPlaylistCheckBox.setChecked(isChecked);
                    includedPlaylistCheckBox.jumpDrawablesToCurrentState();
                    String included = isChecked ? "1" : "0";
                    singleSongInfo.setIncluded(included);
                    songListSQLite.updateOneSongFromSongList(singleSongInfo);
                });

                editSongButton.setOnClickListener(view1 -> editOneSongFromFavoriteList(singleSongInfo));

                deleteSongButton.setOnClickListener(view12 -> deleteOneSongFromFavoriteList(singleSongInfo));

                playSongButton.setOnClickListener(v -> {
                    // play this item (media file)
                    Intent callingIntent = getIntent(); // from ExoPlayActivity or some Activity (like VLC)
                    Log.d(TAG, "playSongButton.callingIntent = " + callingIntent);
                    if (callingIntent != null) {
                        Log.d(TAG, "playSongButton.createPlayerActivityIntent");
                        Intent playerActivityIntent = createPlayerActivityIntent();
                        Bundle extras = new Bundle();
                        extras.putBoolean(PlayerConstants.IsPlaySingleSongState, true);   // play single song
                        extras.putParcelable(PlayerConstants.SongInfoState, singleSongInfo);
                        playerActivityIntent.putExtras(extras);
                        Log.d(TAG, "playSongButton.activityResultLauncher.launch(playerActivityIntent)");
                        selectOneSongActivityLauncher.launch(playerActivityIntent);
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

        public void updateData(ArrayList<SongInfo> newData) {
            clear();
            addAll(newData);
            notifyDataSetChanged();
        }
    }
}
