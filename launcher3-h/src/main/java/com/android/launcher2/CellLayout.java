package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import java.lang.reflect.Array;
import java.util.Arrays;

public class CellLayout extends ViewGroup {
    private boolean mAcceptsDrops;
    private Drawable mActiveBackground;
    private Drawable mActiveBackgroundMini;
    private Drawable mActiveGlowBackground;
    private Drawable mActiveGlowBackgroundMini;
    private float mBackgroundAlpha;
    private float mBackgroundAlphaMultiplier;
    private Rect mBackgroundRect;
    private int mBottomPadding;
    private int mCellHeight;
    private final CellInfo mCellInfo;
    private int mCellWidth;
    private CellLayoutChildren mChildren;
    private int mCountX;
    private int mCountY;
    private InterruptibleInOutAnimator mCrosshairsAnimator;
    private Drawable mCrosshairsDrawable;
    /* access modifiers changed from: private */
    public float mCrosshairsVisibility;
    private final int[] mDragCell;
    private final Point mDragCenter;
    /* access modifiers changed from: private */
    public float[] mDragOutlineAlphas;
    private InterruptibleInOutAnimator[] mDragOutlineAnims;
    private int mDragOutlineCurrent;
    private final Paint mDragOutlinePaint;
    /* access modifiers changed from: private */
    public Point[] mDragOutlines;
    private boolean mDragging;
    private TimeInterpolator mEaseOutInterpolator;
    private float mGlowBackgroundAlpha;
    private Rect mGlowBackgroundRect;
    private float mGlowBackgroundScale;
    private int mHeightGap;
    private View.OnTouchListener mInterceptTouchListener;
    private boolean mIsDefaultDropTarget;
    private boolean mIsDragOccuring;
    private boolean mIsDragOverlapping;
    private int mLeftPadding;
    private Drawable mNormalBackground;
    private Drawable mNormalBackgroundMini;
    private Drawable mNormalGlowBackgroundMini;
    boolean[][] mOccupied;
    private BubbleTextView mPressedOrFocusedIcon;
    private final Rect mRect;
    private int mRightPadding;
    private final int[] mTmpCellXY;
    private final int[] mTmpPoint;
    private final PointF mTmpPointF;
    private int mTopPadding;
    private int mWidthGap;

