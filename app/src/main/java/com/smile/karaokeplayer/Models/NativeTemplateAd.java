package com.smile.karaokeplayer.Models;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;

public class NativeTemplateAd {
    private static final String TAG = new String("NativeTemplateAd");

    private final Context mContext;
    private final String adUnitId;
    private final int maxNumberOfLoad = 15;
    private final TemplateView nativeAdTemplateView;
    private final AdLoader nativeAdLoader;
    private UnifiedNativeAd nativeAd;
    private boolean isNativeAdLoaded;
    private int numberOfLoad;

    public NativeTemplateAd(Context context, String nativeAdID, TemplateView templateView) {
        mContext = context;
        adUnitId = nativeAdID;
        this.nativeAdTemplateView = templateView;
        nativeAd = null;
        numberOfLoad = 0;
        AdLoader.Builder builder = new AdLoader.Builder(mContext, adUnitId);
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

                // start to show ad
                nativeAdTemplateView.setVisibility(View.VISIBLE);
                ColorDrawable background = new ColorDrawable(Color.WHITE);
                NativeTemplateStyle styles = new
                        NativeTemplateStyle.Builder().withMainBackgroundColor(background).build();

                nativeAdTemplateView.setStyles(styles);
                nativeAdTemplateView.setNativeAd(unifiedNativeAd);
                //

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
