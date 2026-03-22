package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.Iterator;

public class DragController {
    public static int DRAG_ACTION_COPY = 1;
    public static int DRAG_ACTION_MOVE = 0;
    private Context mContext;
    private final int[] mCoordinatesTemp = new int[2];
    private RectF mDeleteRegion;
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    /* access modifiers changed from: private */
    public int mDistanceSinceScroll = 0;
    private Object mDragInfo;
    /* access modifiers changed from: private */
    public DragScroller mDragScroller;
    private DragSource mDragSource;
    private DragView mDragView;
    private boolean mDragging;
    private ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    private Handler mHandler;
    private InputMethodManager mInputMethodManager;
    private DropTarget mLastDropTarget;
    private int[] mLastTouch = new int[2];
    private ArrayList<DragListener> mListeners = new ArrayList<>();
    private float mMotionDownX;
    private float mMotionDownY;
    private View mMoveTarget;
    private View mOriginator;
    private Rect mRectTemp = new Rect();
    private ScrollRunnable mScrollRunnable = new ScrollRunnable();
    /* access modifiers changed from: private */
    public int mScrollState = 0;
    private View mScrollView;
    private int mScrollZone;
    private float mTouchOffsetX;
    private float mTouchOffsetY;
    private final Vibrator mVibrator;
    private IBinder mWindowToken;

    interface DragListener {
        void onDragEnd();

        void onDragStart(DragSource dragSource, Object obj, int i);
    }

    public DragController(Context context) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mScrollZone = context.getResources().getDimensionPixelSize(R.dimen.scroll_zone);
        this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public boolean dragging() {
        return this.mDragging;
    }

    public void startDrag(View v, DragSource source, Object dragInfo, int dragAction) {
        startDrag(v, source, dragInfo, dragAction, (Rect) null);
    }

    public void startDrag(View v, DragSource source, Object dragInfo, int dragAction, Rect dragRegion) {
        this.mOriginator = v;
        Bitmap b = getViewBitmap(v);
        if (b != null) {
            int[] loc = this.mCoordinatesTemp;
            v.getLocationOnScreen(loc);
            startDrag(b, loc[0], loc[1], source, dragInfo, dragAction, dragRegion);
            b.recycle();
            if (dragAction == DRAG_ACTION_MOVE) {
                v.setVisibility(8);
            }
        }
    }

    public void startDrag(View v, Bitmap bmp, DragSource source, Object dragInfo, int dragAction, Rect dragRegion) {
        this.mOriginator = v;
        int[] loc = this.mCoordinatesTemp;
        v.getLocationOnScreen(loc);
        startDrag(bmp, loc[0], loc[1], source, dragInfo, dragAction, dragRegion);
        if (dragAction == DRAG_ACTION_MOVE) {
            v.setVisibility(8);
        }
    }

    public void startDrag(Bitmap b, int screenX, int screenY, DragSource source, Object dragInfo, int dragAction) {
        startDrag(b, screenX, screenY, source, dragInfo, dragAction, (Rect) null);
    }

