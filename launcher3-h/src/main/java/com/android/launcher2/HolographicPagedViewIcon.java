package com.android.launcher2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.TextView;

public class HolographicPagedViewIcon extends TextView {
    PagedViewIcon mOriginalIcon;
    Paint mPaint = new Paint();

    public HolographicPagedViewIcon(Context context, PagedViewIcon original) {
        super(context);
        this.mOriginalIcon = original;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        Bitmap overlay = this.mOriginalIcon.getHolographicOutline();
        if (overlay != null) {
            int offset = getScrollX();
            int compoundPaddingLeft = getCompoundPaddingLeft();
            canvas.drawBitmap(overlay, (float) (offset + compoundPaddingLeft + ((((getWidth() - getCompoundPaddingRight()) - compoundPaddingLeft) - overlay.getWidth()) / 2)), (float) getPaddingTop(), this.mPaint);
        }
    }
}
