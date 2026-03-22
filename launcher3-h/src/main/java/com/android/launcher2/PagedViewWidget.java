package com.android.launcher2;
import com.launcher3h.R;

import android.animation.ObjectAnimator;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PagedViewWidget extends LinearLayout implements Checkable {
    /* access modifiers changed from: private */
    public static HolographicOutlineHelper sHolographicOutlineHelper;
    private static final HandlerThread sWorkerThread = new HandlerThread("pagedviewwidget-helper");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper()) {
        private DeferredHandler mHandler = new DeferredHandler();

        public void handleMessage(Message msg) {
            final PagedViewWidget widget = (PagedViewWidget) msg.obj;
            int prevAlpha = widget.mPreview.getAlpha();
            int width = Math.max(widget.mPreview.getIntrinsicWidth(), widget.getMeasuredWidth());
            int height = Math.max(widget.mPreview.getIntrinsicHeight(), widget.getMeasuredHeight());
            final Bitmap outline = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            widget.mHolographicOutlineCanvas.setBitmap(outline);
            widget.mHolographicOutlineCanvas.save();
            widget.mHolographicOutlineCanvas.translate((float) widget.getPaddingLeft(), (float) widget.getPaddingTop());
            widget.mPreview.setAlpha(255);
            widget.mPreview.draw(widget.mHolographicOutlineCanvas);
            widget.mPreview.setAlpha(prevAlpha);
            widget.mHolographicOutlineCanvas.drawColor(Color.argb(156, 0, 0, 0), PorterDuff.Mode.SRC_OVER);
            widget.mHolographicOutlineCanvas.restore();
            widget.mEraseStrokeRect.set(0, 0, width, height);
            widget.mHolographicOutlineCanvas.drawRect(widget.mEraseStrokeRect, widget.mEraseStrokeRectPaint);
            PagedViewWidget.sHolographicOutlineHelper.applyThickExpensiveOutlineWithBlur(outline, widget.mHolographicOutlineCanvas, widget.mHoloBlurColor, widget.mHoloOutlineColor);
            this.mHandler.post(new Runnable() {
                public void run() {
                    Bitmap unused = widget.mHolographicOutline = outline;
                    widget.invalidate();
                }
            });
        }
    };
    private int mAlpha;
    private float mCheckedAlpha;
    private ObjectAnimator mCheckedAlphaAnimator;
    private int mCheckedFadeInDuration;
    private int mCheckedFadeOutDuration;
    /* access modifiers changed from: private */
    public final Rect mEraseStrokeRect;
    /* access modifiers changed from: private */
    public final Paint mEraseStrokeRectPaint;
    /* access modifiers changed from: private */
    public int mHoloBlurColor;
    /* access modifiers changed from: private */
    public int mHoloOutlineColor;
    private int mHolographicAlpha;
    /* access modifiers changed from: private */
    public Bitmap mHolographicOutline;
    /* access modifiers changed from: private */
    public final Canvas mHolographicOutlineCanvas;
    private boolean mIsChecked;
    private final Paint mPaint;
    /* access modifiers changed from: private */
    public FastBitmapDrawable mPreview;
    private ImageView mPreviewImageView;
    private final RectF mTmpScaleRect;

    public PagedViewWidget(Context context) {
        this(context, (AttributeSet) null);
    }

    public PagedViewWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPaint = new Paint();
        this.mHolographicOutlineCanvas = new Canvas();
        this.mTmpScaleRect = new RectF();
        this.mEraseStrokeRect = new Rect();
        this.mEraseStrokeRectPaint = new Paint();
        this.mAlpha = 255;
        this.mCheckedAlpha = 1.0f;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedViewWidget, defStyle, 0);
        this.mHoloBlurColor = a.getColor(R.styleable.PagedViewWidget_blurColor, 0);
        this.mHoloOutlineColor = a.getColor(R.styleable.PagedViewWidget_outlineColor, 0);
        this.mEraseStrokeRectPaint.setStyle(Paint.Style.STROKE);
        this.mEraseStrokeRectPaint.setStrokeWidth((float) HolographicOutlineHelper.MIN_OUTER_BLUR_RADIUS);
        this.mEraseStrokeRectPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        this.mEraseStrokeRectPaint.setFilterBitmap(true);
        this.mEraseStrokeRectPaint.setAntiAlias(true);
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
        setWillNotDraw(false);
        setClipToPadding(false);
    }

    private void queueHolographicOutlineCreation() {
        if (this.mHolographicOutline == null && this.mPreview != null) {
            Message m = sWorker.obtainMessage(1);
            m.obj = this;
            sWorker.sendMessage(m);
        }
    }

    public void applyFromAppWidgetProviderInfo(AppWidgetProviderInfo info, FastBitmapDrawable preview, int maxWidth, int[] cellSpan, PagedViewIconCache cache, boolean createHolographicOutline) {
        ImageView image = (ImageView) findViewById(R.id.widget_preview);
        image.setMaxWidth(maxWidth);
        image.setImageDrawable(preview);
        this.mPreviewImageView = image;
        TextView name = (TextView) findViewById(R.id.widget_name);
        name.setText(info.label);
        name.setLayerType(1, (Paint) null);
        TextView dims = (TextView) findViewById(R.id.widget_dims);
        dims.setText(getContext().getString(R.string.widget_dims_format, new Object[]{Integer.valueOf(cellSpan[0]), Integer.valueOf(cellSpan[1])}));
        dims.setLayerType(1, (Paint) null);
        if (createHolographicOutline) {
            this.mPreview = preview;
        }
    }

    public void applyFromWallpaperInfo(ResolveInfo info, PackageManager packageManager, FastBitmapDrawable preview, int maxWidth, PagedViewIconCache cache, boolean createHolographicOutline) {
        ImageView image = (ImageView) findViewById(R.id.wallpaper_preview);
        image.setMaxWidth(maxWidth);
        image.setImageDrawable(preview);
        this.mPreviewImageView = image;
        TextView name = (TextView) findViewById(R.id.wallpaper_name);
        name.setText(info.loadLabel(packageManager));
        name.setLayerType(1, (Paint) null);
        if (createHolographicOutline) {
            this.mPreview = preview;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!super.onTouchEvent(event)) {
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (this.mAlpha > 0) {
            super.onDraw(canvas);
        }
        if (this.mHolographicOutline != null && this.mHolographicAlpha > 0) {
            this.mTmpScaleRect.set(0.0f, 0.0f, 1.0f, 1.0f);
            this.mPreviewImageView.getImageMatrix().mapRect(this.mTmpScaleRect);
            this.mPaint.setAlpha(this.mHolographicAlpha);
            canvas.save();
            canvas.scale(this.mTmpScaleRect.right, this.mTmpScaleRect.bottom);
            canvas.drawBitmap(this.mHolographicOutline, 0.0f, 0.0f, this.mPaint);
            canvas.restore();
        }
    }

    /* access modifiers changed from: protected */
    public boolean onSetAlpha(int alpha) {
        return true;
    }

    public void setAlpha(float alpha) {
        float viewAlpha = sHolographicOutlineHelper.viewAlphaInterpolator(alpha);
        int newViewAlpha = (int) (viewAlpha * 255.0f);
        int newHolographicAlpha = (int) (sHolographicOutlineHelper.highlightAlphaInterpolator(alpha) * 255.0f);
        if (this.mAlpha != newViewAlpha || this.mHolographicAlpha != newHolographicAlpha) {
            this.mAlpha = newViewAlpha;
            this.mHolographicAlpha = newHolographicAlpha;
            setChildrenAlpha(viewAlpha);
            super.setAlpha(viewAlpha);
        }
    }

    private void setChildrenAlpha(float alpha) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setAlpha(alpha);
        }
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w > 0 && h > 0) {
            queueHolographicOutlineCreation();
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        sWorker.removeMessages(1, this);
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

    public boolean isChecked() {
        return this.mIsChecked;
    }

    public void toggle() {
        setChecked(!this.mIsChecked);
    }
}
