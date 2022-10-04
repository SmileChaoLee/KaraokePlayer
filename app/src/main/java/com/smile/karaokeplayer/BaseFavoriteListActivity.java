package com.smile.karaokeplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smile.karaokeplayer.adapters.SelectedFavoriteAdapter;
import com.smile.karaokeplayer.constants.CommonConstants;
import com.smile.karaokeplayer.constants.PlayerConstants;
import com.smile.karaokeplayer.models.FavoriteSingleTon;
import com.smile.karaokeplayer.models.SongInfo;
import com.smile.karaokeplayer.models.SongListSQLite;
import com.smile.smilelibraries.show_interstitial_ads.ShowInterstitial;
import com.smile.smilelibraries.utilities.ScreenUtil;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

public abstract class BaseFavoriteListActivity extends AppCompatActivity
        implements SelectedFavoriteAdapter.OnRecyclerItemClickListener {

    private static final String TAG = "BFavoriteListActivity";
    private final String CrudActionState = "CrudAction";
    private final String PositionEditState = "PositionEdit";
    private SongListSQLite songListSQLite;
    private float textFontSize;
    private float toastTextSize;
    private ActivityResultLauncher<Intent> editFavoritesLauncher;
    private String currentAction = CommonConstants.AddActionString;
    private ShowInterstitial interstitialAd = null;
    private RecyclerView myListRecyclerView;
    private SelectedFavoriteAdapter myRecyclerViewAdapter;
    private int positionEdit = -1;

    public abstract Intent createIntentFromSongDataActivity();
    public abstract void setAudioLinearLayoutVisibility(LinearLayout linearLayout);

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

        Button exitFavoriteListButton = findViewById(R.id.exitFavoriteListButton);
        ScreenUtil.resizeTextSize(exitFavoriteListButton, textFontSize, ScreenUtil.FontSize_Pixel_Type);
        exitFavoriteListButton.setOnClickListener(v -> returnToPrevious());

        myListRecyclerView = findViewById(R.id.selectedFavoriteRecyclerView);

        /*
        Intent callingIntent = getIntent();
        Bundle arguments = null;
        if (callingIntent != null) {
            arguments = callingIntent.getExtras();
        }
        */

        if (savedInstanceState != null) {
            ArrayList<SongInfo> favoriteList;
            // activity being recreated
            Log.d(TAG, "onCreate.savedInstanceState is not null");
            currentAction = savedInstanceState.getString(CrudActionState);
            positionEdit = savedInstanceState.getInt(PositionEditState, -1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                favoriteList = (ArrayList<SongInfo>) savedInstanceState
                        .getSerializable(PlayerConstants.MyFavoriteListState, ArrayList.class);
            else
                favoriteList = (ArrayList<SongInfo>) savedInstanceState
                        .getSerializable(PlayerConstants.MyFavoriteListState);
            if (favoriteList == null) {
                favoriteList = new ArrayList<>();
            }
            FavoriteSingleTon.INSTANCE.getSelectedList().clear();
            FavoriteSingleTon.INSTANCE.getSelectedList().addAll(favoriteList);
        } else {
            Log.d(TAG, "onCreate.savedInstanceState is null");
            /*
            // first recreating then using the FavoriteSingleTon.INSTANCE.getSelectedList()
            // It won't happen in this case (no R.id.MyFavorite any more, no CommonConstants.AddActionString)
            if (arguments == null) {
                Log.d(TAG, "onCreate.savedInstanceState is null, arguments is null");
                currentAction = CommonConstants.AddActionString;
                favoriteList = songListSQLite.readPlayList();
            } else {
                Log.d(TAG, "onCreate.savedInstanceState is null, arguments is not null");
                currentAction = CommonConstants.EditActionString;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    favoriteList = (ArrayList<SongInfo>) arguments
                            .getSerializable(PlayerConstants.MyFavoriteListState, ArrayList.class);
                else
                    favoriteList = (ArrayList<SongInfo>) arguments
                            .getSerializable(PlayerConstants.MyFavoriteListState);
                if (favoriteList == null) {
                    // for all favorites
                    currentAction = CommonConstants.AddActionString;
                    favoriteList = songListSQLite.readPlayList();
                } else {
                    Log.d(TAG, "onCreate.arguments is not null, favoriteList.size() = " + favoriteList.size());
                }
            }
            FavoriteSingleTon.INSTANCE.getSelectedList().clear();
            FavoriteSingleTon.INSTANCE.getSelectedList().addAll(favoriteList);
            */
        }

        editFavoritesLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result == null) {
                        return;
                    }
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        updateFavoriteList(result.getData());
                    }
                });

        Log.d(TAG, "onCreate.FavoriteSingleTon.INSTANCE.getSelectedList().size() = " +
                FavoriteSingleTon.INSTANCE.getSelectedList().size());

        initSelectedFavoriteRecyclerView();

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
        outState.putInt(PositionEditState, positionEdit);
        outState.putSerializable(PlayerConstants.MyFavoriteListState,
                FavoriteSingleTon.INSTANCE.getSelectedList());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
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
        Log.d(TAG, "returnToPrevious()");
        interstitialAd.new ShowAdThread().startShowAd();
        setResult(Activity.RESULT_OK);   // no bundle data
        finish();
    }

    private void deleteOneSongFromFavoriteList(SongInfo singleSongInfo) {
        currentAction = CommonConstants.DeleteActionString;
        Intent deleteIntent = createIntentFromSongDataActivity();
        deleteIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.DeleteActionString);
        deleteIntent.putExtra(PlayerConstants.SingleSongInfoState, singleSongInfo);
        editFavoritesLauncher.launch(deleteIntent);
    }

    private void editOneSongFromFavoriteList(SongInfo singleSongInfo) {
        currentAction = CommonConstants.EditActionString;
        Intent editIntent = createIntentFromSongDataActivity();
        editIntent.putExtra(CommonConstants.CrudActionString, CommonConstants.EditActionString);
        editIntent.putExtra(PlayerConstants.SingleSongInfoState, singleSongInfo);
        editFavoritesLauncher.launch(editIntent);
    }

    private void initSelectedFavoriteRecyclerView() {
        Log.d(TAG, "initSelectedFavoriteRecyclerView.getSelectedList() = " +
                FavoriteSingleTon.INSTANCE.getSelectedList().size());

        int yellow2Color = ContextCompat.getColor(this, R.color.yellow2);
        int yellow3Color = ContextCompat.getColor(this, R.color.yellow3);

        myRecyclerViewAdapter = SelectedFavoriteAdapter.getInstance(
                this, songListSQLite,
                FavoriteSingleTon.INSTANCE.getSelectedList(),
                textFontSize, yellow2Color, yellow3Color);

        myListRecyclerView.setAdapter(myRecyclerViewAdapter);
        myListRecyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean isAutoMeasureEnabled() {
                return false;
            }
        });
    }

    // implement SelectedFavoriteAdapter.OnRecyclerItemClickListener
    @Override
    public void onRecyclerItemClick(View v, int position) {
        Log.d(TAG, "onRecyclerItemClick.position = " + position);
    }

    @Override
    public void setAudioLayoutVisibility(@NotNull LinearLayout audioLayout) {
        setAudioLinearLayoutVisibility(audioLayout);
    }
    @Override
    public void editSongButtonFunc(int position) {
        if (position<0 || position>=FavoriteSingleTon.INSTANCE.getSelectedList().size()) {
            return;
        }
        Log.d(TAG, "editSongButtonFunc.positionEdit = " + positionEdit);
        positionEdit = position;
        editOneSongFromFavoriteList(FavoriteSingleTon.INSTANCE.getSelectedList().get(position));
    }
    @Override
    public void deleteSongButtonFunc(int position) {
        if (position<0 || position>=FavoriteSingleTon.INSTANCE.getSelectedList().size()) {
            return;
        }
        positionEdit = position;
        Log.d(TAG, "deleteSongButtonFunc.positionEdit = " + positionEdit);
        deleteOneSongFromFavoriteList(FavoriteSingleTon.INSTANCE.getSelectedList().get(position));
    }
    @Override
    public void playSongButtonFunc(int position) {
        // play this item (media file)
        if (position<0 || position>=FavoriteSingleTon.INSTANCE.getSelectedList().size()) {
            return;
        }
        Log.d(TAG, "playSongButtonFunc.positionEdit = " + positionEdit);
        currentAction = CommonConstants.PlayActionString;
        // getCallingActivity() only works from startActivityForResult
        // Intent playerActivityIntent = new Intent();
        // playerActivityIntent.setComponent(getCallingActivity());
        // playerActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        // Bundle extras = new Bundle();
        // extras.putBoolean(PlayerConstants.IsPlaySingleSongState, true);   // play single song
        // extras.putParcelable(PlayerConstants.SingleSongInfoState, singleSongInfo);
        // playerActivityIntent.putExtras(extras);
        // playSongLauncher.launch(playerActivityIntent);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        Intent bIntent = new Intent(PlayerConstants.PlaySingleSongAction);
        Bundle extras = new Bundle();
        extras.putBoolean(PlayerConstants.IsPlaySingleSongState, true);   // play single song
        extras.putParcelable(PlayerConstants.SingleSongInfoState,
                (FavoriteSingleTon.INSTANCE.getSelectedList().get(position)));
        bIntent.putExtras(extras);
        broadcastManager.sendBroadcast(bIntent);
    }
    // Finish implementing SelectedFavoriteAdapter.OnRecyclerItemClickListener

    private void updateFavoriteList(Intent data) {
        if (data != null && positionEdit != -1) {
            Log.d(TAG, "updateFavoriteList.positionEdit = " + positionEdit);
            SongInfo songInfo = data.getParcelableExtra(PlayerConstants.SingleSongInfoState);
            if (currentAction.equals(CommonConstants.EditActionString)) {
                FavoriteSingleTon.INSTANCE.getSelectedList().set(positionEdit, songInfo);
                myRecyclerViewAdapter.notifyItemChanged(positionEdit);
            } else {
                // delete
                FavoriteSingleTon.INSTANCE.getSelectedList().remove(positionEdit);
                myRecyclerViewAdapter.notifyItemRemoved(positionEdit);
            }
        }
    }
    private void updateFavoriteList_Old(Intent data) {
        // for edit and delete
        if (data != null) {
            SongInfo songInfo = data.getParcelableExtra(PlayerConstants.SingleSongInfoState);
            int id = songInfo.getId();
            Log.d(TAG, "updateFavoriteList.id = " + id);
            for (int i = 0; i < FavoriteSingleTon.INSTANCE.getSelectedList().size(); i++) {
                if (FavoriteSingleTon.INSTANCE.getSelectedList().get(i).getId() == id) {
                    if (currentAction.equals(CommonConstants.EditActionString)) {
                        // FavoriteSingleTon.INSTANCE.getSelectedList().set(i, new SongInfo(songInfo));
                        FavoriteSingleTon.INSTANCE.getSelectedList().set(i, songInfo);
                        myRecyclerViewAdapter.notifyItemChanged(i);
                    } else {
                        FavoriteSingleTon.INSTANCE.getSelectedList().remove(i);
                        myRecyclerViewAdapter.notifyItemRemoved(i);
                    }
                    break;
                }
            }
        }
    }
}
