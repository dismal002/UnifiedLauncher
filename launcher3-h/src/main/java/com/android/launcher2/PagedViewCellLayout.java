package com.android.launcher2;
import com.launcher3h.R;

import android.content.res.Resources;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class PagedViewCellLayout extends ViewGroup implements Page {
    private static int sDefaultCellDimensions = 96;
    private boolean mAllowHardwareLayerCreation;
    private int mCellCountX;
    private int mCellCountY;
    private int mCellHeight;
    private int mCellWidth;
    protected PagedViewCellLayoutChildren mChildren;
    private boolean mCreateHardwareLayersIfAllowed;
    private int mHeightGap;
    private PagedViewCellLayoutChildren mHolographicChildren;
    private int mWidthGap;

    public PagedViewCellLayout(Context context) {
        this(context, (AttributeSet) null);
    }

    public PagedViewCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAllowHardwareLayerCreation = false;
        this.mCreateHardwareLayersIfAllowed = false;
        setAlwaysDrawnWithCacheEnabled(false);
        Resources resources = context.getResources();
        this.mCellWidth = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        this.mCellHeight = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        this.mCellCountX = LauncherModel.getCellCountX();
        this.mCellCountY = LauncherModel.getCellCountY();
        this.mHeightGap = -1;
        this.mWidthGap = -1;
        this.mChildren = new PagedViewCellLayoutChildren(context);
        this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight);
        this.mChildren.setGap(this.mWidthGap, this.mHeightGap);
        addView(this.mChildren);
        this.mHolographicChildren = new PagedViewCellLayoutChildren(context);
        this.mHolographicChildren.setAlpha(0.0f);
        this.mHolographicChildren.setCellDimensions(this.mCellWidth, this.mCellHeight);
        this.mHolographicChildren.setGap(this.mWidthGap, this.mHeightGap);
        addView(this.mHolographicChildren);
    }

    public int getCellWidth() {
        return this.mCellWidth;
    }

    public int getCellHeight() {
        return this.mCellHeight;
    }

    public void allowHardwareLayerCreation() {
        if (!this.mAllowHardwareLayerCreation) {
            this.mAllowHardwareLayerCreation = true;
            if (this.mCreateHardwareLayersIfAllowed) {
                createHardwareLayers();
            }
        }
    }

    public void setAlpha(float alpha) {
        this.mChildren.setAlpha(alpha);
        this.mHolographicChildren.setAlpha(1.0f - alpha);
    }

    /* access modifiers changed from: package-private */
    public void destroyHardwareLayers() {
        this.mCreateHardwareLayersIfAllowed = false;
        if (this.mAllowHardwareLayerCreation) {
            this.mChildren.destroyHardwareLayer();
            this.mHolographicChildren.destroyHardwareLayer();
        }
    }

    /* access modifiers changed from: package-private */
    public void createHardwareLayers() {
        this.mCreateHardwareLayersIfAllowed = true;
        if (this.mAllowHardwareLayerCreation) {
            this.mChildren.createHardwareLayer();
            this.mHolographicChildren.createHardwareLayer();
        }
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).cancelLongPress();
        }
    }

    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params) {
        LayoutParams lp = params;
        if (lp.cellX < 0 || lp.cellX > this.mCellCountX - 1 || lp.cellY < 0 || lp.cellY > this.mCellCountY - 1) {
            return false;
        }
        if (lp.cellHSpan < 0) {
            lp.cellHSpan = this.mCellCountX;
        }
        if (lp.cellVSpan < 0) {
            lp.cellVSpan = this.mCellCountY;
        }
        child.setId(childId);
        this.mChildren.addView(child, index, lp);
        if (child instanceof PagedViewIcon) {
            PagedViewIcon pagedViewIcon = (PagedViewIcon) child;
            if (this.mAllowHardwareLayerCreation) {
                pagedViewIcon.disableCache();
            }
            this.mHolographicChildren.addView(pagedViewIcon.getHolographicOutlineView(), index, lp);
        }
        return true;
    }

    public void removeAllViewsOnPage() {
        this.mChildren.removeAllViews();
        this.mHolographicChildren.removeAllViews();
        destroyHardwareLayers();
    }

    public void removeViewOnPageAt(int index) {
        this.mChildren.removeViewAt(index);
        this.mHolographicChildren.removeViewAt(index);
    }

    public int getPageChildCount() {
        return this.mChildren.getChildCount();
    }

    public View getChildOnPageAt(int i) {
        return this.mChildren.getChildAt(i);
    }

    public int indexOfChildOnPage(View v) {
        return this.mChildren.indexOfChild(v);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightGap;
        int widthGap;
        int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == 0 || heightSpecMode == 0) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        int cellWidth = this.mCellWidth;
        int cellHeight = this.mCellHeight;
        int minGap = Math.min((((widthSpecSize - getPaddingLeft()) - getPaddingRight()) - (this.mCellCountX * cellWidth)) / (this.mCellCountX - 1), (((heightSpecSize - getPaddingTop()) - getPaddingBottom()) - (this.mCellCountY * cellHeight)) / (this.mCellCountY - 1));
        if (this.mWidthGap <= -1 || this.mHeightGap <= -1) {
            heightGap = minGap;
            widthGap = minGap;
        } else {
            widthGap = this.mWidthGap;
            heightGap = this.mHeightGap;
        }
        int newWidth = (this.mCellCountX * cellWidth) + ((this.mCellCountX - 1) * widthGap);
        int newHeight = (this.mCellCountY * cellHeight) + ((this.mCellCountY - 1) * heightGap);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(View.MeasureSpec.makeMeasureSpec(newWidth, 1073741824), View.MeasureSpec.makeMeasureSpec(newHeight, 1073741824));
        }
        setMeasuredDimension(getPaddingLeft() + newWidth + getPaddingRight(), getPaddingTop() + newHeight + getPaddingBottom());
    }

    /* access modifiers changed from: package-private */
    public int getContentWidth() {
        return getWidthBeforeFirstLayout() - (this.mCellWidth - Utilities.getIconContentSize());
    }

    /* access modifiers changed from: package-private */
    public int getContentHeight() {
        if (this.mCellCountY > 0) {
            return (this.mCellCountY * this.mCellHeight) + ((this.mCellCountY - 1) * Math.max(0, this.mHeightGap));
        }
        return 0;
    }

    /* access modifiers changed from: package-private */
    public int getWidthBeforeFirstLayout() {
        if (this.mCellCountX > 0) {
            return (this.mCellCountX * this.mCellWidth) + ((this.mCellCountX - 1) * Math.max(0, this.mWidthGap));
        }
        return 0;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).layout(getPaddingLeft(), getPaddingTop(), (r - l) - getPaddingRight(), (b - t) - getPaddingBottom());
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!super.onTouchEvent(event)) {
        }
        return true;
    }

    public void enableCenteredContent(boolean enabled) {
        this.mChildren.enableCenteredContent(enabled);
        this.mHolographicChildren.enableCenteredContent(enabled);
    }

    /* access modifiers changed from: protected */
    public void setChildrenDrawingCacheEnabled(boolean enabled) {
        this.mChildren.setChildrenDrawingCacheEnabled(enabled);
        this.mHolographicChildren.setChildrenDrawingCacheEnabled(enabled);
    }

    public void setCellCount(int xCount, int yCount) {
        this.mCellCountX = xCount;
        this.mCellCountY = yCount;
        requestLayout();
    }

    public void setGap(int widthGap, int heightGap) {
        this.mWidthGap = widthGap;
        this.mHeightGap = heightGap;
        this.mChildren.setGap(widthGap, heightGap);
        this.mHolographicChildren.setGap(widthGap, heightGap);
    }

    public int estimateCellHSpan(int width) {
        return (this.mCellWidth + width) / this.mCellWidth;
    }

    public int estimateCellWidth(int hSpan) {
        return this.mCellWidth * hSpan;
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
        public int cellHSpan;
        public int cellVSpan;
        public int cellX;
        public int cellY;
        int x;
        int y;

        public LayoutParams() {
            super(-1, -1);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(int cellX2, int cellY2, int cellHSpan2, int cellVSpan2) {
            super(-1, -1);
            this.cellX = cellX2;
            this.cellY = cellY2;
            this.cellHSpan = cellHSpan2;
            this.cellVSpan = cellVSpan2;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap, int hStartPadding, int vStartPadding) {
            int myCellHSpan = this.cellHSpan;
            int myCellVSpan = this.cellVSpan;
            int myCellX = this.cellX;
            int myCellY = this.cellY;
            this.width = (((myCellHSpan * cellWidth) + ((myCellHSpan - 1) * widthGap)) - this.leftMargin) - this.rightMargin;
            this.height = (((myCellVSpan * cellHeight) + ((myCellVSpan - 1) * heightGap)) - this.topMargin) - this.bottomMargin;
            this.x = ((cellWidth + widthGap) * myCellX) + hStartPadding + this.leftMargin;
            this.y = ((cellHeight + heightGap) * myCellY) + vStartPadding + this.topMargin;
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ", " + this.cellHSpan + ", " + this.cellVSpan + ")";
        }
    }
}
