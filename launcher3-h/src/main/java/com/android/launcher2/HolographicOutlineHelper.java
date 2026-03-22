package com.android.launcher2;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

public class HolographicOutlineHelper {
    public static final int MAX_OUTER_BLUR_RADIUS;
    public static final int MIN_OUTER_BLUR_RADIUS;
    // TableMaskFilter is not part of the public SDK; keep this null for standalone builds.
    private static final MaskFilter sCoarseClipTable = null;
    private static final BlurMaskFilter sExtraThickInnerBlurMaskFilter;
    private static final BlurMaskFilter sExtraThickOuterBlurMaskFilter;
    private static final BlurMaskFilter sMediumInnerBlurMaskFilter;
    private static final BlurMaskFilter sMediumOuterBlurMaskFilter;
    private static final BlurMaskFilter sThickInnerBlurMaskFilter;
    private static final BlurMaskFilter sThickOuterBlurMaskFilter;
    private static final BlurMaskFilter sThinOuterBlurMaskFilter;
    private final Paint mAlphaClipPaint = new Paint();
    private final Paint mBlurPaint = new Paint();
    private final Paint mErasePaint = new Paint();
    private final Paint mHolographicPaint = new Paint();
    private int[] mTempOffset = new int[2];

    static {
        float scale = LauncherApplication.getScreenDensity();
        if (scale <= 0f) scale = 2.0f; // fallback if Application.onCreate hasn't run yet
        MIN_OUTER_BLUR_RADIUS = Math.max(1, (int) (scale * 1.0f));
        MAX_OUTER_BLUR_RADIUS = Math.max(1, (int) (scale * 12.0f));
        sExtraThickOuterBlurMaskFilter = new BlurMaskFilter(Math.max(1f, 12.0f * scale), BlurMaskFilter.Blur.OUTER);
        sThickOuterBlurMaskFilter = new BlurMaskFilter(Math.max(1f, scale * 6.0f), BlurMaskFilter.Blur.OUTER);
        sMediumOuterBlurMaskFilter = new BlurMaskFilter(Math.max(1f, scale * 2.0f), BlurMaskFilter.Blur.OUTER);
        sThinOuterBlurMaskFilter = new BlurMaskFilter(Math.max(1f, scale * 1.0f), BlurMaskFilter.Blur.OUTER);
        sExtraThickInnerBlurMaskFilter = new BlurMaskFilter(Math.max(1f, scale * 6.0f), BlurMaskFilter.Blur.NORMAL);
        sThickInnerBlurMaskFilter = new BlurMaskFilter(Math.max(1f, 4.0f * scale), BlurMaskFilter.Blur.NORMAL);
        sMediumInnerBlurMaskFilter = new BlurMaskFilter(Math.max(1f, scale * 2.0f), BlurMaskFilter.Blur.NORMAL);
    }

    HolographicOutlineHelper() {
        this.mHolographicPaint.setFilterBitmap(true);
        this.mHolographicPaint.setAntiAlias(true);
        this.mBlurPaint.setFilterBitmap(true);
        this.mBlurPaint.setAntiAlias(true);
        this.mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        this.mErasePaint.setFilterBitmap(true);
        this.mErasePaint.setAntiAlias(true);
        // TableMaskFilter is not available in the public SDK. This is an optional
        // quality enhancement for the glow and can be safely omitted.
        this.mAlphaClipPaint.setMaskFilter(null);
    }

    public float highlightAlphaInterpolator(float r) {
        return (float) Math.pow((double) ((1.0f - r) * 0.8f), 1.5d);
    }

    public float viewAlphaInterpolator(float r) {
        if (r < 0.95f) {
            return (float) Math.pow((double) (r / 0.95f), 1.5d);
        }
        return 1.0f;
    }

    /* access modifiers changed from: package-private */
    public void applyOuterBlur(Bitmap bitmap, Canvas canvas, int color) {
        this.mBlurPaint.setMaskFilter(sThickOuterBlurMaskFilter);
        Bitmap glow = bitmap.extractAlpha(this.mBlurPaint, this.mTempOffset);
        this.mHolographicPaint.setMaskFilter(sCoarseClipTable);
        this.mHolographicPaint.setAlpha(150);
        this.mHolographicPaint.setColor(color);
        canvas.drawBitmap(glow, (float) this.mTempOffset[0], (float) this.mTempOffset[1], this.mHolographicPaint);
        glow.recycle();
    }

