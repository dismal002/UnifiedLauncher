package com.android.launcher2;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.launcher2.DragController;

public class IconDropTarget extends TextView implements DragController.DragListener, DropTarget {
    protected boolean mActive;
    protected boolean mDragAndDropEnabled;
    protected final int[] mDragPadding;
    protected final Paint mHoverPaint;
    protected Launcher mLauncher;
    protected View[] mOverlappingViews;

    public IconDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mOverlappingViews = null;
        this.mHoverPaint = new Paint();
        this.mDragPadding = new int[4];
        this.mDragAndDropEnabled = true;
    }

    /* access modifiers changed from: protected */
    public void setDragPadding(int t, int r, int b, int l) {
        this.mDragPadding[0] = t;
        this.mDragPadding[1] = r;
        this.mDragPadding[2] = b;
        this.mDragPadding[3] = l;
    }

    /* access modifiers changed from: package-private */
    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    /* access modifiers changed from: package-private */
    public void setOverlappingView(View view) {
        this.mOverlappingViews = new View[]{view};
    }

    /* access modifiers changed from: package-private */
    public void setOverlappingViews(View[] views) {
        this.mOverlappingViews = views;
    }

    /* access modifiers changed from: package-private */
    public void setDragAndDropEnabled(boolean enabled) {
        this.mDragAndDropEnabled = enabled;
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        return false;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (this.mDragAndDropEnabled) {
            dragView.setPaint(this.mHoverPaint);
        }
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (this.mDragAndDropEnabled) {
            dragView.setPaint((Paint) null);
        }
    }

    public void onDragStart(DragSource source, Object info, int dragAction) {
    }

    public boolean isDropEnabled() {
        return this.mDragAndDropEnabled && this.mActive;
    }

    public void onDragEnd() {
    }

    public void getHitRect(Rect outRect) {
        super.getHitRect(outRect);
        if (LauncherApplication.isScreenXLarge()) {
            outRect.top -= this.mDragPadding[0];
            outRect.right += this.mDragPadding[1];
            outRect.bottom += this.mDragPadding[2];
            outRect.left -= this.mDragPadding[3];
        }
    }

    public DropTarget getDropTargetDelegate(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        return null;
    }
}
