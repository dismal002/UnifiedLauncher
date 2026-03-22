package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class ClippedImageView extends ImageView {
    private final int mZone;

    public ClippedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClippedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ClippedImageView, defStyle, 0);
        this.mZone = a.getDimensionPixelSize(R.styleable.ClippedImageView_ignoreZone, 0);
        a.recycle();
    }

    public boolean onTouchEvent(MotionEvent event) {
        int zone = this.mZone;
        return (zone == 0 || zone <= 0 || event.getX() < ((float) zone)) && (zone >= 0 || event.getX() >= ((float) (getWidth() + zone))) && super.onTouchEvent(event);
    }
}
