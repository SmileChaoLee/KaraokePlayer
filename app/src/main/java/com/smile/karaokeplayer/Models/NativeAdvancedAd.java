package com.smile.karaokeplayer.Models;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.smile.karaokeplayer.R;

public class NativeAdvancedAd {
    private static final String TAG = new String("NativeAdvancedAd");

    private final Context mContext;
    private final ViewGroup parentView;
    private final int nativeAdViewLayoutId;
    private final int maxNumberOfLoad = 15;
    private final AdLoader nativeAdLoader;
    private UnifiedNativeAd nativeAd;
    private boolean isNativeAdLoaded;
    private int numberOfLoad;

    public NativeAdvancedAd(Context context, String nativeAdID, ViewGroup viewGroup, int layoutId) {
        mContext = context;
        parentView = viewGroup;
        nativeAdViewLayoutId = layoutId;
        nativeAd = null;
        numberOfLoad = 0;
        AdLoader.Builder builder = new AdLoader.Builder(mContext, nativeAdID);
        builder.forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
            // OnUnifiedNativeAdLoadedListener implementation.
            @Override
            public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                // You must call destroy on old ads when you are done with them,
                // otherwise you will have a memory leak.
                if (nativeAd != null) {
                    nativeAd.destroy();
                }
                nativeAd = unifiedNativeAd;
                isNativeAdLoaded = true;
                numberOfLoad = 0;
                displayUnifiedNativeAd(parentView, nativeAdViewLayoutId);
                Log.d(TAG, "Succeeded to load unifiedNativeAd.");
            }

        });

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();

        builder.withNativeAdOptions(adOptions);
        nativeAdLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.d(TAG, "Failed to load unifiedNativeAd.");
                Log.d(TAG, "numberOfLoad = " + numberOfLoad);
                isNativeAdLoaded = false;
                if (numberOfLoad<maxNumberOfLoad) {
                    loadOneAd();
                    Log.d(TAG, "Load again --> numberOfLoad = " + numberOfLoad);
                } else {
                    numberOfLoad = 0;   // set back to zero
                    Log.d(TAG, "Failed to load unifiedNativeAd more than 5.\nSo stopped loading this time. ");
                }
            }
        }).build();
    }

    public void loadOneAd() {
        nativeAdLoader.loadAd(new AdRequest.Builder().build());
        numberOfLoad++;
    }

    public void displayUnifiedNativeAd(ViewGroup parent, int unified_nativead_view_layout) {

        // Inflate a layout and add it to the parent ViewGroup.
        LayoutInflater inflater = (LayoutInflater) parent.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        UnifiedNativeAdView nativeAdView = (UnifiedNativeAdView) inflater
                .inflate(unified_nativead_view_layout, null);

        // Locate the view that will hold the headline, set its text, and call the
        // UnifiedNativeAdView's setHeadlineView method to register it.
        // TextView headlineView = nativeAdView.findViewById(R.id.native_ad_headline);
        // ScreenUtil.resizeTextSize(headlineView, textSize, ScreenUtil.FontSize_Pixel_Type);
        // headlineView.setText(nativeAd.getHeadline());
        // nativeAdView.setHeadlineView(headlineView);

        // ...
        // Repeat the above process for the other assets in the UnifiedNativeAd
        // using additional view objects (Buttons, ImageViews, etc).
        // ...

        // If the app is using a MediaView, it should be
        // instantiated and passed to setMediaView. This view is a little different
        // in that the asset is populated automatically, so there's one less step.
        MediaView mediaView = (MediaView) nativeAdView.findViewById(R.id.native_ad_media);
        nativeAdView.setMediaView(mediaView);

        // Call the UnifiedNativeAdView's setNativeAd method to register the
        // NativeAdObject.
        nativeAdView.setNativeAd(nativeAd);

        // Ensure that the parent view doesn't already contain an ad view.
        parent.removeAllViews();

        // Place the AdView into the parent.
        parent.addView(nativeAdView);
    }

    public AdLoader getNativeAdLoader() {
        return nativeAdLoader;
    }

    public UnifiedNativeAd getNativeAd() {
        return nativeAd;
    }

    public boolean isNativeAdLoaded() {
        return isNativeAdLoaded;
    }

    public void releaseNativeAd() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
    }
}
