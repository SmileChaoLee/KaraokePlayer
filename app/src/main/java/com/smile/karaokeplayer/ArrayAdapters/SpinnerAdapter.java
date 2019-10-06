package com.smile.karaokeplayer.ArrayAdapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.smile.karaokeplayer.R;
import com.smile.smilelibraries.utilities.ScreenUtil;

import java.util.List;

public class SpinnerAdapter extends ArrayAdapter {

    private static final String TAG = new String(".ArrayAdapters.SpinnerAdapter");
    private final Context mContext;
    private final Activity mActivity;
    private final int mResourceId;
    private final int mTextViewResourceId;
    private final float mTextFontSize;
    private final int mScaleType;

    @SuppressWarnings("unchecked")
    public SpinnerAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List objects, float textSize, int scaleType) {
        super(context, resource, textViewResourceId, objects);
        mContext = context;
        mActivity = (Activity)mContext;
        mResourceId = resource;
        mTextViewResourceId = textViewResourceId;
        mTextFontSize = textSize;
        mScaleType = scaleType;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        // or
        // View view = mActivity.getLayoutInflater().inflate(mResourceId, parent, false);

        if (getCount() == 0) {
            return view;
        }

        if (view != null) {
            TextView itemTextView = view.findViewById(mTextViewResourceId);
            // If using View view = mActivity.getLayoutInflater().inflate(mResourceId, parent, false);
            // then the following statement must be used
            // itemTextView.setText(getItem(position).toString());
            ScreenUtil.resizeTextSize(itemTextView, mTextFontSize, mScaleType);
        }

        return view;
    }

    @NonNull
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // View view = super.getView(position, convertView, parent);
        View view = mActivity.getLayoutInflater().inflate(R.layout.spinner_dropdown_item_layout, parent, false);

        if (getCount() == 0) {
            return view;
        }

        if (view != null) {
            TextView itemTextView = view.findViewById(R.id.customSpinnerTextView);
            itemTextView.setText(getItem(position).toString());
            ScreenUtil.resizeTextSize(itemTextView, mTextFontSize, mScaleType);
        }

        return view;
    }
}
