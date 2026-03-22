package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class AllAppsBackground extends View {
    private Drawable mBackground;

    public AllAppsBackground(Context context) {
        this(context, (AttributeSet) null);
    }

    public AllAppsBackground(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsBackground(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        try {
            this.mBackground = getResources().getDrawable(R.drawable.all_apps_bg_gradient);
        } catch (android.content.res.Resources.NotFoundException e) {
            // fallback: transparent
        }
    }

    public void onDraw(Canvas canvas) {
        if (this.mBackground == null) return;
        int sx = getScrollX();
        this.mBackground.setBounds(sx, 0, sx + getMeasuredWidth(), getMeasuredHeight());
        this.mBackground.draw(canvas);
    }
}
