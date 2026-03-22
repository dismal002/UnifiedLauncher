package com.android.launcher2;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher2.CellLayout;
import java.util.ArrayList;
import java.util.Iterator;

public class CellLayoutChildren extends ViewGroup {
    private int mCellHeight;
    private int mCellWidth;
    private AppWidgetResizeFrame mCurrentResizeFrame;
    private int mHeightGap;
    private int mLeftPadding;
    private final ArrayList<AppWidgetResizeFrame> mResizeFrames = new ArrayList<>();
    private final int[] mTmpCellXY = new int[2];
    private int mTopPadding;
    private final WallpaperManager mWallpaperManager;
    private int mWidthGap;
    private int mXDown;
    private int mYDown;

    public CellLayoutChildren(Context context) {
        super(context);
        this.mWallpaperManager = WallpaperManager.getInstance(context);
        setLayerType(2, (Paint) null);
    }

    public void setCellDimensions(int cellWidth, int cellHeight, int leftPadding, int topPadding, int widthGap, int heightGap) {
        this.mCellWidth = cellWidth;
        this.mCellHeight = cellHeight;
        this.mLeftPadding = leftPadding;
        this.mTopPadding = topPadding;
        this.mWidthGap = widthGap;
        this.mHeightGap = heightGap;
    }

    public View getChildAt(int x, int y) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
            if (lp.cellX <= x && x < lp.cellX + lp.cellHSpan && lp.cellY <= y && y < lp.cellY + lp.cellHSpan) {
                return child;
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int cellWidth = this.mCellWidth;
        int cellHeight = this.mCellHeight;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
            lp.setup(cellWidth, cellHeight, this.mWidthGap, this.mHeightGap, this.mLeftPadding, this.mTopPadding);
            child.measure(View.MeasureSpec.makeMeasureSpec(lp.width, 1073741824), View.MeasureSpec.makeMeasureSpec(lp.height, 1073741824));
        }
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.getSize(heightMeasureSpec));
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                int childLeft = lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, lp.width + childLeft, lp.height + childTop);
                if (lp.dropped) {
                    lp.dropped = false;
                    int[] cellXY = this.mTmpCellXY;
                    getLocationOnScreen(cellXY);
                    this.mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop", cellXY[0] + childLeft + (lp.width / 2), cellXY[1] + childTop + (lp.height / 2), 0, (Bundle) null);
                    if (lp.animateDrop) {
                        lp.animateDrop = false;
                        if (getParent() != null && getParent().getParent() instanceof Workspace) {
                            ((Workspace) getParent().getParent()).animateViewIntoPosition(child);
                        }
                    }
                }
            }
        }
    }

    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).cancelLongPress();
        }
    }

    /* access modifiers changed from: protected */
    public void setChildrenDrawingCacheEnabled(boolean enabled) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            if (!view.isHardwareAccelerated()) {
                view.buildDrawingCache(true);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }

    public void clearAllResizeFrames() {
        Iterator<AppWidgetResizeFrame> it = this.mResizeFrames.iterator();
        while (it.hasNext()) {
            removeView(it.next());
        }
        this.mResizeFrames.clear();
    }

    public boolean hasResizeFrames() {
        return this.mResizeFrames.size() > 0;
    }

    public boolean isWidgetBeingResized() {
        return this.mCurrentResizeFrame != null;
    }

    private boolean handleTouchDown(MotionEvent ev) {
        Rect hitRect = new Rect();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        Iterator<AppWidgetResizeFrame> it = this.mResizeFrames.iterator();
        while (it.hasNext()) {
            AppWidgetResizeFrame child = it.next();
            child.getHitRect(hitRect);
            if (hitRect.contains(x, y) && child.beginResizeIfPointInRegion(x - child.getLeft(), y - child.getTop())) {
                this.mCurrentResizeFrame = child;
                this.mXDown = x;
                this.mYDown = y;
                requestDisallowInterceptTouchEvent(true);
                return true;
            }
        }
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() != 0 || !handleTouchDown(ev)) {
            return false;
        }
        return true;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        int action = ev.getAction();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        if (ev.getAction() == 0 && ev.getAction() == 0 && handleTouchDown(ev)) {
            return true;
        }
        if (this.mCurrentResizeFrame != null) {
            handled = true;
            switch (action) {
                case 1:
                case MotionEvent.ACTION_CANCEL:
                    this.mCurrentResizeFrame.commitResizeForDelta(x - this.mXDown, y - this.mYDown);
                    this.mCurrentResizeFrame = null;
                    break;
                case 2:
                    this.mCurrentResizeFrame.visualizeResizeForDelta(x - this.mXDown, y - this.mYDown);
                    break;
            }
        }
        return handled;
    }

    public void addResizeFrame(ItemInfo itemInfo, LauncherAppWidgetHostView widget, CellLayout cellLayout) {
        AppWidgetResizeFrame resizeFrame = new AppWidgetResizeFrame(getContext(), itemInfo, widget, cellLayout);
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(-1, -1, -1, -1);
        lp.isLockedToGrid = false;
        addView(resizeFrame, lp);
        this.mResizeFrames.add(resizeFrame);
        resizeFrame.snapToWidget(false);
    }
}
