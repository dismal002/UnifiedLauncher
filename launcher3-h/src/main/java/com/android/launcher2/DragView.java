package com.android.launcher2;
import com.launcher3h.R;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

public class DragView extends View {
    ValueAnimator mAnim;
    private Bitmap mBitmap;
    private int mDragRegionHeight;
    private int mDragRegionLeft = 0;
    private int mDragRegionTop = 0;
    private int mDragRegionWidth;
    /* access modifiers changed from: private */
    public WindowManager.LayoutParams mLayoutParams;
    /* access modifiers changed from: private */
    public float mOffsetX = 0.0f;
    /* access modifiers changed from: private */
    public float mOffsetY = 0.0f;
    private Runnable mOnDrawRunnable = null;
    private Paint mPaint;
    private int mRegistrationX;
    private int mRegistrationY;
    /* access modifiers changed from: private */
    public WindowManager mWindowManager;

    static /* synthetic */ float access$016(DragView x0, float x1) {
        float f = x0.mOffsetX + x1;
        x0.mOffsetX = f;
        return f;
    }

    static /* synthetic */ float access$116(DragView x0, float x1) {
        float f = x0.mOffsetY + x1;
        x0.mOffsetY = f;
        return f;
    }

    public DragView(Context context, Bitmap bitmap, int registrationX, int registrationY, int left, int top, int width, int height) {
        super(context);
        Resources res = getResources();
        int dragScale = res.getInteger(R.integer.config_dragViewExtraPixels);
        this.mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Matrix scale = new Matrix();
        float scaleFactor = (float) ((width + dragScale) / width);
        if (scaleFactor != 1.0f) {
            scale.setScale(scaleFactor, scaleFactor);
        }
        int offsetX = res.getInteger(R.integer.config_dragViewOffsetX);
        int offsetY = res.getInteger(R.integer.config_dragViewOffsetY);
        this.mAnim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mAnim.setDuration(110);
        this.mAnim.setInterpolator(new DecelerateInterpolator(2.5f));
        final int i = offsetX;
        final int i2 = offsetY;
        this.mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) animation.getAnimatedValue()).floatValue();
                int deltaX = (int) ((((float) i) * value) - DragView.this.mOffsetX);
                int deltaY = (int) ((((float) i2) * value) - DragView.this.mOffsetY);
                DragView.access$016(DragView.this, (float) deltaX);
                DragView.access$116(DragView.this, (float) deltaY);
                if (DragView.this.getParent() == null) {
                    animation.cancel();
                    return;
                }
                WindowManager.LayoutParams lp = DragView.this.mLayoutParams;
                lp.x += deltaX;
                lp.y += deltaY;
                DragView.this.mWindowManager.updateViewLayout(DragView.this, lp);
            }
        });
        this.mBitmap = Bitmap.createBitmap(bitmap, left, top, width, height, scale, true);
        setDragRegion(0, 0, width, height);
        this.mRegistrationX = registrationX;
        this.mRegistrationY = registrationY;
        int ms = View.MeasureSpec.makeMeasureSpec(0, 0);
        measure(ms, ms);
    }

    public float getOffsetY() {
        return this.mOffsetY;
    }

    public void setDragRegion(int left, int top, int width, int height) {
        this.mDragRegionLeft = left;
        this.mDragRegionTop = top;
        this.mDragRegionWidth = width;
        this.mDragRegionHeight = height;
    }

    public void setOnDrawRunnable(Runnable r) {
        this.mOnDrawRunnable = r;
    }

    public int getDragRegionLeft() {
        return this.mDragRegionLeft;
    }

    public int getDragRegionTop() {
        return this.mDragRegionTop;
    }

    public int getDragRegionWidth() {
        return this.mDragRegionWidth;
    }

    public int getDragRegionHeight() {
        return this.mDragRegionHeight;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(this.mBitmap.getWidth(), this.mBitmap.getHeight());
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (!(getParent() == null || this.mOnDrawRunnable == null)) {
            this.mOnDrawRunnable.run();
            this.mOnDrawRunnable = null;
        }
        canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, this.mPaint);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mBitmap.recycle();
    }

    public void setPaint(Paint paint) {
        this.mPaint = paint;
        invalidate();
    }

    public void show(IBinder windowToken, int touchX, int touchY) {
        if (this.mWindowManager == null) {
            return;
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-2, -2, touchX - this.mRegistrationX, touchY - this.mRegistrationY, 1002, 768, -3);
        lp.gravity = 51;
        lp.token = windowToken;
        lp.setTitle("DragView");
        this.mLayoutParams = lp;
        this.mWindowManager.addView(this, lp);
        this.mAnim.start();
    }

    /* access modifiers changed from: package-private */
    public void move(int touchX, int touchY) {
        if (this.mWindowManager == null) {
            return;
        }
        WindowManager.LayoutParams lp = this.mLayoutParams;
        lp.x = (touchX - this.mRegistrationX) + ((int) this.mOffsetX);
        lp.y = (touchY - this.mRegistrationY) + ((int) this.mOffsetY);
        this.mWindowManager.updateViewLayout(this, lp);
    }

    /* access modifiers changed from: package-private */
    public void remove() {
        if (this.mWindowManager != null) {
            this.mWindowManager.removeView(this);
        }
    }

    /* access modifiers changed from: package-private */
    public int[] getPosition(int[] result) {
        WindowManager.LayoutParams lp = this.mLayoutParams;
        if (result == null) {
            result = new int[2];
        }
        result[0] = lp.x;
        result[1] = lp.y;
        return result;
    }
}