    public CellLayout(Context context) {
        this(context, (AttributeSet) null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mRect = new Rect();
        this.mCellInfo = new CellInfo();
        this.mTmpCellXY = new int[2];
        this.mTmpPoint = new int[2];
        this.mTmpPointF = new PointF();
        this.mBackgroundAlphaMultiplier = 1.0f;
        this.mAcceptsDrops = false;
        this.mIsDragOverlapping = false;
        this.mIsDragOccuring = false;
        this.mIsDefaultDropTarget = false;
        this.mDragCenter = new Point();
        this.mDragOutlines = new Point[8];
        this.mDragOutlineAlphas = new float[this.mDragOutlines.length];
        this.mDragOutlineAnims = new InterruptibleInOutAnimator[this.mDragOutlines.length];
        this.mDragOutlineCurrent = 0;
        this.mDragOutlinePaint = new Paint();
        this.mCrosshairsDrawable = null;
        this.mCrosshairsAnimator = null;
        this.mCrosshairsVisibility = 0.0f;
        this.mDragCell = new int[2];
        this.mDragging = false;
        setWillNotDraw(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);
        this.mCellWidth = a.getDimensionPixelSize(R.styleable.CellLayout_cellWidth, 10);
        this.mCellHeight = a.getDimensionPixelSize(R.styleable.CellLayout_cellHeight, 10);
        this.mWidthGap = a.getDimensionPixelSize(R.styleable.CellLayout_widthGap, -1);
        this.mHeightGap = a.getDimensionPixelSize(R.styleable.CellLayout_heightGap, -1);
        this.mLeftPadding = a.getDimensionPixelSize(R.styleable.CellLayout_xAxisStartPadding, 10);
        this.mRightPadding = a.getDimensionPixelSize(R.styleable.CellLayout_xAxisEndPadding, 10);
        this.mTopPadding = a.getDimensionPixelSize(R.styleable.CellLayout_yAxisStartPadding, 10);
        this.mBottomPadding = a.getDimensionPixelSize(R.styleable.CellLayout_yAxisEndPadding, 10);
        this.mCountX = LauncherModel.getCellCountX();
        this.mCountY = LauncherModel.getCellCountY();
        this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        a.recycle();
        setAlwaysDrawnWithCacheEnabled(false);
        Resources res = getResources();
        if (LauncherApplication.isScreenXLarge()) {
            this.mNormalBackground = res.getDrawable(R.drawable.homescreen_large_blue);
            this.mActiveBackground = res.getDrawable(R.drawable.homescreen_large_green);
            this.mActiveGlowBackground = res.getDrawable(R.drawable.homescreen_large_green_strong);
            this.mNormalBackgroundMini = res.getDrawable(R.drawable.homescreen_small_blue);
            this.mNormalGlowBackgroundMini = res.getDrawable(R.drawable.homescreen_small_blue_strong);
            this.mActiveBackgroundMini = res.getDrawable(R.drawable.homescreen_small_green);
            this.mActiveGlowBackgroundMini = res.getDrawable(R.drawable.homescreen_small_green_strong);
            this.mNormalBackground.setFilterBitmap(true);
            this.mActiveBackground.setFilterBitmap(true);
            this.mActiveGlowBackground.setFilterBitmap(true);
            this.mNormalBackgroundMini.setFilterBitmap(true);
            this.mNormalGlowBackgroundMini.setFilterBitmap(true);
            this.mActiveBackgroundMini.setFilterBitmap(true);
            this.mActiveGlowBackgroundMini.setFilterBitmap(true);
        }
        this.mCrosshairsDrawable = res.getDrawable(R.drawable.gardening_crosshairs);
        this.mEaseOutInterpolator = new DecelerateInterpolator(2.5f);
        this.mCrosshairsAnimator = new InterruptibleInOutAnimator((long) res.getInteger(R.integer.config_crosshairsFadeInTime), 0.0f, 1.0f);
        this.mCrosshairsAnimator.getAnimator().addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = CellLayout.this.mCrosshairsVisibility = ((Float) animation.getAnimatedValue()).floatValue();
                CellLayout.this.invalidate();
            }
        });
        this.mCrosshairsAnimator.getAnimator().setInterpolator(this.mEaseOutInterpolator);
        for (int i = 0; i < this.mDragOutlines.length; i++) {
            this.mDragOutlines[i] = new Point(-1, -1);
        }
        int duration = res.getInteger(R.integer.config_dragOutlineFadeTime);
        float toAlphaValue = (float) res.getInteger(R.integer.config_dragOutlineMaxAlpha);
        Arrays.fill(this.mDragOutlineAlphas, 0.0f);
        for (int i2 = 0; i2 < this.mDragOutlineAnims.length; i2++) {
            InterruptibleInOutAnimator interruptibleInOutAnimator = new InterruptibleInOutAnimator((long) duration, 0.0f, toAlphaValue);
            interruptibleInOutAnimator.getAnimator().setInterpolator(this.mEaseOutInterpolator);
            final InterruptibleInOutAnimator interruptibleInOutAnimator2 = interruptibleInOutAnimator;
            final int i3 = i2;
            interruptibleInOutAnimator.getAnimator().addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Bitmap outline = (Bitmap) interruptibleInOutAnimator2.getTag();
                    if (outline == null) {
                        animation.cancel();
                        return;
                    }
                    CellLayout.this.mDragOutlineAlphas[i3] = ((Float) animation.getAnimatedValue()).floatValue();
                    int left = CellLayout.this.mDragOutlines[i3].x;
                    int top = CellLayout.this.mDragOutlines[i3].y;
                    CellLayout.this.invalidate(left, top, outline.getWidth() + left, outline.getHeight() + top);
                }
            });
            final InterruptibleInOutAnimator interruptibleInOutAnimator3 = interruptibleInOutAnimator;
            interruptibleInOutAnimator.getAnimator().addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (((Float) ((ValueAnimator) animation).getAnimatedValue()).floatValue() == 0.0f) {
                        interruptibleInOutAnimator3.setTag((Object) null);
                    }
                }
            });
            this.mDragOutlineAnims[i2] = interruptibleInOutAnimator;
        }
        this.mBackgroundRect = new Rect();
        this.mGlowBackgroundRect = new Rect();
        setHoverScale(1.0f);
        setHoverAlpha(1.0f);
        this.mChildren = new CellLayoutChildren(context);
        this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mLeftPadding, this.mTopPadding, this.mWidthGap, this.mHeightGap);
        addView(this.mChildren);
    }

    static int widthInPortrait(Resources r, int numCells) {
        return ((numCells - 1) * r.getDimensionPixelSize(R.dimen.workspace_width_gap_port)) + (r.getDimensionPixelSize(R.dimen.workspace_cell_width) * numCells);
    }

    static int heightInLandscape(Resources r, int numCells) {
        return ((numCells - 1) * r.getDimensionPixelSize(R.dimen.workspace_height_gap_land)) + (r.getDimensionPixelSize(R.dimen.workspace_cell_height) * numCells);
    }

    private void invalidateBubbleTextView(BubbleTextView icon) {
        int padding = icon.getPressedOrFocusedBackgroundPadding();
        invalidate(icon.getLeft() - padding, icon.getTop() - padding, icon.getRight() + padding, icon.getBottom() + padding);
    }

    /* access modifiers changed from: package-private */
    public void setPressedOrFocusedIcon(BubbleTextView icon) {
        BubbleTextView oldIcon = this.mPressedOrFocusedIcon;
        this.mPressedOrFocusedIcon = icon;
        if (oldIcon != null) {
            invalidateBubbleTextView(oldIcon);
        }
        if (this.mPressedOrFocusedIcon != null) {
            invalidateBubbleTextView(this.mPressedOrFocusedIcon);
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 1 */
    public CellLayoutChildren getChildrenLayout() {
        if (getChildCount() > 0) {
            return (CellLayoutChildren) getChildAt(0);
        }
        return null;
    }

    public void setIsDefaultDropTarget(boolean isDefaultDropTarget) {
        if (this.mIsDefaultDropTarget != isDefaultDropTarget) {
            this.mIsDefaultDropTarget = isDefaultDropTarget;
            invalidate();
        }
    }

    /* access modifiers changed from: package-private */
    public void setIsDragOccuring(boolean isDragOccuring) {
        if (this.mIsDragOccuring != isDragOccuring) {
            this.mIsDragOccuring = isDragOccuring;
            invalidate();
        }
    }

    /* access modifiers changed from: package-private */
    public void setIsDragOverlapping(boolean isDragOverlapping) {
        if (this.mIsDragOverlapping != isDragOverlapping) {
            this.mIsDragOverlapping = isDragOverlapping;
            invalidate();
        }
    }

    /* access modifiers changed from: package-private */
    public boolean getIsDragOverlapping() {
        return this.mIsDragOverlapping;
    }

    private void updateGlowRect() {
        float marginFraction = (this.mGlowBackgroundScale - 1.0f) / 2.0f;
        int marginX = (int) (((float) (this.mBackgroundRect.right - this.mBackgroundRect.left)) * marginFraction);
        int marginY = (int) (((float) (this.mBackgroundRect.bottom - this.mBackgroundRect.top)) * marginFraction);
        this.mGlowBackgroundRect.set(this.mBackgroundRect.left - marginX, this.mBackgroundRect.top - marginY, this.mBackgroundRect.right + marginX, this.mBackgroundRect.bottom + marginY);
        invalidate();
    }

    public void setHoverScale(float scaleFactor) {
        if (scaleFactor != this.mGlowBackgroundScale) {
            this.mGlowBackgroundScale = scaleFactor;
            updateGlowRect();
            if (getParent() != null) {
                ((View) getParent()).invalidate();
            }
        }
    }

    public float getHoverScale() {
        return this.mGlowBackgroundScale;
    }

    public float getHoverAlpha() {
        return this.mGlowBackgroundAlpha;
    }

    public void setHoverAlpha(float alpha) {
        this.mGlowBackgroundAlpha = alpha;
        invalidate();
    }

    /* access modifiers changed from: package-private */
    public void animateDrop() {
        if (LauncherApplication.isScreenXLarge()) {
            Resources res = getResources();
            ObjectAnimator scaleUp = ObjectAnimator.ofFloat(this, "hoverScale", new float[]{((float) res.getInteger(R.integer.config_screenOnDropScalePercent)) / 100.0f});
            scaleUp.setDuration((long) res.getInteger(R.integer.config_screenOnDropScaleUpDuration));
            ObjectAnimator scaleDown = ObjectAnimator.ofFloat(this, "hoverScale", new float[]{1.0f});
            scaleDown.setDuration((long) res.getInteger(R.integer.config_screenOnDropScaleDownDuration));
            ObjectAnimator alphaFadeOut = ObjectAnimator.ofFloat(this, "hoverAlpha", new float[]{0.0f});
            alphaFadeOut.setStartDelay((long) res.getInteger(R.integer.config_screenOnDropAlphaFadeDelay));
            alphaFadeOut.setDuration((long) res.getInteger(R.integer.config_screenOnDropAlphaFadeDelay));
            AnimatorSet bouncer = new AnimatorSet();
            bouncer.play(scaleUp).before(scaleDown);
            bouncer.play(scaleUp).with(alphaFadeOut);
            bouncer.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    CellLayout.this.setIsDragOverlapping(true);
                }

                public void onAnimationEnd(Animator animation) {
                    CellLayout.this.setIsDragOverlapping(false);
                    CellLayout.this.setHoverScale(1.0f);
                    CellLayout.this.setHoverAlpha(1.0f);
                }
            });
            bouncer.start();
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        Drawable bg;
        if (LauncherApplication.isScreenXLarge() && this.mBackgroundAlpha > 0.0f) {
            boolean mini = getScaleX() < 0.5f;
            if (this.mIsDragOverlapping) {
                bg = mini ? this.mActiveBackgroundMini : this.mActiveGlowBackground;
            } else if (!this.mIsDragOccuring || !this.mAcceptsDrops) {
                bg = (!this.mIsDefaultDropTarget || !mini) ? mini ? this.mNormalBackgroundMini : this.mNormalBackground : this.mNormalGlowBackgroundMini;
            } else {
                bg = mini ? this.mActiveBackgroundMini : this.mActiveBackground;
            }
            bg.setAlpha((int) (this.mBackgroundAlpha * this.mBackgroundAlphaMultiplier * 255.0f));
            bg.setBounds(this.mBackgroundRect);
            bg.draw(canvas);
            if (mini && this.mIsDragOverlapping) {
                boolean modifiedClipRect = false;
                if (this.mGlowBackgroundScale > 1.0f) {
                    float marginFraction = (this.mGlowBackgroundScale - 1.0f) / 2.0f;
                    Rect clipRect = canvas.getClipBounds();
                    int marginX = (int) (((float) (clipRect.right - clipRect.left)) * marginFraction);
                    int marginY = (int) (((float) (clipRect.bottom - clipRect.top)) * marginFraction);
                    canvas.save();
                    // Region.Op.REPLACE is banned on API 29+; skip the clip expansion
                    // so the glow can draw slightly outside bounds without crashing.
                    modifiedClipRect = true;
                }
                this.mActiveGlowBackgroundMini.setAlpha((int) (this.mBackgroundAlpha * this.mGlowBackgroundAlpha * 255.0f));
                this.mActiveGlowBackgroundMini.setBounds(this.mGlowBackgroundRect);
                this.mActiveGlowBackgroundMini.draw(canvas);
                if (modifiedClipRect) {
                    canvas.restore();
                }
            }
        }
        if (this.mCrosshairsVisibility > 0.0f) {
            int countX = this.mCountX;
            int countY = this.mCountY;
            Drawable d = this.mCrosshairsDrawable;
            int width = d.getIntrinsicWidth();
            int height = d.getIntrinsicHeight();
            int x = (getLeftPadding() - (this.mWidthGap / 2)) - (width / 2);
            for (int col = 0; col <= countX; col++) {
                int y = (getTopPadding() - (this.mHeightGap / 2)) - (height / 2);
                for (int row = 0; row <= countY; row++) {
                    this.mTmpPointF.set((float) (x - this.mDragCenter.x), (float) (y - this.mDragCenter.y));
                    float alpha = Math.min(0.4f, 0.002f * (600.0f - this.mTmpPointF.length()));
                    if (alpha > 0.0f) {
                        d.setBounds(x, y, x + width, y + height);
                        d.setAlpha((int) (255.0f * alpha * this.mCrosshairsVisibility));
                        d.draw(canvas);
                    }
                    y += this.mCellHeight + this.mHeightGap;
                }
                x += this.mCellWidth + this.mWidthGap;
            }
        }
        Paint paint = this.mDragOutlinePaint;
        for (int i = 0; i < this.mDragOutlines.length; i++) {
            float alpha2 = this.mDragOutlineAlphas[i];
            if (alpha2 > 0.0f) {
                Point p = this.mDragOutlines[i];
                paint.setAlpha((int) (0.5f + alpha2));
                canvas.drawBitmap((Bitmap) this.mDragOutlineAnims[i].getTag(), (float) p.x, (float) p.y, paint);
            }
        }
        if (this.mPressedOrFocusedIcon != null) {
            int padding = this.mPressedOrFocusedIcon.getPressedOrFocusedBackgroundPadding();
            Bitmap b = this.mPressedOrFocusedIcon.getPressedOrFocusedBackground();
            if (b != null) {
                canvas.drawBitmap(b, (float) (this.mPressedOrFocusedIcon.getLeft() - padding), (float) (this.mPressedOrFocusedIcon.getTop() - padding), (Paint) null);
            }
        }
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).cancelLongPress();
        }
    }

    public void setOnInterceptTouchListener(View.OnTouchListener listener) {
        this.mInterceptTouchListener = listener;
    }

    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params, boolean markCells) {
        LayoutParams lp = params;
        if (lp.cellX < 0 || lp.cellX > this.mCountX - 1 || lp.cellY < 0 || lp.cellY > this.mCountY - 1) {
            return false;
        }
        if (lp.cellHSpan < 0) {
            lp.cellHSpan = this.mCountX;
        }
        if (lp.cellVSpan < 0) {
            lp.cellVSpan = this.mCountY;
        }
        child.setId(childId);
        this.mChildren.addView(child, index, lp);
        if (markCells) {
            markCellsAsOccupiedForView(child);
        }
        return true;
    }

    public void setAcceptsDrops(boolean acceptsDrops) {
        if (this.mAcceptsDrops != acceptsDrops) {
            this.mAcceptsDrops = acceptsDrops;
            invalidate();
        }
    }

    public boolean getAcceptsDrops() {
        return this.mAcceptsDrops;
    }

    public void removeAllViews() {
        clearOccupiedCells();
        this.mChildren.removeAllViews();
    }

    public void removeAllViewsInLayout() {
        clearOccupiedCells();
        this.mChildren.removeAllViewsInLayout();
    }

    public void removeViewWithoutMarkingCells(View view) {
        this.mChildren.removeView(view);
    }

    public void removeView(View view) {
        markCellsAsUnoccupiedForView(view);
        this.mChildren.removeView(view);
    }

    public void removeViewAt(int index) {
        markCellsAsUnoccupiedForView(this.mChildren.getChildAt(index));
        this.mChildren.removeViewAt(index);
    }

    public void removeViewInLayout(View view) {
        markCellsAsUnoccupiedForView(view);
        this.mChildren.removeViewInLayout(view);
    }

    public void removeViews(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(this.mChildren.getChildAt(i));
        }
        this.mChildren.removeViews(start, count);
    }

    public void removeViewsInLayout(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(this.mChildren.getChildAt(i));
        }
        this.mChildren.removeViewsInLayout(start, count);
    }

    public void drawChildren(Canvas canvas) {
        this.mChildren.draw(canvas);
    }

    /* access modifiers changed from: package-private */
    public void buildChildrenLayer() {
        this.mChildren.buildLayer();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mCellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this);
    }

    public void setTagToCellInfoForPoint(int touchX, int touchY) {
        CellInfo cellInfo = this.mCellInfo;
        Rect frame = this.mRect;
        int x = touchX + getScrollX();
        int y = touchY + getScrollY();
        boolean found = false;
        int i = this.mChildren.getChildCount() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            View child = this.mChildren.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if ((child.getVisibility() == 0 || child.getAnimation() != null) && lp.isLockedToGrid) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    cellInfo.cell = child;
                    cellInfo.cellX = lp.cellX;
                    cellInfo.cellY = lp.cellY;
                    cellInfo.spanX = lp.cellHSpan;
                    cellInfo.spanY = lp.cellVSpan;
                    cellInfo.valid = true;
                    found = true;
                    break;
                }
            }
            i--;
        }
        if (!found) {
            int[] cellXY = this.mTmpCellXY;
            pointToCellExact(x, y, cellXY);
            cellInfo.cell = null;
            cellInfo.cellX = cellXY[0];
            cellInfo.cellY = cellXY[1];
            cellInfo.spanX = 1;
            cellInfo.spanY = 1;
            cellInfo.valid = cellXY[0] >= 0 && cellXY[1] >= 0 && cellXY[0] < this.mCountX && cellXY[1] < this.mCountY && !this.mOccupied[cellXY[0]][cellXY[1]];
        }
        setTag(cellInfo);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mInterceptTouchListener != null && this.mInterceptTouchListener.onTouch(this, ev)) {
            return true;
        }
        int action = ev.getAction();
        CellInfo cellInfo = this.mCellInfo;
        if (action == 0) {
            setTagToCellInfoForPoint((int) ev.getX(), (int) ev.getY());
        } else if (action == 1) {
            cellInfo.cell = null;
            cellInfo.cellX = -1;
            cellInfo.cellY = -1;
            cellInfo.spanX = 0;
            cellInfo.spanY = 0;
            cellInfo.valid = false;
            setTag(cellInfo);
        }
        return false;
    }

    public CellInfo getTag() {
        return (CellInfo) super.getTag();
    }

    /* access modifiers changed from: package-private */
    public void pointToCellExact(int x, int y, int[] result) {
        int hStartPadding = getLeftPadding();
        int vStartPadding = getTopPadding();
        result[0] = (x - hStartPadding) / (this.mCellWidth + this.mWidthGap);
        result[1] = (y - vStartPadding) / (this.mCellHeight + this.mHeightGap);
        int xAxis = this.mCountX;
        int yAxis = this.mCountY;
        if (result[0] < 0) {
            result[0] = 0;
        }
        if (result[0] >= xAxis) {
            result[0] = xAxis - 1;
        }
        if (result[1] < 0) {
            result[1] = 0;
        }
        if (result[1] >= yAxis) {
            result[1] = yAxis - 1;
        }
    }

    /* access modifiers changed from: package-private */
    public void pointToCellRounded(int x, int y, int[] result) {
        pointToCellExact((this.mCellWidth / 2) + x, (this.mCellHeight / 2) + y, result);
    }

    /* access modifiers changed from: package-private */
    public void cellToPoint(int cellX, int cellY, int[] result) {
        int hStartPadding = getLeftPadding();
        int vStartPadding = getTopPadding();
        result[0] = ((this.mCellWidth + this.mWidthGap) * cellX) + hStartPadding;
        result[1] = ((this.mCellHeight + this.mHeightGap) * cellY) + vStartPadding;
    }

    /* access modifiers changed from: package-private */
    public int getCellWidth() {
        return this.mCellWidth;
    }

    /* access modifiers changed from: package-private */
    public int getCellHeight() {
        return this.mCellHeight;
    }

    /* access modifiers changed from: package-private */
    public int getWidthGap() {
        return this.mWidthGap;
    }

    /* access modifiers changed from: package-private */
    public int getHeightGap() {
        return this.mHeightGap;
    }

    /* access modifiers changed from: package-private */
    public int getLeftPadding() {
        return this.mLeftPadding;
    }

    /* access modifiers changed from: package-private */
    public int getTopPadding() {
        return this.mTopPadding;
    }

    /* access modifiers changed from: package-private */
    public int getRightPadding() {
        return this.mRightPadding;
    }

    /* access modifiers changed from: package-private */
    public int getBottomPadding() {
        return this.mBottomPadding;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == 0 || heightSpecMode == 0) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        int cellWidth = this.mCellWidth;
        int cellHeight = this.mCellHeight;
        int numWidthGaps = this.mCountX - 1;
        int numHeightGaps = this.mCountY - 1;
        if (this.mWidthGap < 0 || this.mHeightGap < 0) {
            this.mHeightGap = (((heightSpecSize - this.mTopPadding) - this.mBottomPadding) - (this.mCountY * cellHeight)) / numHeightGaps;
            this.mWidthGap = (((widthSpecSize - this.mLeftPadding) - this.mRightPadding) - (this.mCountX * cellWidth)) / numWidthGaps;
            int minGap = Math.min(this.mWidthGap, this.mHeightGap);
            this.mHeightGap = minGap;
            this.mWidthGap = minGap;
        }
        int newWidth = widthSpecSize;
        int newHeight = heightSpecSize;
        if (widthSpecMode == Integer.MIN_VALUE) {
            newWidth = this.mLeftPadding + this.mRightPadding + (this.mCountX * cellWidth) + ((this.mCountX - 1) * this.mWidthGap);
            newHeight = this.mTopPadding + this.mBottomPadding + (this.mCountY * cellHeight) + ((this.mCountY - 1) * this.mHeightGap);
            setMeasuredDimension(newWidth, newHeight);
        }
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(View.MeasureSpec.makeMeasureSpec(newWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(newHeight, 1073741824));
        }
        setMeasuredDimension(newWidth, newHeight);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).layout(0, 0, r - l, b - t);
        }
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mBackgroundRect.set(0, 0, w, h);
        updateGlowRect();
    }

    /* access modifiers changed from: protected */
    public void setChildrenDrawingCacheEnabled(boolean enabled) {
        this.mChildren.setChildrenDrawingCacheEnabled(enabled);
    }

    /* access modifiers changed from: protected */
    public void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        this.mChildren.setChildrenDrawnWithCacheEnabled(enabled);
    }

    public float getBackgroundAlpha() {
        return this.mBackgroundAlpha;
    }

    public void setFastBackgroundAlpha(float alpha) {
        this.mBackgroundAlpha = alpha;
    }

    public void setBackgroundAlphaMultiplier(float multiplier) {
        this.mBackgroundAlphaMultiplier = multiplier;
    }

    public float getBackgroundAlphaMultiplier() {
        return this.mBackgroundAlphaMultiplier;
    }

    public void setBackgroundAlpha(float alpha) {
        this.mBackgroundAlpha = alpha;
        invalidate();
    }

    /* access modifiers changed from: protected */
    public boolean onSetAlpha(int alpha) {
        return true;
    }

    public void setAlpha(float alpha) {
        setChildrenAlpha(alpha);
        super.setAlpha(alpha);
    }

    public void setFastAlpha(float alpha) {
        // setFastAlpha was an internal optimization on older platform builds.
        // Use the public property setters for standalone builds.
        setFastChildrenAlpha(alpha);
        super.setAlpha(alpha);
    }

    private void setChildrenAlpha(float alpha) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setAlpha(alpha);
        }
    }

    private void setFastChildrenAlpha(float alpha) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setAlpha(alpha);
        }
    }

    public View getChildAt(int x, int y) {
        return this.mChildren.getChildAt(x, y);
    }

    /* access modifiers changed from: package-private */
    public void estimateDropCell(int originX, int originY, int spanX, int spanY, int[] result) {
        int countX = this.mCountX;
        int countY = this.mCountY;
        pointToCellRounded(originX, originY, result);
        int rightOverhang = (result[0] + spanX) - countX;
        if (rightOverhang > 0) {
            result[0] = result[0] - rightOverhang;
        }
        result[0] = Math.max(0, result[0]);
        int bottomOverhang = (result[1] + spanY) - countY;
        if (bottomOverhang > 0) {
            result[1] = result[1] - bottomOverhang;
        }
        result[1] = Math.max(0, result[1]);
    }

    /* access modifiers changed from: package-private */
    public void visualizeDropLocation(View v, Bitmap dragOutline, int originX, int originY, int spanX, int spanY) {
        int oldDragCellX = this.mDragCell[0];
        int oldDragCellY = this.mDragCell[1];
        int[] nearest = findNearestVacantArea(originX, originY, spanX, spanY, v, this.mDragCell);
        if (v != null) {
            this.mDragCenter.set((v.getWidth() / 2) + originX, (v.getHeight() / 2) + originY);
        } else {
            this.mDragCenter.set(originX, originY);
        }
        if (!(nearest == null || (nearest[0] == oldDragCellX && nearest[1] == oldDragCellY))) {
            int[] topLeft = this.mTmpPoint;
            cellToPoint(nearest[0], nearest[1], topLeft);
            int left = topLeft[0];
            int top = topLeft[1];
            if (v != null) {
                if (v.getParent() instanceof CellLayout) {
                    LayoutParams lp = (LayoutParams) v.getLayoutParams();
                    left += lp.leftMargin;
                    top += lp.topMargin;
                }
                left += (v.getWidth() - dragOutline.getWidth()) / 2;
                top += (v.getHeight() - dragOutline.getHeight()) / 2;
            }
            int oldIndex = this.mDragOutlineCurrent;
            this.mDragOutlineAnims[oldIndex].animateOut();
            this.mDragOutlineCurrent = (oldIndex + 1) % this.mDragOutlines.length;
            this.mDragOutlines[this.mDragOutlineCurrent].set(left, top);
            this.mDragOutlineAnims[this.mDragOutlineCurrent].setTag(dragOutline);
            this.mDragOutlineAnims[this.mDragOutlineCurrent].animateIn();
        }
        if (this.mCrosshairsDrawable != null) {
            invalidate();
        }
    }

    /* access modifiers changed from: package-private */
    public int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestVacantArea(pixelX, pixelY, spanX, spanY, (View) null, result);
    }

    /* access modifiers changed from: package-private */
    public int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY, View ignoreView, int[] result) {
        int[] bestXY;
        markCellsAsUnoccupiedForView(ignoreView);
        if (result != null) {
            bestXY = result;
        } else {
            bestXY = new int[2];
        }
        double bestDistance = Double.MAX_VALUE;
        int countX = this.mCountX;
        int countY = this.mCountY;
        boolean[][] occupied = this.mOccupied;
        int xLimit = countX - spanX;
        int yLimit = countY - spanY;
        if (xLimit >= 0 && yLimit >= 0) {
            for (int y = 0; y <= yLimit; y++) {
                for (int x = 0; x <= xLimit; x++) {
                    boolean available = true;
                    for (int i = 0; i < spanX && available; i++) {
                        for (int j = 0; j < spanY; j++) {
                            if (occupied[x + i][y + j]) {
                                available = false;
                                break;
                            }
                        }
                    }
                    if (available) {
                        int[] cellXY = this.mTmpCellXY;
                        cellToPoint(x, y, cellXY);
                        double distance = Math.sqrt(Math.pow((double) (cellXY[0] - pixelX), 2.0d) + Math.pow((double) (cellXY[1] - pixelY), 2.0d));
                        if (distance <= bestDistance) {
                            bestDistance = distance;
                            bestXY[0] = x;
                            bestXY[1] = y;
                        }
                    }
                }
            }
        }
        markCellsAsOccupiedForView(ignoreView);
        if (bestDistance < Double.MAX_VALUE) {
            return bestXY;
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public boolean existsEmptyCell() {
        return findCellForSpan((int[]) null, 1, 1);
    }

    /* access modifiers changed from: package-private */
    public boolean findCellForSpan(int[] cellXY, int spanX, int spanY) {
        return findCellForSpanThatIntersectsIgnoring(cellXY, spanX, spanY, -1, -1, (View) null);
    }

    /* access modifiers changed from: package-private */
    public boolean findCellForSpanIgnoring(int[] cellXY, int spanX, int spanY, View ignoreView) {
        return findCellForSpanThatIntersectsIgnoring(cellXY, spanX, spanY, -1, -1, ignoreView);
    }

    /* access modifiers changed from: package-private */
    public boolean findCellForSpanThatIntersects(int[] cellXY, int spanX, int spanY, int intersectX, int intersectY) {
        return findCellForSpanThatIntersectsIgnoring(cellXY, spanX, spanY, intersectX, intersectY, (View) null);
    }

    /* access modifiers changed from: package-private */
    public boolean findCellForSpanThatIntersectsIgnoring(int[] cellXY, int spanX, int spanY, int intersectX, int intersectY, View ignoreView) {
        markCellsAsUnoccupiedForView(ignoreView);
        boolean foundCell = false;
        while (true) {
            int startX = 0;
            if (intersectX >= 0) {
                startX = Math.max(0, intersectX - (spanX - 1));
            }
            int endX = this.mCountX - (spanX - 1);
            if (intersectX >= 0) {
                endX = Math.min(endX, (spanX - 1) + intersectX + (spanX == 1 ? 1 : 0));
            }
            int startY = 0;
            if (intersectY >= 0) {
                startY = Math.max(0, intersectY - (spanY - 1));
            }
            int endY = this.mCountY - (spanY - 1);
            if (intersectY >= 0) {
                endY = Math.min(endY, (spanY - 1) + intersectY + (spanY == 1 ? 1 : 0));
            }
            for (int y = startY; y < endY && !foundCell; y++) {
                int x = startX;
                while (x < endX) {
                    boolean available = true;
                    for (int i = 0; i < spanX; i++) {
                        for (int j = 0; j < spanY; j++) {
                            if (this.mOccupied[x + i][y + j]) {
                                available = false;
                                x = x + i;
                                break;
                            }
                        }
                        if (!available) {
                            break;
                        }
                    }
                    if (available) {
                        if (cellXY != null) {
                            cellXY[0] = x;
                            cellXY[1] = y;
                        }
                        foundCell = true;
                        break;
                    }
                    x++;
                }
            }
            if (intersectX == -1 && intersectY == -1) {
                markCellsAsOccupiedForView(ignoreView);
                return foundCell;
            }
            intersectX = -1;
            intersectY = -1;
        }
    }

    /* access modifiers changed from: package-private */
    public void onDragExit() {
        if (this.mDragging) {
            this.mDragging = false;
            if (this.mCrosshairsAnimator != null) {
                this.mCrosshairsAnimator.animateOut();
            }
        }
        this.mDragCell[0] = -1;
        this.mDragCell[1] = -1;
        this.mDragOutlineAnims[this.mDragOutlineCurrent].animateOut();
        this.mDragOutlineCurrent = (this.mDragOutlineCurrent + 1) % this.mDragOutlineAnims.length;
        setIsDragOverlapping(false);
    }

    /* access modifiers changed from: package-private */
    public void onDropChild(View child, boolean animate) {
        int i;
        if (child != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.isDragging = false;
            lp.dropped = true;
            lp.animateDrop = animate;
            if (animate) {
                i = 4;
            } else {
                i = 0;
            }
            child.setVisibility(i);
            child.requestLayout();
        }
    }

    /* access modifiers changed from: package-private */
    public void onDragChild(View child) {
        ((LayoutParams) child.getLayoutParams()).isDragging = true;
    }

    /* access modifiers changed from: package-private */
    public void onDragEnter() {
        if (!this.mDragging && this.mCrosshairsAnimator != null) {
            this.mCrosshairsAnimator.animateIn();
        }
        this.mDragging = true;
    }

    public int[] rectToCell(int width, int height, int[] result) {
        return rectToCell(getResources(), width, height, result);
    }

    public static int[] rectToCell(Resources resources, int width, int height, int[] result) {
        int smallerSize = Math.min(resources.getDimensionPixelSize(R.dimen.workspace_cell_width), resources.getDimensionPixelSize(R.dimen.workspace_cell_height));
        int spanX = (width + smallerSize) / smallerSize;
        int spanY = (height + smallerSize) / smallerSize;
        if (result == null) {
            return new int[]{spanX, spanY};
        }
        result[0] = spanX;
        result[1] = spanY;
        return result;
    }

    public int[] cellSpansToSize(int hSpans, int vSpans) {
        return new int[]{(this.mCellWidth * hSpans) + ((hSpans - 1) * this.mWidthGap), (this.mCellHeight * vSpans) + ((vSpans - 1) * this.mHeightGap)};
    }

    public void calculateSpans(ItemInfo info) {
        int minWidth;
        int minHeight;
        if (info instanceof LauncherAppWidgetInfo) {
            minWidth = ((LauncherAppWidgetInfo) info).minWidth;
            minHeight = ((LauncherAppWidgetInfo) info).minHeight;
        } else if (info instanceof PendingAddWidgetInfo) {
            minWidth = ((PendingAddWidgetInfo) info).minWidth;
            minHeight = ((PendingAddWidgetInfo) info).minHeight;
        } else {
            info.spanY = 1;
            info.spanX = 1;
            return;
        }
        int[] spans = rectToCell(minWidth, minHeight, (int[]) null);
        info.spanX = spans[0];
        info.spanY = spans[1];
    }

    static boolean findVacantCell(int[] vacant, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied) {
        int xLimit = xCount - spanX;
        int yLimit = yCount - spanY;
        if (xLimit < 0 || yLimit < 0) {
            return false;
        }
        for (int y = 0; y <= yLimit; y++) {
            for (int x = 0; x <= xLimit; x++) {
                boolean available = true;
                for (int i = 0; i < spanX && available; i++) {
                    for (int j = 0; j < spanY; j++) {
                        if (occupied[x + i][y + j]) {
                            available = false;
                            break;
                        }
                    }
                }
                if (available) {
                    if (vacant != null) {
                        vacant[0] = x;
                        vacant[1] = y;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void clearOccupiedCells() {
        for (int x = 0; x < this.mCountX; x++) {
            for (int y = 0; y < this.mCountY; y++) {
                this.mOccupied[x][y] = false;
            }
        }
    }

    public void getExpandabilityArrayForView(View view, int[] expandability) {
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        expandability[0] = 0;
        for (int x = lp.cellX - 1; x >= 0; x--) {
            boolean flag = false;
            for (int y = lp.cellY; y < lp.cellY + lp.cellVSpan; y++) {
                if (this.mOccupied[x][y]) {
                    flag = true;
                }
            }
            if (flag) {
                break;
            }
            expandability[0] = expandability[0] + 1;
        }
        expandability[1] = 0;
        for (int y2 = lp.cellY - 1; y2 >= 0; y2--) {
            boolean flag2 = false;
            for (int x2 = lp.cellX; x2 < lp.cellX + lp.cellHSpan; x2++) {
                if (this.mOccupied[x2][y2]) {
                    flag2 = true;
                }
            }
            if (flag2) {
                break;
            }
            expandability[1] = expandability[1] + 1;
        }
        expandability[2] = 0;
        for (int x3 = lp.cellX + lp.cellHSpan; x3 < this.mCountX; x3++) {
            boolean flag3 = false;
            for (int y3 = lp.cellY; y3 < lp.cellY + lp.cellVSpan; y3++) {
                if (this.mOccupied[x3][y3]) {
                    flag3 = true;
                }
            }
            if (flag3) {
                break;
            }
            expandability[2] = expandability[2] + 1;
        }
        expandability[3] = 0;
        int y4 = lp.cellY + lp.cellVSpan;
        while (y4 < this.mCountY) {
            boolean flag4 = false;
            for (int x4 = lp.cellX; x4 < lp.cellX + lp.cellHSpan; x4++) {
                if (this.mOccupied[x4][y4]) {
                    flag4 = true;
                }
            }
            if (!flag4) {
                expandability[3] = expandability[3] + 1;
                y4++;
            } else {
                return;
            }
        }
    }

    public void onMove(View view, int newCellX, int newCellY) {
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsAsUnoccupiedForView(view);
        markCellsForView(newCellX, newCellY, lp.cellHSpan, lp.cellVSpan, true);
    }

    public void markCellsAsOccupiedForView(View view) {
        if (view != null && view.getParent() == this.mChildren) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, true);
        }
    }

    public void markCellsAsUnoccupiedForView(View view) {
        if (view != null && view.getParent() == this.mChildren) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, false);
        }
    }

    private void markCellsForView(int cellX, int cellY, int spanX, int spanY, boolean value) {
        int x = cellX;
        while (x < cellX + spanX && x < this.mCountX) {
            int y = cellY;
            while (y < cellY + spanY && y < this.mCountY) {
                this.mOccupied[x][y] = value;
                y++;
            }
            x++;
        }
    }

    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /* access modifiers changed from: protected */
    public ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        boolean animateDrop;
        public int cellHSpan;
        public int cellVSpan;
        public int cellX;
        public int cellY;
        boolean dropped;
        public boolean isDragging;
        public boolean isLockedToGrid;
        int oldX;
        int oldY;
        int x;
        int y;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.isLockedToGrid = true;
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.isLockedToGrid = true;
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(int cellX2, int cellY2, int cellHSpan2, int cellVSpan2) {
            super(-1, -1);
            this.isLockedToGrid = true;
            this.cellX = cellX2;
            this.cellY = cellY2;
            this.cellHSpan = cellHSpan2;
            this.cellVSpan = cellVSpan2;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap, int hStartPadding, int vStartPadding) {
            if (this.isLockedToGrid) {
                int myCellHSpan = this.cellHSpan;
                int myCellVSpan = this.cellVSpan;
                int myCellX = this.cellX;
                int myCellY = this.cellY;
                this.width = (((myCellHSpan * cellWidth) + ((myCellHSpan - 1) * widthGap)) - this.leftMargin) - this.rightMargin;
                this.height = (((myCellVSpan * cellHeight) + ((myCellVSpan - 1) * heightGap)) - this.topMargin) - this.bottomMargin;
                this.x = ((cellWidth + widthGap) * myCellX) + hStartPadding + this.leftMargin;
                this.y = ((cellHeight + heightGap) * myCellY) + vStartPadding + this.topMargin;
            }
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return this.width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return this.height;
        }

        public void setX(int x2) {
            this.x = x2;
        }

        public int getX() {
            return this.x;
        }

        public void setY(int y2) {
            this.y = y2;
        }

        public int getY() {
            return this.y;
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ")";
        }
    }

    static final class CellInfo implements ContextMenu.ContextMenuInfo {
        View cell;
        int cellX = -1;
        int cellY = -1;
        int screen;
        int spanX;
        int spanY;
        boolean valid;

        CellInfo() {
        }

        public String toString() {
            return "Cell[view=" + (this.cell == null ? "null" : this.cell.getClass()) + ", x=" + this.cellX + ", y=" + this.cellY + "]";
        }
    }
}
