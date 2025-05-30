package com.example.gaanesuno;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class TypeWriter extends AppCompatTextView {

    private CharSequence mText;
    private int mIndex;
    private long mDelay = 80; // delay in ms for each character
    private Handler mHandler = new Handler();

    public TypeWriter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            setText(mText.subSequence(0, mIndex++));
            if (mIndex <= mText.length()) {
                mHandler.postDelayed(characterAdder, mDelay);
            }
        }
    };

    public void animateText(CharSequence text) {
        mText = text;
        mIndex = 0;
        setText("");
        mHandler.removeCallbacks(characterAdder);
        mHandler.postDelayed(characterAdder, mDelay);
    }

    public void setCharacterDelay(long millis) {
        mDelay = millis;
    }
}