    public void startDrag(Bitmap b, int screenX, int screenY, DragSource source, Object dragInfo, int dragAction, Rect dragRegion) {
        if (this.mInputMethodManager == null) {
            this.mInputMethodManager = (InputMethodManager) this.mContext.getSystemService("input_method");
        }
        this.mInputMethodManager.hideSoftInputFromWindow(this.mWindowToken, 0);
        Iterator<DragListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDragStart(source, dragInfo, dragAction);
        }
        int registrationX = ((int) this.mMotionDownX) - screenX;
        int registrationY = ((int) this.mMotionDownY) - screenY;
        int dragRegionLeft = dragRegion == null ? 0 : dragRegion.left;
        int dragRegionTop = dragRegion == null ? 0 : dragRegion.top;
        this.mTouchOffsetX = (this.mMotionDownX - ((float) screenX)) - ((float) dragRegionLeft);
        this.mTouchOffsetY = (this.mMotionDownY - ((float) screenY)) - ((float) dragRegionTop);
        this.mDragging = true;
        this.mDragSource = source;
        this.mDragInfo = dragInfo;
        if (this.mVibrator != null) {
            this.mVibrator.vibrate(35);
        }
        DragView dragView = new DragView(this.mContext, b, registrationX, registrationY, 0, 0, b.getWidth(), b.getHeight());
        this.mDragView = dragView;
        final DragSource dragSource = source;
        dragView.setOnDrawRunnable(new Runnable() {
            public void run() {
                dragSource.onDragViewVisible();
            }
        });
        if (dragRegion != null) {
            dragView.setDragRegion(dragRegionLeft, dragRegion.top, dragRegion.right - dragRegionLeft, dragRegion.bottom - dragRegionTop);
        }
        dragView.show(this.mWindowToken, (int) this.mMotionDownX, (int) this.mMotionDownY);
        handleMoveEvent((int) this.mMotionDownX, (int) this.mMotionDownY);
    }

    /* access modifiers changed from: package-private */
    public Bitmap getViewBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);
        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);
        float alpha = v.getAlpha();
        v.setAlpha(1.0f);
        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            Log.e("Launcher.DragController", "failed getViewBitmap(" + v + ")", new RuntimeException());
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);
        v.destroyDrawingCache();
        v.setAlpha(alpha);
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);
        return bitmap;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return this.mDragging;
    }

    public boolean isDragging() {
        return this.mDragging;
    }

    public void cancelDrag() {
        if (this.mDragging) {
            this.mDragSource.onDropCompleted((View) null, this.mDragInfo, false);
        }
        endDrag();
    }

    private void endDrag() {
        if (this.mDragging) {
            this.mDragging = false;
            if (this.mOriginator != null) {
                this.mOriginator.setVisibility(0);
            }
            Iterator<DragListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onDragEnd();
            }
            if (this.mDragView != null) {
                this.mDragView.remove();
                this.mDragView = null;
            }
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == 0) {
            recordScreenSize();
        }
        int screenX = clamp((int) ev.getRawX(), 0, this.mDisplayMetrics.widthPixels);
        int screenY = clamp((int) ev.getRawY(), 0, this.mDisplayMetrics.heightPixels);
        switch (action) {
            case 0:
                this.mMotionDownX = (float) screenX;
                this.mMotionDownY = (float) screenY;
                this.mLastDropTarget = null;
                break;
            case 1:
                if (this.mDragging) {
                    drop((float) screenX, (float) screenY);
                }
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL /*3*/:
                cancelDrag();
                break;
        }
        return this.mDragging;
    }

    /* access modifiers changed from: package-private */
    public void setMoveTarget(View view) {
        this.mMoveTarget = view;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        return this.mMoveTarget != null && this.mMoveTarget.dispatchUnhandledMove(focused, direction);
    }

    private void handleMoveEvent(int x, int y) {
        this.mDragView.move(x, y);
        int[] coordinates = this.mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget(x, y, coordinates);
        if (dropTarget != null) {
            DropTarget delegate = dropTarget.getDropTargetDelegate(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo);
            if (delegate != null) {
                dropTarget = delegate;
            }
            if (this.mLastDropTarget != dropTarget) {
                if (this.mLastDropTarget != null) {
                    this.mLastDropTarget.onDragExit(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo);
                }
                dropTarget.onDragEnter(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo);
            }
            dropTarget.onDragOver(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo);
        } else if (this.mLastDropTarget != null) {
            this.mLastDropTarget.onDragExit(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo);
        }
        this.mLastDropTarget = dropTarget;
        boolean inDeleteRegion = false;
        if (this.mDeleteRegion != null) {
            inDeleteRegion = this.mDeleteRegion.contains((float) x, (float) y);
        }
        int slop = ViewConfiguration.get(this.mContext).getScaledWindowTouchSlop();
        this.mDistanceSinceScroll = (int) (((double) this.mDistanceSinceScroll) + Math.sqrt(Math.pow((double) (this.mLastTouch[0] - x), 2.0d) + Math.pow((double) (this.mLastTouch[1] - y), 2.0d)));
        this.mLastTouch[0] = x;
        this.mLastTouch[1] = y;
        if (inDeleteRegion || x >= this.mScrollZone) {
            if (inDeleteRegion || x <= this.mScrollView.getWidth() - this.mScrollZone) {
                if (this.mScrollState == 1) {
                    this.mScrollState = 0;
                    this.mScrollRunnable.setDirection(1);
                    this.mHandler.removeCallbacks(this.mScrollRunnable);
                    this.mDragScroller.onExitScrollArea();
                }
            } else if (this.mScrollState == 0 && this.mDistanceSinceScroll > slop) {
                this.mScrollState = 1;
                this.mScrollRunnable.setDirection(1);
                this.mHandler.postDelayed(this.mScrollRunnable, 600);
                this.mDragScroller.onEnterScrollArea(1);
            }
        } else if (this.mScrollState == 0 && this.mDistanceSinceScroll > slop) {
            this.mScrollState = 1;
            this.mScrollRunnable.setDirection(0);
            this.mHandler.postDelayed(this.mScrollRunnable, 600);
            this.mDragScroller.onEnterScrollArea(0);
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.mDragging) {
            return false;
        }
        int action = ev.getAction();
        int screenX = clamp((int) ev.getRawX(), 0, this.mDisplayMetrics.widthPixels);
        int screenY = clamp((int) ev.getRawY(), 0, this.mDisplayMetrics.heightPixels);
        switch (action) {
            case 0:
                this.mMotionDownX = (float) screenX;
                this.mMotionDownY = (float) screenY;
                if (screenX >= this.mScrollZone && screenX <= this.mScrollView.getWidth() - this.mScrollZone) {
                    this.mScrollState = 0;
                    break;
                } else {
                    this.mScrollState = 1;
                    this.mHandler.postDelayed(this.mScrollRunnable, 600);
                    break;
                }
            case 1:
                handleMoveEvent(screenX, screenY);
                this.mHandler.removeCallbacks(this.mScrollRunnable);
                if (this.mDragging) {
                    drop((float) screenX, (float) screenY);
                }
                endDrag();
                break;
            case 2:
                handleMoveEvent(screenX, screenY);
                break;
            case MotionEvent.ACTION_CANCEL /*3*/:
                cancelDrag();
                break;
        }
        return true;
    }

    private void drop(float x, float y) {
        int[] coordinates = this.mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget((int) x, (int) y, coordinates);
        boolean accepted = false;
        if (dropTarget != null) {
            dropTarget.onDragExit(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo);
            if (dropTarget.acceptDrop(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo)) {
                dropTarget.onDrop(this.mDragSource, coordinates[0], coordinates[1], (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo);
                accepted = true;
            }
        }
        this.mDragSource.onDropCompleted((View) dropTarget, this.mDragInfo, accepted);
    }

    private DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {
        Rect r = this.mRectTemp;
        ArrayList<DropTarget> dropTargets = this.mDropTargets;
        for (int i = dropTargets.size() - 1; i >= 0; i--) {
            DropTarget target = dropTargets.get(i);
            if (target.isDropEnabled()) {
                target.getHitRect(r);
                target.getLocationOnScreen(dropCoordinates);
                r.offset(dropCoordinates[0] - target.getLeft(), dropCoordinates[1] - target.getTop());
                if (r.contains(x, y)) {
                    DropTarget delegate = target.getDropTargetDelegate(this.mDragSource, x, y, (int) this.mTouchOffsetX, (int) this.mTouchOffsetY, this.mDragView, this.mDragInfo);
                    if (delegate != null) {
                        target = delegate;
                        target.getLocationOnScreen(dropCoordinates);
                    }
                    dropCoordinates[0] = x - dropCoordinates[0];
                    dropCoordinates[1] = y - dropCoordinates[1];
                    return target;
                }
            }
        }
        return null;
    }

    private void recordScreenSize() {
        ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getMetrics(this.mDisplayMetrics);
    }

    private static int clamp(int val, int min, int max) {
        if (val < min) {
            return min;
        }
        return val >= max ? max - 1 : val;
    }

    public void setDragScoller(DragScroller scroller) {
        this.mDragScroller = scroller;
    }

    public void setWindowToken(IBinder token) {
        this.mWindowToken = token;
    }

    public void addDragListener(DragListener l) {
        this.mListeners.add(l);
    }

    public void addDropTarget(DropTarget target) {
        this.mDropTargets.add(target);
    }

    public void removeDropTarget(DropTarget target) {
        this.mDropTargets.remove(target);
    }

    public void setScrollView(View v) {
        this.mScrollView = v;
    }

    /* access modifiers changed from: package-private */
    public void setDeleteRegion(RectF region) {
        this.mDeleteRegion = region;
    }

    /* access modifiers changed from: package-private */
    public DragView getDragView() {
        return this.mDragView;
    }

    private class ScrollRunnable implements Runnable {
        private int mDirection;

        ScrollRunnable() {
        }

        public void run() {
            if (DragController.this.mDragScroller != null) {
                if (this.mDirection == 0) {
                    DragController.this.mDragScroller.scrollLeft();
                } else {
                    DragController.this.mDragScroller.scrollRight();
                }
                int unused = DragController.this.mScrollState = 0;
                int unused2 = DragController.this.mDistanceSinceScroll = 0;
                DragController.this.mDragScroller.onExitScrollArea();
            }
        }

        /* access modifiers changed from: package-private */
        public void setDirection(int direction) {
            this.mDirection = direction;
        }
    }
}
