package com.odonataworshop.audio.server;

import android.app.Activity;
import android.view.TextureView;
import android.widget.TextView;

/**
 * Created by test on 12/14/14.
 */
public class TextUpdateHandler {
    private Activity mActivity;
    private TextView mText;

    public TextView getText() {
        return mText;
    }

    public void setText(TextView aText) {
        this.mText = aText;
    }

    public TextUpdateHandler(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void post(final String aText){
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mText.append(aText + "\n");
            }
        });
    }
}
