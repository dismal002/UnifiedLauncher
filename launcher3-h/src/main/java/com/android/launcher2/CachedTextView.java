package com.android.launcher2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

public class CachedTextView extends TextView {
    private Bitmap mCache;
    private final Canvas mCacheCanvas = new Canvas();
    private final Paint mCachePaint = new Paint();
    private boolean mEnabled = true;
    private boolean mIsBuildingCache;
    boolean mIsTextCacheDirty;
    private float mPaddingH = 0.0f;
    private float mPaddingV = 0.0f;
    private int mPrevAlpha = -1;
    float mRectLeft;
    float mRectTop;
    private CharSequence mText;
    float mTextCacheLeft;
    float mTextCacheScrollX;
    float mTextCacheTop;

    public CachedTextView(Context context) {
        super(context);
    }

    public CachedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CachedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /* access modifiers changed from: protected */
    public int getCacheTopPadding() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public int getCacheLeftPadding() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public int getCacheRightPadding() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public int getCacheBottomPadding() {
        return 0;
    }

    public void disableCache() {
        this.mEnabled = false;
    }

    public void setText(CharSequence text, TextView.BufferType type) {
        super.setText(text, type);
        this.mIsTextCacheDirty = true;
    }

    private void buildAndUpdateCache() {
        Layout layout = getLayout();
        int left = getCompoundPaddingLeft();
        int top = getExtendedPaddingTop();
        float prevAlpha = getAlpha();
        this.mTextCacheLeft = layout.getLineLeft(0) - ((float) getCacheLeftPadding());
        this.mTextCacheTop = (((float) (layout.getLineTop(0) + top)) - this.mPaddingV) - ((float) getCacheTopPadding());
        this.mRectLeft = (float) (getScrollX() + getLeft());
        this.mRectTop = 0.0f;
        this.mTextCacheScrollX = (float) getScrollX();
        float textCacheRight = Math.min(((float) left) + layout.getLineRight(0) + this.mPaddingH, (float) (getScrollX() + getWidth())) + ((float) getCacheRightPadding());
        float textCacheBottom = ((float) (layout.getLineBottom(0) + top)) + this.mPaddingV + ((float) getCacheBottomPadding());
        int width = (int) ((textCacheRight - this.mTextCacheLeft) + (2.0f * getPaint().measureText("x")));
        int height = (int) (textCacheBottom - this.mTextCacheTop);
        if (width != 0 && height != 0) {
            if (!(this.mCache == null || (this.mCache.getWidth() == width && this.mCache.getHeight() == height))) {
                this.mCache.recycle();
                this.mCache = null;
            }
            if (this.mCache == null) {
                this.mCache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                this.mCacheCanvas.setBitmap(this.mCache);
            } else {
                this.mCacheCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            }
            this.mCacheCanvas.save();
            this.mCacheCanvas.translate(-this.mTextCacheLeft, -this.mTextCacheTop);
            this.mIsBuildingCache = true;
            setAlpha(1.0f);
            draw(this.mCacheCanvas);
            setAlpha(prevAlpha);
            this.mIsBuildingCache = false;
            this.mCacheCanvas.restore();
            this.mText = getText();
            setText(" ");
        }
    }

    public CharSequence getText() {
        return this.mText == null ? super.getText() : this.mText;
    }

    public void draw(Canvas canvas) {
        if (this.mEnabled && this.mIsTextCacheDirty && !this.mIsBuildingCache) {
            buildAndUpdateCache();
            this.mIsTextCacheDirty = false;
        }
        if (this.mCache != null && !this.mIsBuildingCache) {
            canvas.drawBitmap(this.mCache, (this.mTextCacheLeft - this.mTextCacheScrollX) + ((float) getScrollX()), this.mTextCacheTop, this.mCachePaint);
        }
        super.draw(canvas);
    }

    /* access modifiers changed from: protected */
    public boolean onSetAlpha(int alpha) {
        if (this.mPrevAlpha == alpha) {
            return true;
        }
        this.mPrevAlpha = alpha;
        this.mCachePaint.setAlpha(alpha);
        super.onSetAlpha(alpha);
        return true;
    }
}
