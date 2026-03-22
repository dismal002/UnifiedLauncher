package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.launcher2.CellLayout;

public class AppWidgetResizeFrame extends FrameLayout {
    final int BACKGROUND_PADDING = 24;
    final float DIMMED_HANDLE_ALPHA = 0.0f;
    final float RESIZE_THRESHOLD = 0.66f;
    final int SNAP_DURATION = 150;
    private int mBackgroundPadding;
    private int mBaselineHeight;
    private int mBaselineWidth;
    private int mBaselineX;
    private int mBaselineY;
    private boolean mBottomBorderActive;
    private ImageView mBottomHandle;
    private CellLayout mCellLayout;
    private int mDeltaX;
    private int mDeltaY;
    private int[] mExpandability = new int[4];
    private ItemInfo mItemInfo;
    private boolean mLeftBorderActive;
    private ImageView mLeftHandle;
    private int mMinHSpan;
    private int mMinVSpan;
    private int mResizeMode;
    private boolean mRightBorderActive;
    private ImageView mRightHandle;
    private int mRunningHInc;
    private int mRunningVInc;
    private boolean mTopBorderActive;
    private ImageView mTopHandle;
    private int mTouchTargetWidth;
    private LauncherAppWidgetHostView mWidgetView;
    private final Context mContext;

    public AppWidgetResizeFrame(Context context, ItemInfo itemInfo, LauncherAppWidgetHostView widgetView, CellLayout cellLayout) {
        super(context);
        this.mContext = context;
        this.mItemInfo = itemInfo;
        this.mCellLayout = cellLayout;
        this.mWidgetView = widgetView;
        this.mResizeMode = widgetView.getAppWidgetInfo().resizeMode;
        AppWidgetProviderInfo info = widgetView.getAppWidgetInfo();
        int[] result = this.mCellLayout.rectToCell(info.minWidth, info.minHeight, (int[]) null);
        this.mMinHSpan = result[0];
        this.mMinVSpan = result[1];
        setBackgroundResource(R.drawable.widget_resize_frame_holo);
        setPadding(0, 0, 0, 0);
        this.mLeftHandle = new ImageView(context);
        this.mLeftHandle.setImageResource(R.drawable.widget_resize_handle_left);
        addView(this.mLeftHandle, new FrameLayout.LayoutParams(-2, -2, 19));
        this.mRightHandle = new ImageView(context);
        this.mRightHandle.setImageResource(R.drawable.widget_resize_handle_right);
        addView(this.mRightHandle, new FrameLayout.LayoutParams(-2, -2, 21));
        this.mTopHandle = new ImageView(context);
        this.mTopHandle.setImageResource(R.drawable.widget_resize_handle_top);
        addView(this.mTopHandle, new FrameLayout.LayoutParams(-2, -2, 49));
        this.mBottomHandle = new ImageView(context);
        this.mBottomHandle.setImageResource(R.drawable.widget_resize_handle_bottom);
        addView(this.mBottomHandle, new FrameLayout.LayoutParams(-2, -2, 81));
        if (this.mResizeMode == 1) {
            this.mTopHandle.setVisibility(8);
            this.mBottomHandle.setVisibility(8);
        } else if (this.mResizeMode == 2) {
            this.mLeftHandle.setVisibility(8);
            this.mRightHandle.setVisibility(8);
        }
        this.mBackgroundPadding = (int) Math.ceil((double) (24.0f * this.mContext.getResources().getDisplayMetrics().density));
        this.mTouchTargetWidth = this.mBackgroundPadding * 2;
    }

