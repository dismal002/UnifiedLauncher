package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class BubbleTextView extends TextView {
    private Drawable mBackground;
    private boolean mBackgroundSizeChanged;
    private float mBubbleColorAlpha;
    private boolean mDidInvalidateForPressedState;
    private int mFocusedGlowColor;
    private int mFocusedOutlineColor;
    private VisibilityChangedListener mOnVisibilityChangedListener;
    private final HolographicOutlineHelper mOutlineHelper = new HolographicOutlineHelper();
    private Paint mPaint;
    private int mPressedGlowColor;
    private Bitmap mPressedOrFocusedBackground;
    private int mPressedOutlineColor;
    private int mPrevAlpha = -1;
    private boolean mStayPressed;
    private final Canvas mTempCanvas = new Canvas();
    private final Paint mTempPaint = new Paint();
    private final Rect mTempRect = new Rect();

    public BubbleTextView(Context context) {
        super(context);
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.mBackground = getBackground();
        setFocusable(true);
        setBackgroundDrawable((Drawable) null);
        Resources res = getContext().getResources();
        int bubbleColor = res.getColor(R.color.bubble_dark_background);
        this.mPaint = new Paint(1);
        this.mPaint.setColor(bubbleColor);
        this.mBubbleColorAlpha = ((float) Color.alpha(bubbleColor)) / 255.0f;
        this.mFocusedOutlineColor = res.getColor(R.color.workspace_item_focused_outline_color);
        this.mFocusedGlowColor = res.getColor(R.color.workspace_item_focused_glow_color);
        this.mPressedOutlineColor = res.getColor(R.color.workspace_item_pressed_outline_color);
        this.mPressedGlowColor = res.getColor(R.color.workspace_item_pressed_glow_color);
        setShadowLayer(4.0f, 0.0f, 2.0f, -872415232);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache) {
        setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new FastBitmapDrawable(info.getIcon(iconCache)), (Drawable) null, (Drawable) null);
        setText(info.title);
        setTag(info);
    }

    /* access modifiers changed from: protected */
    public boolean setFrame(int left, int top, int right, int bottom) {
        if (!(getLeft() == left && getRight() == right && getTop() == top && getBottom() == bottom)) {
            this.mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable who) {
        return who == this.mBackground || super.verifyDrawable(who);
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        boolean backgroundEmptyBefore;
        boolean backgroundEmptyNow;
        if (!isPressed()) {
            if (this.mPressedOrFocusedBackground == null) {
                backgroundEmptyBefore = true;
            } else {
                backgroundEmptyBefore = false;
            }
            if (!this.mStayPressed) {
                this.mPressedOrFocusedBackground = null;
            }
            if (isFocused()) {
                if (getLayout() == null) {
                    this.mPressedOrFocusedBackground = null;
                } else {
                    this.mPressedOrFocusedBackground = createGlowingOutline(this.mTempCanvas, this.mFocusedGlowColor, this.mFocusedOutlineColor);
                }
                this.mStayPressed = false;
                setCellLayoutPressedOrFocusedIcon();
            }
            if (this.mPressedOrFocusedBackground == null) {
                backgroundEmptyNow = true;
            } else {
                backgroundEmptyNow = false;
            }
            if (!backgroundEmptyBefore && backgroundEmptyNow) {
                setCellLayoutPressedOrFocusedIcon();
            }
        } else if (!this.mDidInvalidateForPressedState) {
            setCellLayoutPressedOrFocusedIcon();
        }
        Drawable d = this.mBackground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    private void drawWithPadding(Canvas destCanvas, int padding) {
        Rect clipRect = this.mTempRect;
        getDrawingRect(clipRect);
        clipRect.bottom = (getExtendedPaddingTop() - 3) + getLayout().getLineTop(0);
        destCanvas.save();
        destCanvas.translate((float) ((-getScrollX()) + (padding / 2)), (float) ((-getScrollY()) + (padding / 2)));
        destCanvas.clipRect(clipRect);
        draw(destCanvas);
        destCanvas.restore();
    }

    private Bitmap createGlowingOutline(Canvas canvas, int outlineColor, int glowColor) {
        int padding = HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS;
        Bitmap b = Bitmap.createBitmap(getWidth() + padding, getHeight() + padding, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(b);
        drawWithPadding(canvas, padding);
        this.mOutlineHelper.applyExtraThickExpensiveOutlineWithBlur(b, canvas, glowColor, outlineColor);
        return b;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        switch (event.getAction()) {
            case 0:
                if (this.mPressedOrFocusedBackground == null) {
                    this.mPressedOrFocusedBackground = createGlowingOutline(this.mTempCanvas, this.mPressedGlowColor, this.mPressedOutlineColor);
                }
                if (!isPressed()) {
                    this.mDidInvalidateForPressedState = false;
                    break;
                } else {
                    this.mDidInvalidateForPressedState = true;
                    invalidate();
                    break;
                }
            case 1:
            case MotionEvent.ACTION_CANCEL /*3*/:
                if (!isPressed()) {
                    this.mPressedOrFocusedBackground = null;
                    break;
                }
                break;
        }
        return result;
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        if (this.mOnVisibilityChangedListener != null) {
            this.mOnVisibilityChangedListener.receiveVisibilityChangedMessage(this);
        }
        super.onVisibilityChanged(changedView, visibility);
    }

    /* access modifiers changed from: package-private */
    public void setStayPressed(boolean stayPressed) {
        this.mStayPressed = stayPressed;
        if (!stayPressed) {
            this.mPressedOrFocusedBackground = null;
        }
        setCellLayoutPressedOrFocusedIcon();
    }

    /* access modifiers changed from: package-private */
    public void setCellLayoutPressedOrFocusedIcon() {
        CellLayoutChildren parent = (CellLayoutChildren) getParent();
        if (parent != null) {
            ((CellLayout) parent.getParent()).setPressedOrFocusedIcon(this.mPressedOrFocusedBackground != null ? this : null);
        }
    }

    /* access modifiers changed from: package-private */
    public Bitmap getPressedOrFocusedBackground() {
        return this.mPressedOrFocusedBackground;
    }

    /* access modifiers changed from: package-private */
    public int getPressedOrFocusedBackgroundPadding() {
        return HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS / 2;
    }

    public void draw(Canvas canvas) {
        Drawable background = this.mBackground;
        if (background != null) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            if (this.mBackgroundSizeChanged) {
                background.setBounds(0, 0, getWidth(), getHeight());
                this.mBackgroundSizeChanged = false;
            }
            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
            } else {
                canvas.translate((float) scrollX, (float) scrollY);
                background.draw(canvas);
                canvas.translate((float) (-scrollX), (float) (-scrollY));
            }
        }
        getPaint().setShadowLayer(4.0f, 0.0f, 2.0f, -872415232);
        super.draw(canvas);
        canvas.save();
        canvas.clipRect((float) getScrollX(), (float) (getScrollY() + getExtendedPaddingTop()), (float) (getScrollX() + getWidth()), (float) (getScrollY() + getHeight()));
        getPaint().setShadowLayer(1.75f, 0.0f, 0.0f, -1157627904);
        super.draw(canvas);
        canvas.restore();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mBackground != null) {
            this.mBackground.setCallback(this);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mBackground != null) {
            this.mBackground.setCallback((Drawable.Callback) null);
        }
    }

    /* access modifiers changed from: protected */
    public boolean onSetAlpha(int alpha) {
        if (this.mPrevAlpha == alpha) {
            return true;
        }
        this.mPrevAlpha = alpha;
        this.mPaint.setAlpha((int) (((float) alpha) * this.mBubbleColorAlpha));
        super.onSetAlpha(alpha);
        return true;
    }
}
