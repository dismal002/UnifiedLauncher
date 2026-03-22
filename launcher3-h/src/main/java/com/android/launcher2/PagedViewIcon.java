package com.android.launcher2;
import com.launcher3h.R;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.Checkable;
import com.android.launcher2.PagedViewIconCache;

public class PagedViewIcon extends CachedTextView implements Checkable {
    /* access modifiers changed from: private */
    public static HolographicOutlineHelper sHolographicOutlineHelper;
    private static final HandlerThread sWorkerThread = new HandlerThread("pagedviewicon-helper");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper()) {
        private DeferredHandler mHandler = new DeferredHandler();
        private Paint mPaint = new Paint();

        public void handleMessage(Message msg) {
            final PagedViewIcon icon = (PagedViewIcon) msg.obj;
            final Bitmap holographicOutline = Bitmap.createBitmap(icon.mIcon.getWidth(), icon.mIcon.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas holographicOutlineCanvas = new Canvas(holographicOutline);
            holographicOutlineCanvas.drawBitmap(icon.mIcon, 0.0f, 0.0f, this.mPaint);
            PagedViewIcon.sHolographicOutlineHelper.applyThickExpensiveOutlineWithBlur(holographicOutline, holographicOutlineCanvas, icon.mHoloBlurColor, icon.mHoloOutlineColor);
            this.mHandler.post(new Runnable() {
                public void run() {
                    Bitmap unused = icon.mHolographicOutline = holographicOutline;
                    icon.mIconCache.addOutline(icon.mIconCacheKey, holographicOutline);
                    icon.getHolographicOutlineView().invalidate();
                }
            });
        }
    };
    private int mAlpha;
    private float mCheckedAlpha;
    private ObjectAnimator mCheckedAlphaAnimator;
    private int mCheckedFadeInDuration;
    private int mCheckedFadeOutDuration;
    private Bitmap mCheckedOutline;
    /* access modifiers changed from: private */
    public int mHoloBlurColor;
    /* access modifiers changed from: private */
    public int mHoloOutlineColor;
    private int mHolographicAlpha;
    /* access modifiers changed from: private */
    public Bitmap mHolographicOutline;
    HolographicPagedViewIcon mHolographicOutlineView;
    /* access modifiers changed from: private */
    public Bitmap mIcon;
    /* access modifiers changed from: private */
    public PagedViewIconCache mIconCache;
    /* access modifiers changed from: private */
    public PagedViewIconCache.Key mIconCacheKey;
    private boolean mIsChecked;
    private final Paint mPaint;

    public PagedViewIcon(Context context) {
        this(context, (AttributeSet) null);
    }

    public PagedViewIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPaint = new Paint();
        this.mAlpha = 255;
        this.mCheckedAlpha = 1.0f;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedViewIcon, defStyle, 0);
        this.mHoloBlurColor = a.getColor(R.styleable.PagedViewIcon_blurColor, 0);
        this.mHoloOutlineColor = a.getColor(R.styleable.PagedViewIcon_outlineColor, 0);
        a.recycle();
        if (sHolographicOutlineHelper == null) {
            sHolographicOutlineHelper = new HolographicOutlineHelper();
        }
        Resources r = context.getResources();
        if (r.getInteger(R.integer.icon_allAppsCustomizeFadeAlpha) > 0) {
            this.mCheckedAlpha = ((float) r.getInteger(R.integer.icon_allAppsCustomizeFadeAlpha)) / 256.0f;
            this.mCheckedFadeInDuration = r.getInteger(R.integer.icon_allAppsCustomizeFadeInTime);
            this.mCheckedFadeOutDuration = r.getInteger(R.integer.icon_allAppsCustomizeFadeOutTime);
        }
        setFocusable(true);
        setBackgroundDrawable((Drawable) null);
        this.mHolographicOutlineView = new HolographicPagedViewIcon(context, this);
    }

    /* access modifiers changed from: protected */
    public HolographicPagedViewIcon getHolographicOutlineView() {
        return this.mHolographicOutlineView;
    }

    /* access modifiers changed from: protected */
    public Bitmap getHolographicOutline() {
        return this.mHolographicOutline;
    }

    private boolean queueHolographicOutlineCreation() {
        if (this.mHolographicOutline != null) {
            return false;
        }
        Message m = sWorker.obtainMessage(1);
        m.obj = this;
        sWorker.sendMessage(m);
        return true;
    }

    public void applyFromApplicationInfo(ApplicationInfo info, PagedViewIconCache cache, boolean scaleUp, boolean createHolographicOutlines) {
        this.mIcon = info.iconBitmap;
        setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new FastBitmapDrawable(this.mIcon), (Drawable) null, (Drawable) null);
        setText(info.title);
        setTag(info);
        if (createHolographicOutlines) {
            this.mIconCache = cache;
            this.mIconCacheKey = new PagedViewIconCache.Key(info);
            this.mHolographicOutline = this.mIconCache.getOutline(this.mIconCacheKey);
            if (!queueHolographicOutlineCreation()) {
                getHolographicOutlineView().invalidate();
            }
        }
    }

    public void applyFromResolveInfo(ResolveInfo info, PackageManager packageManager, PagedViewIconCache cache, IconCache modelIconCache, boolean createHolographicOutlines) {
        this.mIcon = Utilities.createIconBitmap(modelIconCache.getFullResIcon(info, packageManager), getContext());
        setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new FastBitmapDrawable(this.mIcon), (Drawable) null, (Drawable) null);
        setText(info.loadLabel(packageManager));
        setTag(info);
        if (createHolographicOutlines) {
            this.mIconCache = cache;
            this.mIconCacheKey = new PagedViewIconCache.Key(info);
            this.mHolographicOutline = this.mIconCache.getOutline(this.mIconCacheKey);
            if (!queueHolographicOutlineCreation()) {
                getHolographicOutlineView().invalidate();
            }
        }
    }

    public void setAlpha(float alpha) {
        float viewAlpha = sHolographicOutlineHelper.viewAlphaInterpolator(alpha);
        int newViewAlpha = (int) (viewAlpha * 255.0f);
        int newHolographicAlpha = (int) (sHolographicOutlineHelper.highlightAlphaInterpolator(alpha) * 255.0f);
        if (this.mAlpha != newViewAlpha || this.mHolographicAlpha != newHolographicAlpha) {
            this.mAlpha = newViewAlpha;
            this.mHolographicAlpha = newHolographicAlpha;
            super.setAlpha(viewAlpha);
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (this.mAlpha > 0) {
            super.onDraw(canvas);
        }
        Bitmap overlay = null;
        if (this.mCheckedOutline != null) {
            this.mPaint.setAlpha(255);
            overlay = this.mCheckedOutline;
        } else if (this.mHolographicOutline != null && this.mHolographicAlpha > 0) {
            this.mPaint.setAlpha(this.mHolographicAlpha);
            overlay = this.mHolographicOutline;
        }
        if (overlay != null) {
            int offset = getScrollX();
            int compoundPaddingLeft = getCompoundPaddingLeft();
            canvas.drawBitmap(overlay, (float) (offset + compoundPaddingLeft + ((((getWidth() - getCompoundPaddingRight()) - compoundPaddingLeft) - overlay.getWidth()) / 2)), (float) getPaddingTop(), this.mPaint);
        }
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        sWorker.removeMessages(1, this);
    }

    public boolean isChecked() {
        return this.mIsChecked;
    }

    /* access modifiers changed from: package-private */
    public void setChecked(boolean checked, boolean animate) {
        float alpha;
        int duration;
        if (this.mIsChecked != checked) {
            this.mIsChecked = checked;
            if (this.mIsChecked) {
                alpha = this.mCheckedAlpha;
                duration = this.mCheckedFadeInDuration;
            } else {
                alpha = 1.0f;
                duration = this.mCheckedFadeOutDuration;
            }
            if (this.mCheckedAlphaAnimator != null) {
                this.mCheckedAlphaAnimator.cancel();
            }
            if (animate) {
                this.mCheckedAlphaAnimator = ObjectAnimator.ofFloat(this, "alpha", new float[]{getAlpha(), alpha});
                this.mCheckedAlphaAnimator.setDuration((long) duration);
                this.mCheckedAlphaAnimator.start();
            } else {
                setAlpha(alpha);
            }
            invalidate();
        }
    }

    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    public void toggle() {
        setChecked(!this.mIsChecked);
    }
}