    public boolean beginResizeIfPointInRegion(int x, int y) {
        boolean horizontalActive;
        boolean verticalActive;
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean anyBordersActive;
        float f;
        float f2;
        float f3;
        float f4;
        if ((this.mResizeMode & 1) != 0) {
            horizontalActive = true;
        } else {
            horizontalActive = false;
        }
        if ((this.mResizeMode & 2) != 0) {
            verticalActive = true;
        } else {
            verticalActive = false;
        }
        if (x >= this.mTouchTargetWidth || !horizontalActive) {
            z = false;
        } else {
            z = true;
        }
        this.mLeftBorderActive = z;
        if (x <= getWidth() - this.mTouchTargetWidth || !horizontalActive) {
            z2 = false;
        } else {
            z2 = true;
        }
        this.mRightBorderActive = z2;
        if (y >= this.mTouchTargetWidth || !verticalActive) {
            z3 = false;
        } else {
            z3 = true;
        }
        this.mTopBorderActive = z3;
        if (y <= getHeight() - this.mTouchTargetWidth || !verticalActive) {
            z4 = false;
        } else {
            z4 = true;
        }
        this.mBottomBorderActive = z4;
        if (this.mLeftBorderActive || this.mRightBorderActive || this.mTopBorderActive || this.mBottomBorderActive) {
            anyBordersActive = true;
        } else {
            anyBordersActive = false;
        }
        this.mBaselineWidth = getMeasuredWidth();
        this.mBaselineHeight = getMeasuredHeight();
        this.mBaselineX = getLeft();
        this.mBaselineY = getTop();
        this.mRunningHInc = 0;
        this.mRunningVInc = 0;
        if (anyBordersActive) {
            ImageView imageView = this.mLeftHandle;
            if (this.mLeftBorderActive) {
                f = 1.0f;
            } else {
                f = 0.0f;
            }
            imageView.setAlpha(f);
            ImageView imageView2 = this.mRightHandle;
            if (this.mRightBorderActive) {
                f2 = 1.0f;
            } else {
                f2 = 0.0f;
            }
            imageView2.setAlpha(f2);
            ImageView imageView3 = this.mTopHandle;
            if (this.mTopBorderActive) {
                f3 = 1.0f;
            } else {
                f3 = 0.0f;
            }
            imageView3.setAlpha(f3);
            ImageView imageView4 = this.mBottomHandle;
            if (this.mBottomBorderActive) {
                f4 = 1.0f;
            } else {
                f4 = 0.0f;
            }
            imageView4.setAlpha(f4);
        }
        this.mCellLayout.getExpandabilityArrayForView(this.mWidgetView, this.mExpandability);
        return anyBordersActive;
    }

    public void updateDeltas(int deltaX, int deltaY) {
        if (this.mLeftBorderActive) {
            this.mDeltaX = Math.max(-this.mBaselineX, deltaX);
            this.mDeltaX = Math.min(this.mBaselineWidth - (this.mTouchTargetWidth * 2), this.mDeltaX);
        } else if (this.mRightBorderActive) {
            this.mDeltaX = Math.min(this.mCellLayout.getWidth() - (this.mBaselineX + this.mBaselineWidth), deltaX);
            this.mDeltaX = Math.max((-this.mBaselineWidth) + (this.mTouchTargetWidth * 2), this.mDeltaX);
        }
        if (this.mTopBorderActive) {
            this.mDeltaY = Math.max(-this.mBaselineY, deltaY);
            this.mDeltaY = Math.min(this.mBaselineHeight - (this.mTouchTargetWidth * 2), this.mDeltaY);
        } else if (this.mBottomBorderActive) {
            this.mDeltaY = Math.min(this.mCellLayout.getHeight() - (this.mBaselineY + this.mBaselineHeight), deltaY);
            this.mDeltaY = Math.max((-this.mBaselineHeight) + (this.mTouchTargetWidth * 2), this.mDeltaY);
        }
    }

    public void visualizeResizeForDelta(int deltaX, int deltaY) {
        updateDeltas(deltaX, deltaY);
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        if (this.mLeftBorderActive) {
            lp.x = this.mBaselineX + this.mDeltaX;
            lp.width = this.mBaselineWidth - this.mDeltaX;
        } else if (this.mRightBorderActive) {
            lp.width = this.mBaselineWidth + this.mDeltaX;
        }
        if (this.mTopBorderActive) {
            lp.y = this.mBaselineY + this.mDeltaY;
            lp.height = this.mBaselineHeight - this.mDeltaY;
        } else if (this.mBottomBorderActive) {
            lp.height = this.mBaselineHeight + this.mDeltaY;
        }
        resizeWidgetIfNeeded();
        requestLayout();
    }