    /* access modifiers changed from: package-private */
    public void applyExpensiveOutlineWithBlur(Bitmap srcDst, Canvas srcDstCanvas, int color, int outlineColor, int thickness) {
        BlurMaskFilter outerBlurMaskFilter;
        BlurMaskFilter innerBlurMaskFilter;
        Bitmap glowShape = srcDst.extractAlpha(this.mAlphaClipPaint, this.mTempOffset);
        switch (thickness) {
            case 0:
                outerBlurMaskFilter = sThickOuterBlurMaskFilter;
                break;
            case 1:
                outerBlurMaskFilter = sMediumOuterBlurMaskFilter;
                break;
            case 2:
                outerBlurMaskFilter = sExtraThickOuterBlurMaskFilter;
                break;
            default:
                throw new RuntimeException("Invalid blur thickness");
        }
        this.mBlurPaint.setMaskFilter(outerBlurMaskFilter);
        int[] outerBlurOffset = new int[2];
        Bitmap thickOuterBlur = glowShape.extractAlpha(this.mBlurPaint, outerBlurOffset);
        if (thickness == 2) {
            this.mBlurPaint.setMaskFilter(sMediumOuterBlurMaskFilter);
        } else {
            this.mBlurPaint.setMaskFilter(sThinOuterBlurMaskFilter);
        }
        int[] brightOutlineOffset = new int[2];
        Bitmap brightOutline = glowShape.extractAlpha(this.mBlurPaint, brightOutlineOffset);
        srcDstCanvas.setBitmap(glowShape);
        srcDstCanvas.drawColor(-16777216, PorterDuff.Mode.SRC_OUT);
        switch (thickness) {
            case 0:
                innerBlurMaskFilter = sThickInnerBlurMaskFilter;
                break;
            case 1:
                innerBlurMaskFilter = sMediumInnerBlurMaskFilter;
                break;
            case 2:
                innerBlurMaskFilter = sExtraThickInnerBlurMaskFilter;
                break;
            default:
                throw new RuntimeException("Invalid blur thickness");
        }
        this.mBlurPaint.setMaskFilter(innerBlurMaskFilter);
        int[] thickInnerBlurOffset = new int[2];
        Bitmap thickInnerBlur = glowShape.extractAlpha(this.mBlurPaint, thickInnerBlurOffset);
        srcDstCanvas.setBitmap(thickInnerBlur);
        srcDstCanvas.drawBitmap(glowShape, (float) (-thickInnerBlurOffset[0]), (float) (-thickInnerBlurOffset[1]), this.mErasePaint);
        srcDstCanvas.drawRect(0.0f, 0.0f, (float) (-thickInnerBlurOffset[0]), (float) thickInnerBlur.getHeight(), this.mErasePaint);
        srcDstCanvas.drawRect(0.0f, 0.0f, (float) thickInnerBlur.getWidth(), (float) (-thickInnerBlurOffset[1]), this.mErasePaint);
        srcDstCanvas.setBitmap(srcDst);
        srcDstCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        this.mHolographicPaint.setColor(color);
        srcDstCanvas.drawBitmap(thickInnerBlur, (float) thickInnerBlurOffset[0], (float) thickInnerBlurOffset[1], this.mHolographicPaint);
        srcDstCanvas.drawBitmap(thickOuterBlur, (float) outerBlurOffset[0], (float) outerBlurOffset[1], this.mHolographicPaint);
        this.mHolographicPaint.setColor(outlineColor);
        srcDstCanvas.drawBitmap(brightOutline, (float) brightOutlineOffset[0], (float) brightOutlineOffset[1], this.mHolographicPaint);
        brightOutline.recycle();
        thickOuterBlur.recycle();
        thickInnerBlur.recycle();
        glowShape.recycle();
    }

    /* access modifiers changed from: package-private */
    public void applyExtraThickExpensiveOutlineWithBlur(Bitmap srcDst, Canvas srcDstCanvas, int color, int outlineColor) {
        applyExpensiveOutlineWithBlur(srcDst, srcDstCanvas, color, outlineColor, 2);
    }

    /* access modifiers changed from: package-private */
    public void applyThickExpensiveOutlineWithBlur(Bitmap srcDst, Canvas srcDstCanvas, int color, int outlineColor) {
        applyExpensiveOutlineWithBlur(srcDst, srcDstCanvas, color, outlineColor, 0);
    }

    /* access modifiers changed from: package-private */
    public void applyMediumExpensiveOutlineWithBlur(Bitmap srcDst, Canvas srcDstCanvas, int color, int outlineColor) {
        applyExpensiveOutlineWithBlur(srcDst, srcDstCanvas, color, outlineColor, 1);
    }
}
