package com.smile.karaokeplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.smile.karaokeplayer.constants.CommonConstants;
import com.smile.karaokeplayer.constants.PlayerConstants;
import com.smile.karaokeplayer.models.SongInfo;
import com.smile.karaokeplayer.models.SongListSQLite;
import com.smile.karaokeplayer.utilities.SelectFavoritesUtil;
import com.smile.smilelibraries.show_interstitial_ads.ShowInterstitial;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.ArrayList;
import java.util.Collection;

public abstract class BaseFavoriteListActivity extends AppCompatActivity {

    private static final String TAG = "BFavoriteListActivity";
    private final String CrudActionState = "CrudAction";
    private SongListSQLite songListSQLite;
    private float textFontSize;
    private float toastTextSize;
    private ArrayList<SongInfo> mFavoriteList = new ArrayList<>();
    private FavoriteListAdapter favoriteListAdapter;
    private ActivityResultLauncher<Intent> selectOneSongLauncher;
    private ActivityResultLauncher<Intent> selectSongsLauncher;
    private String currentAction = CommonConstants.AddActionString;
    private ShowInterstitial interstitialAd = null;

    public abstract Intent createIntentFromSongDataActivity();
    public abstract void setAudioLinearLayoutVisibility(LinearLayout linearLayout);
    public abstract Intent createPlayerActivityIntent();

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {

        float defaultTextFontSize = ScreenUtil.getDefaultTextSizeFromTheme(this, ScreenUtil.FontSize_Pixel_Type, null);
        textFontSize = ScreenUtil.suitableFontSize(this, defaultTextFontSize, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        // float fontScale = ScreenUtil.suitableFontScale(this, ScreenUtil.FontSize_Pixel_Type, 0.0f);
        toastTextSize = 0.8f * textFontSize;

        interstitialAd = new ShowInterstitial(this,
                ((BaseApplication)getApplication()).facebookInterstitial,
                ((BaseApplication)getApplication()).adMobInterstitial);

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

        Intent callingIntent = getIntent();
        Bundle arguments = null;
        if (callingIntent != null) {
            arguments = callingIntent.getExtras();
        }
        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate.savedInstanceState is not null");
            currentAction = savedInstanceState.getString(CrudActionState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                mFavoriteList = (ArrayList<SongInfo>) savedInstanceState
                        .getSerializable(PlayerConstants.MyFavoriteListState, ArrayList.class);
            else
                mFavoriteList = (ArrayList<SongInfo>) savedInstanceState
                        .getSerializable(PlayerConstants.MyFavoriteListState);
        } else {
            if (arguments == null) {
                Log.d(TAG, "onCreate.savedInstanceState is null, arguments is null");
                currentAction = CommonConstants.AddActionString;
                mFavoriteList = songListSQLite.readPlayList();
            } else {
                Log.d(TAG, "onCreate.savedInstanceState is null, arguments is not null");
                currentAction = CommonConstants.EditActionString;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    mFavoriteList = (ArrayList<SongInfo>) arguments
                            .getSerializable(PlayerConstants.MyFavoriteListState, ArrayList.class);
                else
                    mFavoriteList = (ArrayList<SongInfo>) arguments
                            .getSerializable(PlayerConstants.MyFavoriteListState);
                if (mFavoriteList == null) {
                    // for all favorites
                    currentAction = CommonConstants.AddActionString;
                    mFavoriteList = songListSQLite.readPlayList();
                }
            }
        }

        addFavoriteListButton.setVisibility(currentAction.equals(CommonConstants.AddActionString)?
                View.VISIBLE : View.GONE) ;

        favoriteListAdapter = new FavoriteListAdapter(this, R.layout.activity_favorite_list_item, mFavoriteList);
        favoriteListAdapter.setNotifyOnChange(false);

        ListView favoriteListListView = findViewById(R.id.favoriteListListView);
        favoriteListListView.setAdapter(favoriteListAdapter);
        favoriteListListView.setOnItemClickListener((adapterView, view, position, rowId) -> {
        });

        selectOneSongLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result == null) {
                        return;
                    }
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        updateFavoriteList(result.getData());
                    }
                });

        selectSongsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result == null) {
                        return;
                    }
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        addSongsToFavoriteList(result.getData());
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
        outState.putString(CrudActionState, currentAction);
        outState.putSerializable(PlayerConstants.MyFavoriteListState, mFavoriteList);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (songListSQLite != null) {
            songListSQLite.closeDatabase();
            songListSQLite = null;
        }
        if (interstitialAd != null) {
            interstitialAd.close();
        }
    }

    private void returnToPrevious() {
        interstitialAd.new ShowAdThread().startShowAd();
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);    // can bundle some data to previous activity
        // setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    private void selectMultipleSong() {
        Intent selectFileIntent = new Intent(this, OpenFileActivity.class);
        selectFileIntent.putExtra(CommonConstants.IsButtonForPlay, false);
        selectSongsLauncher.launch(selectFileIntent);
    }

    private void deleteOneSongFromFavoriteList(SongInfo singleSongInfo) {
        currentAction = CommonConstants.DeleteActionString;
        Intent deleteIntent = createIntentFromSongDataActivity();
        deleteIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.DeleteActionString);
        deleteIntent.putExtra(PlayerConstants.SingleSongInfoState, singleSongInfo);
        selectOneSongLauncher.launch(deleteIntent);
    }

    private void editOneSongFromFavoriteList(SongInfo singleSongInfo) {
        currentAction = CommonConstants.EditActionString;
        Intent editIntent = createIntentFromSongDataActivity();
        editIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.EditActionString);
        editIntent.putExtra(PlayerConstants.SingleSongInfoState, singleSongInfo);
        selectOneSongLauncher.launch(editIntent);
    }

    protected void addSongsToFavoriteList(Intent data) {
        currentAction = CommonConstants.AddActionString;
        SelectFavoritesUtil.addDataToFavoriteList(data, songListSQLite);

        // update the UI (currentAction = CommonConstants.AddActionString)
        mFavoriteList = songListSQLite.readPlayList();
        favoriteListAdapter.updateData(mFavoriteList);    // update the UI
    }

    private void updateFavoriteList(Intent data) {
        // for edit and delete
        if (data != null) {
            SongInfo songInfo = data.getParcelableExtra(PlayerConstants.SingleSongInfoState);
            int id = songInfo.getId();
            Log.d(TAG, "updateFavoriteList.id = " + id);
            for (int i = 0; i < mFavoriteList.size(); i++) {
                if (mFavoriteList.get(i).getId() == id) {
                    if (currentAction.equals(CommonConstants.EditActionString)) {
                        mFavoriteList.set(i, new SongInfo(songInfo));
                    } else {
                        mFavoriteList.remove(i);
                    }
                    break;
                }
            }
            favoriteListAdapter.updateData(mFavoriteList);    // update the UI
        }
    }

    private class FavoriteListAdapter extends ArrayAdapter {

        private final float itemTextSize = textFontSize * 0.6f;
        private final float buttonTextSize = textFontSize * 0.8f;
        private final int yellow2Color;
        private final int yellow3Color;
        private final int layoutId;
        private final ArrayList<SongInfo> favoriteList;

        @SuppressWarnings("unchecked")
        FavoriteListAdapter(Context context, int layoutId, ArrayList<SongInfo> favoriteList) {
            super(context, layoutId, favoriteList);
            this.layoutId = layoutId;
            this.favoriteList = new ArrayList<>(favoriteList);
            yellow2Color = ContextCompat.getColor(context, R.color.yellow2);
            yellow3Color = ContextCompat.getColor(context, R.color.yellow3);
        }

        // getCount() must be overridden
        @Override
        public int getCount() {
            return favoriteList.size();
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
            // the following is still needed to be test (have to reduce the usage of memory)
            if (!com.smile.karaokeplayer.BuildConfig.DEBUG) playSongButton.setVisibility(View.GONE);

            int favoriteListSize = 0;
            if (favoriteList != null) {
                favoriteListSize = favoriteList.size();
            }
            if (favoriteListSize > 0) {
                final SongInfo singleSongInfo = favoriteList.get(position);

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
                    currentAction = CommonConstants.PlayActionString;
                    /*
                    // getCallingActivity() only works from startActivityForResult
                    Intent playerActivityIntent = new Intent();
                    playerActivityIntent.setComponent(getCallingActivity());
                    */
                    Intent playerActivityIntent = createPlayerActivityIntent();
                    Bundle extras = new Bundle();
                    extras.putBoolean(PlayerConstants.IsPlaySingleSongState, true);   // play single song
                    extras.putParcelable(PlayerConstants.SingleSongInfoState, singleSongInfo);
                    playerActivityIntent.putExtras(extras);
                    startActivity(playerActivityIntent);
                    // close Player Activity (ExoPlayerActivity or VLCPlayerActivity) using Broadcast
                    LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
                    Intent bIntent = new Intent(PlayerConstants.ClosePlayerActivity);
                    broadcastManager.sendBroadcast(bIntent);
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
            favoriteList.clear();
            favoriteList.addAll(newData);
            notifyDataSetChanged();
        }
    }
}