    private void resizeWidgetIfNeeded() {
        int xThreshold = this.mCellLayout.getCellWidth() + this.mCellLayout.getWidthGap();
        int yThreshold = this.mCellLayout.getCellHeight() + this.mCellLayout.getHeightGap();
        float hSpanIncF = ((((float) this.mDeltaX) * 1.0f) / ((float) xThreshold)) - ((float) this.mRunningHInc);
        float vSpanIncF = ((((float) this.mDeltaY) * 1.0f) / ((float) yThreshold)) - ((float) this.mRunningVInc);
        int hSpanInc = 0;
        int vSpanInc = 0;
        int cellXInc = 0;
        int cellYInc = 0;
        if (Math.abs(hSpanIncF) > 0.66f) {
            hSpanInc = Math.round(hSpanIncF);
        }
        if (Math.abs(vSpanIncF) > 0.66f) {
            vSpanInc = Math.round(vSpanIncF);
        }
        if (hSpanInc != 0 || vSpanInc != 0) {
            this.mCellLayout.markCellsAsUnoccupiedForView(this.mWidgetView);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) this.mWidgetView.getLayoutParams();
            if (this.mLeftBorderActive) {
                cellXInc = Math.min(lp.cellHSpan - this.mMinHSpan, Math.max(-this.mExpandability[0], hSpanInc));
                hSpanInc = Math.max(-(lp.cellHSpan - this.mMinHSpan), Math.min(this.mExpandability[0], hSpanInc * -1));
                this.mRunningHInc -= hSpanInc;
            } else if (this.mRightBorderActive) {
                hSpanInc = Math.max(-(lp.cellHSpan - this.mMinHSpan), Math.min(this.mExpandability[2], hSpanInc));
                this.mRunningHInc += hSpanInc;
            }
            if (this.mTopBorderActive) {
                cellYInc = Math.min(lp.cellVSpan - this.mMinVSpan, Math.max(-this.mExpandability[1], vSpanInc));
                vSpanInc = Math.max(-(lp.cellVSpan - this.mMinVSpan), Math.min(this.mExpandability[1], vSpanInc * -1));
                this.mRunningVInc -= vSpanInc;
            } else if (this.mBottomBorderActive) {
                vSpanInc = Math.max(-(lp.cellVSpan - this.mMinVSpan), Math.min(this.mExpandability[3], vSpanInc));
                this.mRunningVInc += vSpanInc;
            }
            if (this.mLeftBorderActive || this.mRightBorderActive) {
                lp.cellHSpan += hSpanInc;
                lp.cellX += cellXInc;
            }
            if (this.mTopBorderActive || this.mBottomBorderActive) {
                lp.cellVSpan += vSpanInc;
                lp.cellY += cellYInc;
            }
            this.mCellLayout.getExpandabilityArrayForView(this.mWidgetView, this.mExpandability);
            this.mCellLayout.markCellsAsOccupiedForView(this.mWidgetView);
        }
    }

    public void commitResizeForDelta(int deltaX, int deltaY) {
        visualizeResizeForDelta(deltaX, deltaY);
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) this.mWidgetView.getLayoutParams();
        LauncherModel.resizeItemInDatabase(getContext(), this.mItemInfo, lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan);
        this.mWidgetView.requestLayout();
        post(new Runnable() {
            public void run() {
                AppWidgetResizeFrame.this.snapToWidget(true);
            }
        });
    }

    public void snapToWidget(boolean animate) {
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        int newWidth = this.mWidgetView.getWidth() + (this.mBackgroundPadding * 2);
        int newHeight = this.mWidgetView.getHeight() + (this.mBackgroundPadding * 2);
        int newX = this.mWidgetView.getLeft() - this.mBackgroundPadding;
        int newY = this.mWidgetView.getTop() - this.mBackgroundPadding;
        if (newY < 0) {
            newHeight -= -newY;
            newY = 0;
        }
        if (newY + newHeight > this.mCellLayout.getHeight()) {
            newHeight -= (newY + newHeight) - this.mCellLayout.getHeight();
        }
        if (!animate) {
            lp.width = newWidth;
            lp.height = newHeight;
            lp.x = newX;
            lp.y = newY;
            this.mLeftHandle.setAlpha(1.0f);
            this.mRightHandle.setAlpha(1.0f);
            this.mTopHandle.setAlpha(1.0f);
            this.mBottomHandle.setAlpha(1.0f);
            requestLayout();
            return;
        }
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(lp, new PropertyValuesHolder[]{PropertyValuesHolder.ofInt("width", new int[]{lp.width, newWidth}), PropertyValuesHolder.ofInt("height", new int[]{lp.height, newHeight}), PropertyValuesHolder.ofInt("x", new int[]{lp.x, newX}), PropertyValuesHolder.ofInt("y", new int[]{lp.y, newY})});
        ObjectAnimator leftOa = ObjectAnimator.ofFloat(this.mLeftHandle, "alpha", new float[]{1.0f});
        ObjectAnimator rightOa = ObjectAnimator.ofFloat(this.mRightHandle, "alpha", new float[]{1.0f});
        ObjectAnimator topOa = ObjectAnimator.ofFloat(this.mTopHandle, "alpha", new float[]{1.0f});
        ObjectAnimator bottomOa = ObjectAnimator.ofFloat(this.mBottomHandle, "alpha", new float[]{1.0f});
        oa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                AppWidgetResizeFrame.this.requestLayout();
            }
        });
        AnimatorSet set = new AnimatorSet();
        if (this.mResizeMode == 2) {
            set.playTogether(new Animator[]{oa, topOa, bottomOa});
        } else if (this.mResizeMode == 1) {
            set.playTogether(new Animator[]{oa, leftOa, rightOa});
        } else {
            set.playTogether(new Animator[]{oa, leftOa, rightOa, topOa, bottomOa});
        }
        set.setDuration(150);
        set.start();
    }
}
