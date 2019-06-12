package com.smile.karaokeplayer;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.smile.smilelibraries.utilities.ScreenUtil;

public class SmileApplication extends Application {

    public static final String PrivacyPolicyUrl = "http://ec2-13-59-195-3.us-east-2.compute.amazonaws.com/PrivacyPolicy";
    public static final int FontSize_Scale_Type = ScreenUtil.FontSize_Pixel_Type;

    public static Resources AppResources;
    public static Context AppContext;

    @Override
    public void onCreate() {
        super.onCreate();

        AppResources = getResources();
        AppContext = getApplicationContext();
    }
}
