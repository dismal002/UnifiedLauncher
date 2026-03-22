package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class DragLayer extends FrameLayout {
    private DragController mDragController;
    private int[] mTmpXY = new int[2];

    public DragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMotionEventSplittingEnabled(false);
    }

    public void setDragController(DragController controller) {
        this.mDragController = controller;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return this.mDragController.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Workspace w = (Workspace) findViewById(R.id.workspace);
        if (w != null) {
            final CellLayoutChildren childrenLayout = ((CellLayout) w.getChildAt(w.getCurrentPage())).getChildrenLayout();
            if (childrenLayout.hasResizeFrames() && !childrenLayout.isWidgetBeingResized()) {
                post(new Runnable() {
                    public void run() {
                        if (!childrenLayout.isWidgetBeingResized()) {
                            childrenLayout.clearAllResizeFrames();
                        }
                    }
                });
            }
        }
        return this.mDragController.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mDragController.onTouchEvent(ev);
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        return this.mDragController.dispatchUnhandledMove(focused, direction);
    }

    public View createDragView(Bitmap b, int xPos, int yPos) {
        ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(b);
        imageView.setX((float) xPos);
        imageView.setY((float) yPos);
        addView(imageView, b.getWidth(), b.getHeight());
        return imageView;
    }

    public View createDragView(View v) {
        v.getLocationOnScreen(this.mTmpXY);
        return createDragView(this.mDragController.getViewBitmap(v), this.mTmpXY[0], this.mTmpXY[1]);
    }
}
