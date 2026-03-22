package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher2.CellLayout;
import com.android.launcher2.InstallWidgetReceiver;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Workspace extends SmoothPagedView implements View.OnClickListener, View.OnTouchListener, DragScroller, DragSource, DropTarget {
    /* access modifiers changed from: private */
    public AnimatorSet mAnimator;
    private Drawable mBackground;
    private float mBackgroundAlpha;
    private ValueAnimator mBackgroundFadeInAnimation;
    private ValueAnimator mBackgroundFadeOutAnimation;
    private final Camera mCamera;
    private float mChildrenOutlineAlpha;
    private ObjectAnimator mChildrenOutlineFadeInAnimation;
    private ObjectAnimator mChildrenOutlineFadeOutAnimation;
    private View mCustomizationDrawer;
    private View mCustomizationDrawerContent;
    private int[] mCustomizationDrawerPos;
    private float[] mCustomizationDrawerTransformedPos;
    private Drawable mCustomizeTrayBackground;
    private int mDefaultPage;
    /* access modifiers changed from: private */
    public DragController mDragController;
    private CellLayout.CellInfo mDragInfo;
    private Bitmap mDragOutline;
    private CellLayout mDragTargetLayout;
    boolean mDrawBackground;
    /* access modifiers changed from: private */
    public boolean mDrawCustomizeTrayBackground;
    private ValueAnimator mDropAnim;
    /* access modifiers changed from: private */
    public View mDropView;
    /* access modifiers changed from: private */
    public int[] mDropViewPos;
    private final Paint mExternalDragOutlinePaint;
    private IconCache mIconCache;
    private boolean mInScrollArea;
    private boolean mIsDragInProcess;
    /* access modifiers changed from: private */
    public boolean mIsInUnshrinkAnimation;
    private boolean mIsSmall;
    /* access modifiers changed from: private */
    public int mLastDragOriginX;
    /* access modifiers changed from: private */
    public int mLastDragOriginY;
    /* access modifiers changed from: private */
    public DragView mLastDragView;
    /* access modifiers changed from: private */
    public int mLastDragXOffset;
    /* access modifiers changed from: private */
    public int mLastDragYOffset;
    /* access modifiers changed from: private */
    public Launcher mLauncher;
    private final Matrix mMatrix;
    private Drawable mNextIndicator;
    private final HolographicOutlineHelper mOutlineHelper;
    private float mOverScrollMaxBackgroundAlpha;
    private int mOverScrollPageIndex;
    private int mPendingScrollDirection;
    private Drawable mPreviousIndicator;
    private TimeInterpolator mQuintEaseOutInterpolator;
    private Animator.AnimatorListener mShrinkAnimationListener;
    /* access modifiers changed from: private */
    public ShrinkState mShrinkState;
    /* access modifiers changed from: private */
    public SpringLoadedDragController mSpringLoadedDragController;
    boolean mSyncWallpaperOffsetWithScroll;
    private int[] mTargetCell;
    private int[] mTempCell;
    private float[] mTempCellLayoutCenterCoordinates;
    private float[] mTempDragBottomRightCoordinates;
    private float[] mTempDragCoordinates;
    private int[] mTempEstimate;
    private final float[] mTempFloat2;
    private Matrix mTempInverseMatrix;
    private float[] mTempOriginXY;
    private final Rect mTempRect;
    private float[] mTempTouchCoordinates;
    private final int[] mTempXY;
    private Animator.AnimatorListener mUnshrinkAnimationListener;
    boolean mUpdateWallpaperOffsetImmediately;
    /* access modifiers changed from: private */
    public boolean mWaitingToShrink;
    /* access modifiers changed from: private */
    public ShrinkState mWaitingToShrinkState;
    int mWallpaperHeight;
    /* access modifiers changed from: private */
    public final WallpaperManager mWallpaperManager;
    WallpaperOffsetInterpolator mWallpaperOffset;
    int mWallpaperWidth;
    private boolean mWasSpringLoadedOnDragExit;
    private IBinder mWindowToken;
    private float mXDown;
    private float mYDown;
    private final ZoomInInterpolator mZoomInInterpolator;
    private final ZoomOutInterpolator mZoomOutInterpolator;

    enum ShrinkState {
        TOP,
        SPRING_LOADED,
        MIDDLE,
        BOTTOM_HIDDEN,
        BOTTOM_VISIBLE
    }

    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mChildrenOutlineAlpha = 0.0f;
        this.mDrawBackground = true;
        this.mBackgroundAlpha = 0.0f;
        this.mOverScrollMaxBackgroundAlpha = 0.0f;
        this.mOverScrollPageIndex = -1;
        this.mCustomizationDrawerPos = new int[2];
        this.mCustomizationDrawerTransformedPos = new float[2];
        this.mIsDragInProcess = false;
        this.mTargetCell = null;
        this.mDragTargetLayout = null;
        this.mTempCell = new int[2];
        this.mTempEstimate = new int[2];
        this.mTempOriginXY = new float[2];
        this.mTempDragCoordinates = new float[2];
        this.mTempTouchCoordinates = new float[2];
        this.mTempCellLayoutCenterCoordinates = new float[2];
        this.mTempDragBottomRightCoordinates = new float[2];
        this.mTempInverseMatrix = new Matrix();
        this.mIsSmall = false;
        this.mIsInUnshrinkAnimation = false;
        this.mWasSpringLoadedOnDragExit = false;
        this.mWaitingToShrink = false;
        this.mInScrollArea = false;
        this.mPendingScrollDirection = -1;
        this.mOutlineHelper = new HolographicOutlineHelper();
        this.mDragOutline = null;
        this.mTempRect = new Rect();
        this.mTempXY = new int[2];
        this.mDropAnim = null;
        this.mQuintEaseOutInterpolator = new DecelerateInterpolator(2.5f);
        this.mDropView = null;
        this.mDropViewPos = new int[]{-1, -1};
        this.mExternalDragOutlinePaint = new Paint();
        this.mMatrix = new Matrix();
        this.mCamera = new Camera();
        this.mTempFloat2 = new float[2];
        this.mUpdateWallpaperOffsetImmediately = false;
        this.mSyncWallpaperOffsetWithScroll = true;
        this.mZoomOutInterpolator = new ZoomOutInterpolator();
        this.mZoomInInterpolator = new ZoomInInterpolator();
        this.mContentIsRefreshable = false;
        if (!LauncherApplication.isScreenXLarge()) {
            this.mFadeInAdjacentScreens = false;
        }
        this.mWallpaperManager = WallpaperManager.getInstance(context);
        int cellCountX = 4;
        int cellCountY = 4;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Workspace, defStyle, 0);
        if (LauncherApplication.isScreenXLarge()) {
            Resources res = context.getResources();
            float density = res.getDisplayMetrics().density;
            float actionBarHeight = context.obtainStyledAttributes(new int[]{16843499}).getDimension(0, 0.0f) / density;
            float systemBarHeight = res.getDimension(R.dimen.status_bar_height) / density;
            float smallestScreenDim = (float) res.getConfiguration().smallestScreenWidthDp;
            cellCountX = 1;
            while (((float) CellLayout.widthInPortrait(res, cellCountX + 1)) / density <= smallestScreenDim) {
                cellCountX++;
            }
            cellCountY = 1;
            while (((float) CellLayout.heightInLandscape(res, cellCountY + 1)) / density + actionBarHeight <= smallestScreenDim - systemBarHeight) {
                cellCountY++;
            }
        }
        int cellCountX2 = a.getInt(R.styleable.Workspace_cellCountX, cellCountX);
        int cellCountY2 = a.getInt(R.styleable.Workspace_cellCountY, cellCountY);
        this.mDefaultPage = a.getInt(R.styleable.Workspace_defaultScreen, 1);
        a.recycle();
        LauncherModel.updateWorkspaceLayoutCells(cellCountX2, cellCountY2);
        setHapticFeedbackEnabled(false);
        initWorkspace();
        setMotionEventSplittingEnabled(true);
    }

    /* access modifiers changed from: protected */
    public void initWorkspace() {
        Context context = getContext();
        this.mCurrentPage = this.mDefaultPage;
        Launcher.setScreen(this.mCurrentPage);
        this.mIconCache = LauncherApplication.getSharedInstance(context).getIconCache();
        this.mExternalDragOutlinePaint.setAntiAlias(true);
        setWillNotDraw(false);
        try {
            Resources res = getResources();
            this.mBackground = res.getDrawable(R.drawable.all_apps_bg_gradient);
            this.mCustomizeTrayBackground = res.getDrawable(R.drawable.customize_bg_gradient);
        } catch (Resources.NotFoundException e) {
        }
        this.mUnshrinkAnimationListener = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                boolean unused = Workspace.this.mIsInUnshrinkAnimation = true;
            }

            public void onAnimationEnd(Animator animation) {
                boolean z;
                boolean unused = Workspace.this.mIsInUnshrinkAnimation = false;
                Workspace.this.mSyncWallpaperOffsetWithScroll = true;
                if (Workspace.this.mShrinkState == ShrinkState.SPRING_LOADED) {
                    View layout = null;
                    if (Workspace.this.mLastDragView != null) {
                        layout = Workspace.this.findMatchingPageForDragOver(Workspace.this.mLastDragView, Workspace.this.mLastDragOriginX, Workspace.this.mLastDragOriginY, Workspace.this.mLastDragXOffset, Workspace.this.mLastDragYOffset);
                    }
                    SpringLoadedDragController access$800 = Workspace.this.mSpringLoadedDragController;
                    if (layout == null) {
                        z = true;
                    } else {
                        z = false;
                    }
                    access$800.onEnterSpringLoadedMode(z);
                } else {
                    boolean unused2 = Workspace.this.mDrawCustomizeTrayBackground = false;
                }
                Workspace.this.mWallpaperOffset.setOverrideHorizontalCatchupConstant(false);
                AnimatorSet unused3 = Workspace.this.mAnimator = null;
                Workspace.this.enableChildrenLayers(false);
            }
        };
        this.mShrinkAnimationListener = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                Workspace.this.enableChildrenLayers(true);
            }

            public void onAnimationEnd(Animator animation) {
                Workspace.this.mWallpaperOffset.setOverrideHorizontalCatchupConstant(false);
                AnimatorSet unused = Workspace.this.mAnimator = null;
            }
        };
        this.mSnapVelocity = 600;
        this.mWallpaperOffset = new WallpaperOffsetInterpolator();
    }

    /* access modifiers changed from: protected */
    public int getScrollMode() {
        if (LauncherApplication.isScreenXLarge()) {
            return 1;
        }
        return 0;
    }

    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        ((CellLayout) child).setOnInterceptTouchListener(this);
        child.setOnClickListener(this);
        child.setClickable(true);
        super.addView(child, index, params);
    }

    public void addView(View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        ((CellLayout) child).setOnInterceptTouchListener(this);
        child.setOnClickListener(this);
        child.setClickable(true);
        super.addView(child);
    }

    public void addView(View child, int index) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        ((CellLayout) child).setOnInterceptTouchListener(this);
        child.setOnClickListener(this);
        child.setClickable(true);
        super.addView(child, index);
    }

    public void addView(View child, int width, int height) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        ((CellLayout) child).setOnInterceptTouchListener(this);
        child.setOnClickListener(this);
        child.setClickable(true);
        super.addView(child, width, height);
    }

    public void addView(View child, ViewGroup.LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        ((CellLayout) child).setOnInterceptTouchListener(this);
        child.setOnClickListener(this);
        child.setClickable(true);
        super.addView(child, params);
    }

    /* access modifiers changed from: package-private */
    public Folder getOpenFolder() {
        ViewGroup currentPage = ((CellLayout) getChildAt(this.mCurrentPage)).getChildrenLayout();
        int count = currentPage.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = currentPage.getChildAt(i);
            if (child instanceof Folder) {
                Folder folder = (Folder) child;
                if (folder.getInfo().opened) {
                    return folder;
                }
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public ArrayList<Folder> getOpenFolders() {
        int screenCount = getChildCount();
        ArrayList<Folder> folders = new ArrayList<>(screenCount);
        for (int screen = 0; screen < screenCount; screen++) {
            ViewGroup currentPage = ((CellLayout) getChildAt(screen)).getChildrenLayout();
            int count = currentPage.getChildCount();
            int i = 0;
            while (true) {
                if (i >= count) {
                    break;
                }
                View child = currentPage.getChildAt(i);
                if (child instanceof Folder) {
                    Folder folder = (Folder) child;
                    if (folder.getInfo().opened) {
                        folders.add(folder);
                    }
                } else {
                    i++;
                }
            }
        }
        return folders;
    }

    /* access modifiers changed from: package-private */
    public boolean isTouchActive() {
        return this.mTouchState != 0;
    }

    /* access modifiers changed from: package-private */
    public void addInScreen(View child, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, screen, x, y, spanX, spanY, false);
    }

    /* access modifiers changed from: package-private */
    public void addInFullScreen(View child, int screen) {
        addInScreen(child, screen, 0, 0, -1, -1);
    }

    /* access modifiers changed from: package-private */
    public void addInScreen(View child, int screen, int x, int y, int spanX, int spanY, boolean insert) {
        if (screen < 0 || screen >= getChildCount()) {
            Log.e("Launcher.Workspace", "The screen must be >= 0 and < " + getChildCount() + " (was " + screen + "); skipping child");
            return;
        }
        CellLayout group = (CellLayout) getChildAt(screen);
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        if (lp == null) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }
        if (!group.addViewToCellLayout(child, insert ? 0 : -1, LauncherModel.getCellLayoutChildId(-1, screen, x, y, spanX, spanY), lp, !(child instanceof Folder))) {
            Log.w("Launcher.Workspace", "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout");
        }
        if (!(child instanceof Folder)) {
            child.setHapticFeedbackEnabled(false);
            child.setOnLongClickListener(this.mLongClickListener);
        }
        if (child instanceof DropTarget) {
            this.mDragController.addDropTarget((DropTarget) child);
        }
    }

    private boolean hitsPage(int index, float x, float y) {
        View page = getChildAt(index);
        if (page == null) {
            return false;
        }
        float[] localXY = {x, y};
        mapPointFromSelfToChild(page, localXY);
        if (localXY[0] < 0.0f || localXY[0] >= ((float) page.getWidth()) || localXY[1] < 0.0f || localXY[1] >= ((float) page.getHeight())) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean hitsPreviousPage(float x, float y) {
        return hitsPage((this.mNextPage == -1 ? this.mCurrentPage : this.mNextPage) - 1, x, y);
    }

    /* access modifiers changed from: protected */
    public boolean hitsNextPage(float x, float y) {
        return hitsPage((this.mNextPage == -1 ? this.mCurrentPage : this.mNextPage) + 1, x, y);
    }

    public boolean onTouch(View v, MotionEvent event) {
        return this.mIsSmall || this.mIsInUnshrinkAnimation;
    }

    public void onClick(View cellLayout) {
        if ((this.mIsSmall || this.mIsInUnshrinkAnimation) && this.mShrinkState != ShrinkState.BOTTOM_HIDDEN) {
            this.mLauncher.onWorkspaceClick((CellLayout) cellLayout);
        }
    }

    /* access modifiers changed from: protected */
    public void onWindowVisibilityChanged(int visibility) {
        this.mLauncher.onWindowVisibilityChanged(visibility);
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (this.mIsSmall || this.mIsInUnshrinkAnimation) {
            return false;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mXDown = ev.getX();
            this.mYDown = ev.getY();
        }
        if (!this.mIsSmall && !this.mIsInUnshrinkAnimation) {
            return super.onInterceptTouchEvent(ev);
        }
        if (!this.mLauncher.isAllAppsVisible() || this.mShrinkState != ShrinkState.BOTTOM_HIDDEN) {
            return false;
        }
        AllAppsPagedView allApps = (AllAppsPagedView) this.mLauncher.findViewById(R.id.all_apps_paged_view);
        if (allApps != null) {
            allApps.onInterceptTouchEvent(ev);
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void determineScrollingStart(MotionEvent ev) {
        if (!this.mIsSmall && !this.mIsInUnshrinkAnimation) {
            float deltaX = Math.abs(ev.getX() - this.mXDown);
            float deltaY = Math.abs(ev.getY() - this.mYDown);
            if (Float.compare(deltaX, 0.0f) != 0) {
                float theta = (float) Math.atan((double) (deltaY / deltaX));
                if (deltaX > ((float) this.mTouchSlop) || deltaY > ((float) this.mTouchSlop)) {
                    cancelCurrentPageLongPress();
                }
                if (theta > 1.0471976f) {
                    return;
                }
                if (theta > 0.5235988f) {
                    super.determineScrollingStart(ev, 1.0f + (4.0f * ((float) Math.sqrt((double) ((theta - 0.5235988f) / 0.5235988f)))));
                } else {
                    super.determineScrollingStart(ev);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onPageBeginMoving() {
        if (this.mNextPage != -1) {
            enableChildrenCache(this.mCurrentPage, this.mNextPage);
        } else {
            enableChildrenCache(this.mCurrentPage - 1, this.mCurrentPage + 1);
        }
        showOutlines();
    }

    /* access modifiers changed from: protected */
    public void onPageEndMoving() {
        clearChildrenCache();
        if (!this.mDragController.dragging()) {
            hideOutlines();
        }
        this.mOverScrollMaxBackgroundAlpha = 0.0f;
        this.mOverScrollPageIndex = -1;
    }

    /* access modifiers changed from: protected */
    public void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        if (this.mPreviousIndicator != null) {
            int page = this.mNextPage;
            if (page == -1) {
                page = this.mCurrentPage;
            }
            this.mPreviousIndicator.setLevel(page);
            this.mNextIndicator.setLevel(page);
        }
        Launcher.setScreen(this.mCurrentPage);
    }

    private float wallpaperTravelToScreenHeightRatio(int width, int height) {
        return 1.1f;
    }

    private float wallpaperTravelToScreenWidthRatio(int width, int height) {
        return (0.30769226f * (((float) width) / ((float) height))) + 1.0076923f;
    }

    private int getScrollRange() {
        return getChildOffset(getChildCount() - 1) - getChildOffset(0);
    }

    /* access modifiers changed from: protected */
    public void setWallpaperDimension() {
        Display display = this.mLauncher.getWindowManager().getDefaultDisplay();
        int height = display.getHeight() + ((int) getResources().getDimension(R.dimen.status_bar_height));
        int maxDim = Math.max(display.getWidth(), height);
        int minDim = Math.min(display.getWidth(), height);
        this.mWallpaperWidth = (int) (((float) maxDim) * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
        this.mWallpaperHeight = (int) (((float) maxDim) * wallpaperTravelToScreenHeightRatio(maxDim, minDim));
        new Thread("setWallpaperDimension") {
            public void run() {
                Workspace.this.mWallpaperManager.suggestDesiredDimensions(Workspace.this.mWallpaperWidth, Workspace.this.mWallpaperHeight);
            }
        }.start();
    }

    public void setVerticalWallpaperOffset(float offset) {
        this.mWallpaperOffset.setFinalY(offset);
    }

    public float getVerticalWallpaperOffset() {
        return this.mWallpaperOffset.getCurrY();
    }

    public void setHorizontalWallpaperOffset(float offset) {
        this.mWallpaperOffset.setFinalX(offset);
    }

    public float getHorizontalWallpaperOffset() {
        return this.mWallpaperOffset.getCurrX();
    }

    private float wallpaperOffsetForCurrentScroll() {
        Display display = this.mLauncher.getWindowManager().getDefaultDisplay();
        boolean isStaticWallpaper = this.mWallpaperManager.getWallpaperInfo() == null;
        int wallpaperTravelWidth = (int) (((float) display.getWidth()) * wallpaperTravelToScreenWidthRatio(display.getWidth(), display.getHeight()));
        if (!isStaticWallpaper) {
            wallpaperTravelWidth = this.mWallpaperWidth;
        }
        this.mWallpaperManager.setWallpaperOffsetSteps(1.0f / ((float) (getChildCount() - 1)), 0.5f);
        int scrollRange = getScrollRange();
        float scrollProgressOffset = 0.0f;
        if (isStaticWallpaper) {
            int overscrollOffset = (int) (maxOverScroll() * ((float) display.getWidth()));
            scrollProgressOffset = 0.0f + (((float) overscrollOffset) / ((float) getScrollRange()));
            scrollRange += overscrollOffset * 2;
        }
        return ((((float) wallpaperTravelWidth) * ((((float) getScrollX()) / ((float) scrollRange)) + scrollProgressOffset)) + ((float) ((this.mWallpaperWidth - wallpaperTravelWidth) / 2))) / ((float) this.mWallpaperWidth);
    }

    private void syncWallpaperOffsetWithScroll() {
        if (LauncherApplication.isScreenXLarge()) {
            this.mWallpaperOffset.setFinalX(wallpaperOffsetForCurrentScroll());
        }
    }

    public void updateWallpaperOffsetImmediately() {
        this.mUpdateWallpaperOffsetImmediately = true;
    }

    private void updateWallpaperOffsets() {
        boolean keepUpdating;
        boolean updateNow;
        if (this.mUpdateWallpaperOffsetImmediately) {
            updateNow = true;
            keepUpdating = false;
            this.mWallpaperOffset.jumpToFinal();
            this.mUpdateWallpaperOffsetImmediately = false;
        } else {
            keepUpdating = this.mWallpaperOffset.computeScrollOffset();
            updateNow = keepUpdating;
        }
        android.os.IBinder windowToken = getWindowToken();
        if (updateNow && windowToken != null) {
            this.mWallpaperManager.setWallpaperOffsets(windowToken, this.mWallpaperOffset.getCurrX(), this.mWallpaperOffset.getCurrY());
        }
        if (keepUpdating) {
            invalidate();
        }
    }

    class WallpaperOffsetInterpolator {
        float mFinalHorizontalWallpaperOffset = 0.0f;
        float mFinalVerticalWallpaperOffset = 0.5f;
        float mHorizontalCatchupConstant = 0.35f;
        float mHorizontalWallpaperOffset = 0.0f;
        boolean mIsMovingFast;
        long mLastWallpaperOffsetUpdateTime;
        boolean mOverrideHorizontalCatchupConstant;
        float mVerticalCatchupConstant = 0.35f;
        float mVerticalWallpaperOffset = 0.5f;

        public WallpaperOffsetInterpolator() {
        }

        public void setOverrideHorizontalCatchupConstant(boolean override) {
            this.mOverrideHorizontalCatchupConstant = override;
        }

        public void setHorizontalCatchupConstant(float f) {
            this.mHorizontalCatchupConstant = f;
        }

        public void setVerticalCatchupConstant(float f) {
            this.mVerticalCatchupConstant = f;
        }

        public boolean computeScrollOffset() {
            boolean isLandscape;
            float fractionToCatchUpIn1MsHorizontal;
            if (Float.compare(this.mHorizontalWallpaperOffset, this.mFinalHorizontalWallpaperOffset) == 0 && Float.compare(this.mVerticalWallpaperOffset, this.mFinalVerticalWallpaperOffset) == 0) {
                this.mIsMovingFast = false;
                return false;
            }
            Display display = Workspace.this.mLauncher.getWindowManager().getDefaultDisplay();
            if (display.getWidth() > display.getHeight()) {
                isLandscape = true;
            } else {
                isLandscape = false;
            }
            long timeSinceLastUpdate = Math.max(1, Math.min(33, System.currentTimeMillis() - this.mLastWallpaperOffsetUpdateTime));
            float xdiff = Math.abs(this.mFinalHorizontalWallpaperOffset - this.mHorizontalWallpaperOffset);
            if (!this.mIsMovingFast && ((double) xdiff) > 0.07d) {
                this.mIsMovingFast = true;
            }
            if (this.mOverrideHorizontalCatchupConstant) {
                fractionToCatchUpIn1MsHorizontal = this.mHorizontalCatchupConstant;
            } else if (this.mIsMovingFast) {
                fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.5f : 0.75f;
            } else if (isLandscape) {
                fractionToCatchUpIn1MsHorizontal = 0.27f;
            } else {
                fractionToCatchUpIn1MsHorizontal = 0.5f;
            }
            float fractionToCatchUpIn1MsHorizontal2 = fractionToCatchUpIn1MsHorizontal / 33.0f;
            float fractionToCatchUpIn1MsVertical = this.mVerticalCatchupConstant / 33.0f;
            float hOffsetDelta = this.mFinalHorizontalWallpaperOffset - this.mHorizontalWallpaperOffset;
            float vOffsetDelta = this.mFinalVerticalWallpaperOffset - this.mVerticalWallpaperOffset;
            if (Math.abs(hOffsetDelta) < 1.0E-5f && Math.abs(vOffsetDelta) < 1.0E-5f) {
                this.mHorizontalWallpaperOffset = this.mFinalHorizontalWallpaperOffset;
                this.mVerticalWallpaperOffset = this.mFinalVerticalWallpaperOffset;
            } else {
                float percentToCatchUpVertical = Math.min(1.0f, ((float) timeSinceLastUpdate) * fractionToCatchUpIn1MsVertical);
                this.mHorizontalWallpaperOffset += Math.min(1.0f, ((float) timeSinceLastUpdate) * fractionToCatchUpIn1MsHorizontal2) * hOffsetDelta;
                this.mVerticalWallpaperOffset += percentToCatchUpVertical * vOffsetDelta;
            }
            this.mLastWallpaperOffsetUpdateTime = System.currentTimeMillis();
            return true;
        }

        public float getCurrX() {
            return this.mHorizontalWallpaperOffset;
        }

        public float getCurrY() {
            return this.mVerticalWallpaperOffset;
        }

        public void setFinalX(float x) {
            this.mFinalHorizontalWallpaperOffset = Math.max(0.0f, Math.min(x, 1.0f));
        }

        public void setFinalY(float y) {
            this.mFinalVerticalWallpaperOffset = Math.max(0.0f, Math.min(y, 1.0f));
        }

        public void jumpToFinal() {
            this.mHorizontalWallpaperOffset = this.mFinalHorizontalWallpaperOffset;
            this.mVerticalWallpaperOffset = this.mFinalVerticalWallpaperOffset;
        }
    }

    public void computeScroll() {
        super.computeScroll();
        if (this.mSyncWallpaperOffsetWithScroll) {
            syncWallpaperOffsetWithScroll();
        }
    }

    /* access modifiers changed from: package-private */
    public void showOutlines() {
        if (!this.mIsSmall && !this.mIsInUnshrinkAnimation) {
            if (this.mChildrenOutlineFadeOutAnimation != null) {
                this.mChildrenOutlineFadeOutAnimation.cancel();
            }
            if (this.mChildrenOutlineFadeInAnimation != null) {
                this.mChildrenOutlineFadeInAnimation.cancel();
            }
            this.mChildrenOutlineFadeInAnimation = ObjectAnimator.ofFloat(this, "childrenOutlineAlpha", new float[]{1.0f});
            this.mChildrenOutlineFadeInAnimation.setDuration(100);
            this.mChildrenOutlineFadeInAnimation.start();
        }
    }

    /* access modifiers changed from: package-private */
    public void hideOutlines() {
        if (!this.mIsSmall && !this.mIsInUnshrinkAnimation) {
            if (this.mChildrenOutlineFadeInAnimation != null) {
                this.mChildrenOutlineFadeInAnimation.cancel();
            }
            if (this.mChildrenOutlineFadeOutAnimation != null) {
                this.mChildrenOutlineFadeOutAnimation.cancel();
            }
            this.mChildrenOutlineFadeOutAnimation = ObjectAnimator.ofFloat(this, "childrenOutlineAlpha", new float[]{0.0f});
            this.mChildrenOutlineFadeOutAnimation.setDuration(375);
            this.mChildrenOutlineFadeOutAnimation.setStartDelay(0);
            this.mChildrenOutlineFadeOutAnimation.start();
        }
    }

    public void showOutlinesTemporarily() {
        if (!this.mIsPageMoving && !isTouchActive()) {
            snapToPage(this.mCurrentPage);
        }
    }

    public void setChildrenOutlineAlpha(float alpha) {
        this.mChildrenOutlineAlpha = alpha;
        for (int i = 0; i < getChildCount(); i++) {
            ((CellLayout) getChildAt(i)).setBackgroundAlpha(alpha);
        }
    }

    public float getChildrenOutlineAlpha() {
        return this.mChildrenOutlineAlpha;
    }

    /* access modifiers changed from: package-private */
    public void disableBackground() {
        this.mDrawBackground = false;
    }

    /* access modifiers changed from: package-private */
    public void enableBackground() {
        this.mDrawBackground = true;
    }

    private void showBackgroundGradientForAllApps() {
        showBackgroundGradient();
        this.mDrawCustomizeTrayBackground = false;
    }

    private void showBackgroundGradientForCustomizeTray() {
        showBackgroundGradient();
        this.mDrawCustomizeTrayBackground = true;
    }

    private void showBackgroundGradient() {
        if (this.mBackground != null) {
            if (this.mBackgroundFadeOutAnimation != null) {
                this.mBackgroundFadeOutAnimation.cancel();
            }
            if (this.mBackgroundFadeInAnimation != null) {
                this.mBackgroundFadeInAnimation.cancel();
            }
            this.mBackgroundFadeInAnimation = ValueAnimator.ofFloat(new float[]{getBackgroundAlpha(), 1.0f});
            this.mBackgroundFadeInAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Workspace.this.setBackgroundAlpha(((Float) animation.getAnimatedValue()).floatValue());
                }
            });
            this.mBackgroundFadeInAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
            this.mBackgroundFadeInAnimation.setDuration(350);
            this.mBackgroundFadeInAnimation.start();
        }
    }

    private void hideBackgroundGradient() {
        if (this.mBackground != null) {
            if (this.mBackgroundFadeInAnimation != null) {
                this.mBackgroundFadeInAnimation.cancel();
            }
            if (this.mBackgroundFadeOutAnimation != null) {
                this.mBackgroundFadeOutAnimation.cancel();
            }
            this.mBackgroundFadeOutAnimation = ValueAnimator.ofFloat(new float[]{getBackgroundAlpha(), 0.0f});
            this.mBackgroundFadeOutAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Workspace.this.setBackgroundAlpha(((Float) animation.getAnimatedValue()).floatValue());
                }
            });
            this.mBackgroundFadeOutAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
            this.mBackgroundFadeOutAnimation.setDuration(350);
            this.mBackgroundFadeOutAnimation.start();
        }
    }

    public void setBackgroundAlpha(float alpha) {
        if (alpha != this.mBackgroundAlpha) {
            this.mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    public float getBackgroundAlpha() {
        return this.mBackgroundAlpha;
    }

    private float getOffsetXForRotation(float f, int i, int i2) {
        this.mMatrix.reset();
        this.mCamera.save();
        this.mCamera.rotateY(Math.abs(f));
        this.mCamera.getMatrix(this.mMatrix);
        this.mCamera.restore();
        this.mMatrix.preTranslate(((float) (-i)) * 0.5f, ((float) (-i2)) * 0.5f);
        this.mMatrix.postTranslate(((float) i) * 0.5f, ((float) i2) * 0.5f);
        this.mTempFloat2[0] = (float) i;
        this.mTempFloat2[1] = (float) i2;
        this.mMatrix.mapPoints(this.mTempFloat2);
        return (((float) i) - this.mTempFloat2[0]) * (f > 0.0f ? 1.0f : -1.0f);
    }

    /* access modifiers changed from: package-private */
    public float backgroundAlphaInterpolator(float f) {
        if (f < 0.1f) {
            return 0.0f;
        }
        if (f > 0.4f) {
            return 1.0f;
        }
        return (f - 0.1f) / (0.4f - 0.1f);
    }

    /* access modifiers changed from: package-private */
    public float overScrollBackgroundAlphaInterpolator(float f) {
        float f2;
        if (f > this.mOverScrollMaxBackgroundAlpha) {
            this.mOverScrollMaxBackgroundAlpha = f;
            f2 = f;
        } else {
            f2 = f < this.mOverScrollMaxBackgroundAlpha ? this.mOverScrollMaxBackgroundAlpha : f;
        }
        return Math.min(f2 / 0.08f, 1.0f);
    }

    /* access modifiers changed from: protected */
    public void screenScrolled(int i) {
        int measuredWidth = getMeasuredWidth() / 2;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < getChildCount()) {
                CellLayout cellLayout = (CellLayout) getChildAt(i3);
                if (cellLayout != null) {
                    float max = Math.max(Math.min(((float) (i - ((getChildOffset(i3) - getRelativeChildOffset(i3)) + measuredWidth))) / (((float) (getScaledMeasuredWidth(cellLayout) + this.mPageSpacing)) * 1.0f), 1.0f), -1.0f);
                    if ((getScrollX() < 0 && i3 == 0) || (getScrollX() > this.mMaxScrollX && i3 == getChildCount() - 1)) {
                        cellLayout.setBackgroundAlphaMultiplier(overScrollBackgroundAlphaInterpolator(Math.abs(max)));
                        this.mOverScrollPageIndex = i3;
                    } else if (this.mOverScrollPageIndex != i3) {
                        cellLayout.setBackgroundAlphaMultiplier(backgroundAlphaInterpolator(Math.abs(max)));
                    }
                    float f = max * 12.5f;
                    cellLayout.setTranslationX(getOffsetXForRotation(f, cellLayout.getWidth(), cellLayout.getHeight()));
                    cellLayout.setRotationY(f);
                }
                i2 = i3 + 1;
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mWindowToken = getWindowToken();
        computeScroll();
        this.mDragController.setWindowToken(this.mWindowToken);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        this.mWindowToken = null;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mFirstLayout && this.mCurrentPage >= 0 && this.mCurrentPage < getChildCount()) {
            this.mUpdateWallpaperOffsetImmediately = true;
        }
        super.onLayout(changed, left, top, right, bottom);
        if (this.mWaitingToShrink) {
            post(new Runnable() {
                public void run() {
                    Workspace.this.shrink(Workspace.this.mWaitingToShrinkState, false);
                    boolean unused = Workspace.this.mWaitingToShrink = false;
                }
            });
        }
        if (LauncherApplication.isInPlaceRotationEnabled()) {
            setCurrentPage(getCurrentPage());
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        updateWallpaperOffsets();
        if (this.mBackground != null && this.mBackgroundAlpha > 0.0f && this.mDrawBackground) {
            int i = (int) (this.mBackgroundAlpha * 255.0f);
            if (this.mDrawCustomizeTrayBackground) {
                this.mCustomizationDrawer.getLocationOnScreen(this.mCustomizationDrawerPos);
                Matrix matrix = this.mCustomizationDrawer.getMatrix();
                this.mCustomizationDrawerTransformedPos[0] = 0.0f;
                this.mCustomizationDrawerTransformedPos[1] = (float) this.mCustomizationDrawerContent.getTop();
                matrix.mapPoints(this.mCustomizationDrawerTransformedPos);
                this.mCustomizeTrayBackground.setAlpha(i);
                this.mCustomizeTrayBackground.setBounds(getScrollX(), 0, getScrollX() + getMeasuredWidth(), getMeasuredHeight());
                this.mCustomizeTrayBackground.draw(canvas);
                int i2 = (int) (((float) this.mCustomizationDrawerPos[1]) + this.mCustomizationDrawerTransformedPos[1]);
                this.mBackground.setAlpha(i);
                this.mBackground.setBounds(getScrollX(), i2, getScrollX() + getMeasuredWidth(), getMeasuredHeight() + i2);
                this.mBackground.draw(canvas);
            } else {
                this.mBackground.setAlpha(i);
                this.mBackground.setBounds(getScrollX(), 0, getScrollX() + getMeasuredWidth(), getMeasuredHeight());
                this.mBackground.draw(canvas);
            }
        }
        super.onDraw(canvas);
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        if (this.mIsSmall || this.mIsInUnshrinkAnimation) {
            int childCount = getChildCount();
            long drawingTime = getDrawingTime();
            for (int i = 0; i < childCount; i++) {
                CellLayout cellLayout = (CellLayout) getChildAt(i);
                if (cellLayout.getVisibility() == 0 && !(cellLayout.getAlpha() == 0.0f && cellLayout.getBackgroundAlpha() == 0.0f)) {
                    drawChild(canvas, cellLayout, drawingTime);
                }
            }
            return;
        }
        super.dispatchDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (this.mInScrollArea && height > width) {
            int height2 = getChildAt(0).getHeight();
            int i2 = (int) ((((float) height2) * 0.1f) + (((float) (height - height2)) * 0.5f));
            CellLayout cellLayout2 = (CellLayout) getChildAt(this.mCurrentPage - 1);
            CellLayout cellLayout3 = (CellLayout) getChildAt(this.mCurrentPage + 1);
            if (cellLayout2 != null && cellLayout2.getIsDragOverlapping()) {
                Drawable drawable = getResources().getDrawable(R.drawable.page_hover_left);
                drawable.setBounds(getScrollX(), i2, getScrollX() + drawable.getIntrinsicWidth(), height - i2);
                drawable.draw(canvas);
            } else if (cellLayout3 != null && cellLayout3.getIsDragOverlapping()) {
                Drawable drawable2 = getResources().getDrawable(R.drawable.page_hover_right);
                drawable2.setBounds((getScrollX() + width) - drawable2.getIntrinsicWidth(), i2, width + getScrollX(), height - i2);
                drawable2.draw(canvas);
            }
        }
        if (this.mDropView != null) {
            canvas.save();
            canvas.translate((float) (this.mDropViewPos[0] - this.mDropView.getScrollX()), (float) (this.mDropViewPos[1] - this.mDropView.getScrollY()));
            this.mDropView.draw(canvas);
            canvas.restore();
        }
    }

    /* access modifiers changed from: protected */
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (this.mLauncher == null || this.mLauncher.isAllAppsVisible()) {
            return false;
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            return openFolder.requestFocus(direction, previouslyFocusedRect);
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (this.mLauncher == null || !this.mLauncher.isAllAppsVisible()) {
            Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                openFolder.addFocusables(views, direction);
            } else {
                super.addFocusables(views, direction, focusableMode);
            }
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() != 0 || LauncherApplication.isScreenXLarge() || this.mLauncher == null || !this.mLauncher.isAllAppsVisible()) {
            return super.dispatchTouchEvent(ev);
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void enableChildrenCache(int fromPage, int toPage) {
        if (fromPage > toPage) {
            int temp = fromPage;
            fromPage = toPage;
            toPage = temp;
        }
        int screenCount = getChildCount();
        int fromPage2 = Math.max(fromPage, 0);
        int toPage2 = Math.min(toPage, screenCount - 1);
        for (int i = fromPage2; i <= toPage2; i++) {
            CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(true);
            layout.setChildrenDrawingCacheEnabled(true);
        }
    }

    /* access modifiers changed from: package-private */
    public void clearChildrenCache() {
        int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            ((CellLayout) getChildAt(i)).setChildrenDrawnWithCacheEnabled(false);
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mLauncher == null) return super.onTouchEvent(ev);
        AllAppsPagedView allApps = (AllAppsPagedView) this.mLauncher.findViewById(R.id.all_apps_paged_view);
        if (!this.mLauncher.isAllAppsVisible() || this.mShrinkState != ShrinkState.BOTTOM_HIDDEN || allApps == null) {
            return super.onTouchEvent(ev);
        }
        if (ev.getAction() != 1 || allApps.getTouchState() != 0) {
            return allApps.onTouchEvent(ev);
        }
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
        setCurrentPage(this.mCurrentPage);
        if (this.mShrinkState == ShrinkState.BOTTOM_HIDDEN) {
            this.mLauncher.showWorkspace(true);
        }
        allApps.onTouchEvent(ev);
        return true;
    }

    /* access modifiers changed from: protected */
    public void enableChildrenLayers(boolean enable) {
        final int layerType = enable ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE;
        for (int i = 0; i < getPageCount(); i++) {
            View page = getChildAt(i);
            if (page instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) page;
                for (int j = 0; j < vg.getChildCount(); j++) {
                    vg.getChildAt(j).setLayerType(layerType, null);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void pageBeginMoving() {
        enableChildrenLayers(true);
        super.pageBeginMoving();
    }

    /* access modifiers changed from: protected */
    public void pageEndMoving() {
        if (!this.mIsSmall && !this.mIsInUnshrinkAnimation) {
            enableChildrenLayers(false);
        }
        super.pageEndMoving();
    }

    /* access modifiers changed from: protected */
    public void onWallpaperTap(MotionEvent motionEvent) {
        int[] iArr = this.mTempCell;
        getLocationOnScreen(iArr);
        int actionIndex = motionEvent.getActionIndex();
        iArr[0] = iArr[0] + ((int) motionEvent.getX(actionIndex));
        iArr[1] = ((int) motionEvent.getY(actionIndex)) + iArr[1];
        this.mWallpaperManager.sendWallpaperCommand(getWindowToken(), motionEvent.getAction() == 1 ? "android.wallpaper.tap" : "android.wallpaper.secondaryTap", iArr[0], iArr[1], 0, (Bundle) null);
    }

    public boolean isSmall() {
        return this.mIsSmall;
    }

    private float getYScaleForScreen(int i) {
        switch (Math.abs(i - 2)) {
            case 0:
                return 0.972f;
            case 1:
                return 1.0f;
            case 2:
                return 1.1f;
            default:
                return 1.0f;
        }
    }

    public void shrink(ShrinkState shrinkState) {
        shrink(shrinkState, true);
    }

    private int getCustomizeDrawerHeight() {
        TabHost customizationDrawer = this.mLauncher.getCustomizationDrawer();
        int height = customizationDrawer.getHeight();
        TabWidget tabWidget = (TabWidget) customizationDrawer.findViewById(16908307);
        if (tabWidget.getTabCount() > 0) {
            return height - ((tabWidget.getHeight() - ((TextView) tabWidget.getChildTabViewAt(0)).getLineHeight()) / 2);
        }
        return height;
    }

    public void shrink(ShrinkState shrinkState, boolean z) {
        ShrinkState shrinkState2;
        int i;
        float f;
        int i2;
        final float verticalWallpaperOffsetTarget;
        if (!this.mIsSmall || this.mShrinkState != ShrinkState.BOTTOM_VISIBLE) {
            shrinkState2 = shrinkState;
        } else {
            shrinkState2 = ShrinkState.BOTTOM_VISIBLE;
        }
        if (this.mFirstLayout) {
            this.mWaitingToShrink = true;
            this.mWaitingToShrinkState = shrinkState2;
            return;
        }
        if (this.mNextPage != -1) {
            i = this.mNextPage;
        } else {
            i = this.mCurrentPage;
        }
        setCurrentPage(i);
        if (!this.mIsDragInProcess) {
            updateWhichPagesAcceptDrops(shrinkState2);
        }
        CellLayout cellLayout = (CellLayout) getChildAt(this.mCurrentPage);
        if (cellLayout == null) {
            Log.w("Launcher.Workspace", "currentPage is NULL! mCurrentPage " + this.mCurrentPage + " mNextPage " + this.mNextPage);
            return;
        }
        if (cellLayout.getBackgroundAlphaMultiplier() < 1.0f) {
            cellLayout.setBackgroundAlpha(0.0f);
        }
        cellLayout.setBackgroundAlphaMultiplier(1.0f);
        this.mIsSmall = true;
        this.mShrinkState = shrinkState2;
        this.mTouchState = 0;
        this.mActivePointerId = -1;
        Resources resources = getResources();
        int width = getWidth();
        int height = getHeight();
        float integer = ((float) resources.getInteger(R.integer.config_workspaceShrinkPercent)) / 100.0f;
        int measuredWidth = getChildAt(0).getMeasuredWidth();
        int measuredHeight = getChildAt(0).getMeasuredHeight();
        int i3 = (int) (((float) measuredWidth) * integer);
        int i4 = (int) (((float) measuredHeight) * integer);
        float dimension = resources.getDimension(R.dimen.smallScreenExtraSpacing);
        final int childCount = getChildCount();
        float f3 = ((float) (childCount * i3)) + (((float) (childCount - 1)) * dimension);
        float dimension2 = getMeasuredHeight() > getMeasuredWidth() ? getResources().getDimension(R.dimen.allAppsSmallScreenVerticalMarginPortrait) : getResources().getDimension(R.dimen.allAppsSmallScreenVerticalMarginLandscape);
        if (shrinkState2 == ShrinkState.BOTTOM_VISIBLE) {
            dimension2 = (((float) height) - dimension2) - ((float) i4);
            f = 1.0f;
        } else if (shrinkState2 == ShrinkState.BOTTOM_HIDDEN) {
            dimension2 = (((float) height) - dimension2) - ((float) i4);
            f = 0.0f;
        } else if (shrinkState2 == ShrinkState.MIDDLE) {
            dimension2 = (float) ((height / 2) - (i4 / 2));
            f = 1.0f;
        } else if (shrinkState2 == ShrinkState.TOP) {
            dimension2 = (float) (((height - getCustomizeDrawerHeight()) - i4) / 2);
            f = 1.0f;
        } else {
            f = 1.0f;
        }
        if (shrinkState2 == ShrinkState.BOTTOM_HIDDEN || shrinkState2 == ShrinkState.BOTTOM_VISIBLE) {
            i2 = resources.getInteger(R.integer.config_allAppsWorkspaceShrinkTime);
        } else {
            i2 = resources.getInteger(R.integer.config_customizeWorkspaceShrinkTime);
        }
        float finalX = (((float) ((width / 2) + this.mScroller.getFinalX())) - (f3 / 2.0f)) - (((float) (measuredWidth - i3)) / 2.0f);
        float f4 = dimension2 - (((float) (measuredHeight - i4)) / 2.0f);
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        this.mAnimator = new AnimatorSet();
        int childCount2 = getChildCount();
        final float[] fArr = new float[childCount2];
        final float[] fArr2 = new float[childCount2];
        final float[] fArr3 = new float[childCount2];
        final float[] fArr4 = new float[childCount2];
        final float[] fArr5 = new float[childCount2];
        final float[] fArr6 = new float[childCount2];
        final float[] fArr7 = new float[childCount2];
        final float[] fArr8 = new float[childCount2];
        final float[] fArr9 = new float[childCount2];
        final float[] fArr10 = new float[childCount2];
        final float[] fArr11 = new float[childCount2];
        final float[] fArr12 = new float[childCount2];
        final float[] fArr13 = new float[childCount2];
        final float[] fArr14 = new float[childCount2];
        float f5 = finalX;
        for (int i5 = 0; i5 < childCount; i5++) {
            CellLayout cellLayout2 = (CellLayout) getChildAt(i5);
            float f6 = ((float) ((-i5) + 2)) * 12.5f;
            float cos = (float) (1.0d / Math.cos((3.141592653589793d * ((double) f6)) / 180.0d));
            float yScaleForScreen = getYScaleForScreen(i5);
            fArr6[i5] = cellLayout2.getAlpha();
            fArr13[i5] = f;
            if (z && !(fArr6[i5] == 0.0f && fArr13[i5] == 0.0f)) {
                cellLayout2.buildChildrenLayer();
            }
            if (z) {
                fArr[i5] = cellLayout2.getX();
                fArr2[i5] = cellLayout2.getY();
                fArr3[i5] = cellLayout2.getScaleX();
                fArr4[i5] = cellLayout2.getScaleY();
                fArr5[i5] = cellLayout2.getBackgroundAlpha();
                fArr7[i5] = cellLayout2.getRotationY();
                fArr8[i5] = f5;
                fArr9[i5] = f4;
                fArr10[i5] = cos * integer * 1.0f;
                fArr11[i5] = integer * yScaleForScreen * 1.0f;
                fArr12[i5] = f;
                fArr14[i5] = f6;
            } else {
                cellLayout2.setX((float) ((int) f5));
                cellLayout2.setY((float) ((int) f4));
                cellLayout2.setScaleX(cos * integer * 1.0f);
                cellLayout2.setScaleY(integer * yScaleForScreen * 1.0f);
                cellLayout2.setBackgroundAlpha(f);
                cellLayout2.setAlpha(f);
                cellLayout2.setRotationY(f6);
                this.mShrinkAnimationListener.onAnimationEnd((Animator) null);
            }
            f5 += ((float) i3) + dimension;
        }
        float f7 = 0.5f;
        Display defaultDisplay = this.mLauncher.getWindowManager().getDefaultDisplay();
        float height2 = (((float) ((int) (((float) defaultDisplay.getHeight()) * wallpaperTravelToScreenHeightRatio(defaultDisplay.getWidth(), defaultDisplay.getHeight())))) / ((float) this.mWallpaperHeight)) / 2.0f;
        boolean z2 = defaultDisplay.getWidth() > defaultDisplay.getHeight();
        switch (AnonymousClass14.$SwitchMap$com$android$launcher2$Workspace$ShrinkState[shrinkState2.ordinal()]) {
            case 1:
                float f8 = 0.5f + height2;
                this.mWallpaperOffset.setVerticalCatchupConstant(z2 ? 0.46f : 0.44f);
                f7 = f8;
                break;
            case 2:
            case 3:
                this.mWallpaperOffset.setVerticalCatchupConstant(z2 ? 0.34f : 0.32f);
                f7 = 0.5f;
                break;
            case 4:
            case 5:
                f7 = 0.5f - height2;
                this.mWallpaperOffset.setVerticalCatchupConstant(z2 ? 0.34f : 0.32f);
                break;
        }
        verticalWallpaperOffsetTarget = f7;
        setLayoutScale(1.0f);
        if (z) {
            this.mWallpaperOffset.setHorizontalCatchupConstant(0.46f);
            this.mWallpaperOffset.setOverrideHorizontalCatchupConstant(true);
            this.mSyncWallpaperOffsetWithScroll = false;
            ValueAnimator duration = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f}).setDuration((long) i2);
            duration.setInterpolator(this.mZoomOutInterpolator);
            final float horizontalWallpaperOffset = getHorizontalWallpaperOffset();
            final float verticalWallpaperOffset = getVerticalWallpaperOffset();
            duration.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float b = ((Float) animation.getAnimatedValue()).floatValue();
                    float a = 1.0f - b;
                    if (b != 0.0f) {
                        Workspace.this.invalidate();
                        Workspace.this.setHorizontalWallpaperOffset((horizontalWallpaperOffset * a) + (0.5f * b));
                        Workspace.this.setVerticalWallpaperOffset((verticalWallpaperOffset * a) + (verticalWallpaperOffsetTarget * b));
                        for (int i = 0; i < childCount; i++) {
                            CellLayout cl = (CellLayout) Workspace.this.getChildAt(i);
                            cl.invalidate();
                            cl.setX((fArr[i] * a) + (fArr8[i] * b));
                            cl.setY((fArr2[i] * a) + (fArr9[i] * b));
                            cl.setScaleX((fArr3[i] * a) + (fArr10[i] * b));
                            cl.setScaleY((fArr4[i] * a) + (fArr11[i] * b));
                            cl.setFastBackgroundAlpha((fArr5[i] * a) + (fArr12[i] * b));
                            cl.setFastAlpha((fArr6[i] * a) + (fArr13[i] * b));
                            cl.setRotationY((fArr7[i] * a) + (fArr14[i] * b));
                        }
                    }
                }
            });
            this.mAnimator.playTogether(new Animator[]{duration});
            this.mAnimator.addListener(this.mShrinkAnimationListener);
            this.mAnimator.start();
        } else {
            setVerticalWallpaperOffset(verticalWallpaperOffsetTarget);
            setHorizontalWallpaperOffset(0.5f);
            updateWallpaperOffsetImmediately();
        }
        setChildrenDrawnWithCacheEnabled(true);
        if (shrinkState2 == ShrinkState.TOP) {
            showBackgroundGradientForCustomizeTray();
        } else {
            showBackgroundGradientForAllApps();
        }
    }

    /* renamed from: com.android.launcher2.Workspace$14  reason: invalid class name */
    static /* synthetic */ class AnonymousClass14 {
        static final /* synthetic */ int[] $SwitchMap$com$android$launcher2$Workspace$ShrinkState = new int[ShrinkState.values().length];

        static {
            try {
                $SwitchMap$com$android$launcher2$Workspace$ShrinkState[ShrinkState.TOP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$launcher2$Workspace$ShrinkState[ShrinkState.MIDDLE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$launcher2$Workspace$ShrinkState[ShrinkState.SPRING_LOADED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$launcher2$Workspace$ShrinkState[ShrinkState.BOTTOM_HIDDEN.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$launcher2$Workspace$ShrinkState[ShrinkState.BOTTOM_VISIBLE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    static class ZInterpolator implements TimeInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            this.focalLength = foc;
        }

        public float getInterpolation(float input) {
            return (1.0f - (this.focalLength / (this.focalLength + input))) / (1.0f - (this.focalLength / (this.focalLength + 1.0f)));
        }
    }

    static class InverseZInterpolator implements TimeInterpolator {
        private ZInterpolator zInterpolator;

        public InverseZInterpolator(float foc) {
            this.zInterpolator = new ZInterpolator(foc);
        }

        public float getInterpolation(float input) {
            return 1.0f - this.zInterpolator.getInterpolation(1.0f - input);
        }
    }

    static class ZoomOutInterpolator implements TimeInterpolator {
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(1.8f);
        private final ZInterpolator zInterpolator = new ZInterpolator(0.2f);

        ZoomOutInterpolator() {
        }

        public float getInterpolation(float input) {
            return this.decelerate.getInterpolation(this.zInterpolator.getInterpolation(input));
        }
    }

    static class ZoomInInterpolator implements TimeInterpolator {
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);
        private final InverseZInterpolator inverseZInterpolator = new InverseZInterpolator(0.35f);

        ZoomInInterpolator() {
        }

        public float getInterpolation(float input) {
            return this.decelerate.getInterpolation(this.inverseZInterpolator.getInterpolation(input));
        }
    }

    private void updateWhichPagesAcceptDrops(ShrinkState state) {
        updateWhichPagesAcceptDropsHelper(state, false, 1, 1);
    }

    private void updateWhichPagesAcceptDropsDuringDrag(ShrinkState state, int spanX, int spanY) {
        updateWhichPagesAcceptDropsHelper(state, true, spanX, spanY);
    }

    private void updateWhichPagesAcceptDropsHelper(ShrinkState shrinkState, boolean z, int i, int i2) {
        boolean z2;
        int childCount = getChildCount();
        int i3 = i2;
        int i4 = i;
        for (int i5 = 0; i5 < childCount; i5++) {
            CellLayout cellLayout = (CellLayout) getChildAt(i5);
            cellLayout.setIsDragOccuring(z);
            switch (AnonymousClass14.$SwitchMap$com$android$launcher2$Workspace$ShrinkState[shrinkState.ordinal()]) {
                case 1:
                    if (i5 == this.mCurrentPage) {
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    cellLayout.setIsDefaultDropTarget(z2);
                    break;
                case 3:
                case 4:
                case 5:
                    break;
                default:
                    throw new RuntimeException("Unhandled ShrinkState " + shrinkState);
            }
            if (shrinkState != ShrinkState.TOP) {
                cellLayout.setIsDefaultDropTarget(false);
            }
            if (!z) {
                i3 = 1;
                i4 = 1;
            }
            cellLayout.setAcceptsDrops(cellLayout.findCellForSpan((int[]) null, i4, i3));
        }
    }

    public void onDragStartedWithItemSpans(int spanX, int spanY, Bitmap b) {
        this.mIsDragInProcess = true;
        Canvas canvas = new Canvas();
        int bitmapPadding = HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS;
        int[] desiredSize = ((CellLayout) getChildAt(0)).cellSpansToSize(spanX, spanY);
        this.mDragOutline = createDragOutline(b, canvas, bitmapPadding, desiredSize[0], desiredSize[1]);
        updateWhichPagesAcceptDropsDuringDrag(this.mShrinkState, spanX, spanY);
    }

    public void onDragStopped(boolean success) {
        this.mLastDragView = null;
        if (!success) {
            doDragExit();
        }
        this.mIsDragInProcess = false;
        updateWhichPagesAcceptDrops(this.mShrinkState);
    }

    public void unshrink(CellLayout clThatWasClicked) {
        unshrink(clThatWasClicked, false);
    }

    public void unshrink(CellLayout cellLayout, boolean z) {
        int indexOfChild = indexOfChild(cellLayout);
        if (this.mIsSmall) {
            if (z) {
                setLayoutScale(0.7f);
            }
            scrollToNewPageWithoutMovingPages(indexOfChild);
            unshrink(true, z);
        }
    }

    public void enterSpringLoadedDragMode(CellLayout clThatWasClicked) {
        this.mShrinkState = ShrinkState.SPRING_LOADED;
        unshrink(clThatWasClicked, true);
        this.mDragTargetLayout.onDragEnter();
    }

    public void exitSpringLoadedDragMode(ShrinkState shrinkState) {
        shrink(shrinkState);
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.onDragExit();
        }
    }

    public void exitWidgetResizeMode() {
        ((CellLayout) getChildAt(getCurrentPage())).getChildrenLayout().clearAllResizeFrames();
    }

    /* access modifiers changed from: package-private */
    public void unshrink(boolean animated) {
        unshrink(animated, false);
    }

    /* access modifiers changed from: package-private */
    public void unshrink(boolean z, boolean z2) {
        float f;
        this.mWaitingToShrink = false;
        if (this.mIsSmall) {
            float f2 = 0.0f;
            if (z2) {
                f2 = 1.0f;
                f = 0.7f;
            } else {
                this.mIsSmall = false;
                f = 1.0f;
            }
            if (this.mAnimator != null) {
                this.mAnimator.cancel();
            }
            this.mAnimator = new AnimatorSet();
            final int childCount = getChildCount();
            int integer = getResources().getInteger(R.integer.config_workspaceUnshrinkTime);
            final float[] fArr = new float[getChildCount()];
            final float[] fArr2 = new float[getChildCount()];
            final float[] fArr3 = new float[getChildCount()];
            final float[] fArr4 = new float[getChildCount()];
            final float[] fArr5 = new float[getChildCount()];
            final float[] fArr6 = new float[getChildCount()];
            final float[] fArr7 = new float[getChildCount()];
            float[] fArr8 = new float[getChildCount()];
            final float[] fArr9 = new float[getChildCount()];
            final float[] fArr10 = new float[getChildCount()];
            final float[] fArr11 = new float[getChildCount()];
            final float[] fArr12 = new float[getChildCount()];
            final float[] fArr13 = new float[getChildCount()];
            final float[] fArr14 = new float[getChildCount()];
            final float[] fArr15 = new float[getChildCount()];
            float[] fArr16 = new float[getChildCount()];
            int i = 0;
            while (i < childCount) {
                CellLayout cellLayout = (CellLayout) getChildAt(i);
                float f3 = i == this.mCurrentPage ? 1.0f : 0.0f;
                float f4 = (i != this.mCurrentPage || this.mShrinkState == ShrinkState.SPRING_LOADED) ? 1.0f : 0.0f;
                float f5 = 0.0f;
                if (i < this.mCurrentPage) {
                    f5 = 12.5f;
                } else {
                    if (i > this.mCurrentPage) {
                        f5 = -12.5f;
                    }
                }
                float offsetXForRotation = getOffsetXForRotation(f5, cellLayout.getWidth(), cellLayout.getHeight());
                fArr7[i] = cellLayout.getAlpha();
                fArr15[i] = f3;
                if (z) {
                    fArr[i] = cellLayout.getTranslationX();
                    fArr2[i] = cellLayout.getTranslationY();
                    fArr3[i] = cellLayout.getScaleX();
                    fArr4[i] = cellLayout.getScaleY();
                    fArr5[i] = cellLayout.getBackgroundAlpha();
                    fArr6[i] = cellLayout.getBackgroundAlphaMultiplier();
                    fArr8[i] = cellLayout.getRotationY();
                    fArr9[i] = offsetXForRotation;
                    fArr10[i] = 0.0f;
                    fArr11[i] = f;
                    fArr12[i] = f;
                    fArr13[i] = f2;
                    fArr14[i] = f4;
                    fArr16[i] = f5;
                } else {
                    cellLayout.setTranslationX(offsetXForRotation);
                    cellLayout.setTranslationY(0.0f);
                    cellLayout.setScaleX(f);
                    cellLayout.setScaleY(f);
                    cellLayout.setBackgroundAlpha(0.0f);
                    cellLayout.setBackgroundAlphaMultiplier(f4);
                    cellLayout.setAlpha(f3);
                    cellLayout.setRotationY(f5);
                    this.mUnshrinkAnimationListener.onAnimationEnd((Animator) null);
                }
                i++;
            }
            Display defaultDisplay = this.mLauncher.getWindowManager().getDefaultDisplay();
            boolean z3 = defaultDisplay.getWidth() > defaultDisplay.getHeight();
            switch (AnonymousClass14.$SwitchMap$com$android$launcher2$Workspace$ShrinkState[this.mShrinkState.ordinal()]) {
                case 1:
                    if (z) {
                        this.mWallpaperOffset.setHorizontalCatchupConstant(z3 ? 0.65f : 0.62f);
                        this.mWallpaperOffset.setVerticalCatchupConstant(z3 ? 0.65f : 0.62f);
                        this.mWallpaperOffset.setOverrideHorizontalCatchupConstant(true);
                        break;
                    }
                    break;
                case 2:
                case 3:
                    if (z) {
                        this.mWallpaperOffset.setHorizontalCatchupConstant(z3 ? 0.49f : 0.46f);
                        this.mWallpaperOffset.setVerticalCatchupConstant(z3 ? 0.49f : 0.46f);
                        this.mWallpaperOffset.setOverrideHorizontalCatchupConstant(true);
                        break;
                    }
                    break;
                case 4:
                case 5:
                    if (z) {
                        this.mWallpaperOffset.setHorizontalCatchupConstant(z3 ? 0.65f : 0.65f);
                        this.mWallpaperOffset.setVerticalCatchupConstant(z3 ? 0.65f : 0.65f);
                        this.mWallpaperOffset.setOverrideHorizontalCatchupConstant(true);
                        break;
                    }
                    break;
            }
            if (z) {
                ValueAnimator duration = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f}).setDuration((long) integer);
                duration.setInterpolator(this.mZoomInInterpolator);
                final float horizontalWallpaperOffset = getHorizontalWallpaperOffset();
                final float verticalWallpaperOffset = getVerticalWallpaperOffset();
                final float wallpaperOffsetForCurrentScroll = wallpaperOffsetForCurrentScroll();
                duration.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float b = ((Float) animation.getAnimatedValue()).floatValue();
                        float a = 1.0f - b;
                        if (b != 0.0f) {
                            Workspace.this.invalidate();
                            Workspace.this.setHorizontalWallpaperOffset((horizontalWallpaperOffset * a) + (wallpaperOffsetForCurrentScroll * b));
                            Workspace.this.setVerticalWallpaperOffset((verticalWallpaperOffset * a) + (0.5f * b));
                            for (int i = 0; i < childCount; i++) {
                                CellLayout cl = (CellLayout) Workspace.this.getChildAt(i);
                                cl.invalidate();
                                cl.setTranslationX((fArr[i] * a) + (fArr9[i] * b));
                                cl.setTranslationY((fArr2[i] * a) + (fArr10[i] * b));
                                cl.setScaleX((fArr3[i] * a) + (fArr11[i] * b));
                                cl.setScaleY((fArr4[i] * a) + (fArr12[i] * b));
                                cl.setFastBackgroundAlpha((fArr5[i] * a) + (fArr13[i] * b));
                                cl.setBackgroundAlphaMultiplier((fArr6[i] * a) + (fArr14[i] * b));
                                cl.setFastAlpha((fArr7[i] * a) + (fArr15[i] * b));
                            }
                        }
                    }
                });
                ValueAnimator duration2 = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f}).setDuration((long) integer);
                duration2.setInterpolator(new DecelerateInterpolator(2.0f));
                final int i2 = childCount;
                final float[] fArr17 = fArr8;
                final float[] fArr18 = fArr16;
                duration2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float b = ((Float) animation.getAnimatedValue()).floatValue();
                        float a = 1.0f - b;
                        if (b != 0.0f) {
                            for (int i = 0; i < i2; i++) {
                                ((CellLayout) Workspace.this.getChildAt(i)).setRotationY((fArr17[i] * a) + (fArr18[i] * b));
                            }
                        }
                    }
                });
                this.mAnimator.playTogether(new Animator[]{duration, duration2});
                this.mAnimator.addListener(this.mUnshrinkAnimationListener);
                this.mAnimator.start();
            } else {
                setHorizontalWallpaperOffset(wallpaperOffsetForCurrentScroll());
                setVerticalWallpaperOffset(0.5f);
                updateWallpaperOffsetImmediately();
            }
        }
        if (!z2) {
            hideBackgroundGradient();
        }
    }

    private void drawDragView(View v, Canvas destCanvas, int padding) {
        Rect clipRect = this.mTempRect;
        v.getDrawingRect(clipRect);
        if (v instanceof BubbleTextView) {
            BubbleTextView tv = (BubbleTextView) v;
            clipRect.bottom = (tv.getExtendedPaddingTop() - 3) + tv.getLayout().getLineTop(0);
        } else if (v instanceof TextView) {
            TextView tv2 = (TextView) v;
            clipRect.bottom = (tv2.getExtendedPaddingTop() - tv2.getCompoundDrawablePadding()) + tv2.getLayout().getLineTop(0);
        }
        destCanvas.save();
        destCanvas.translate((float) ((-v.getScrollX()) + (padding / 2)), (float) ((-v.getScrollY()) + (padding / 2)));
        destCanvas.clipRect(clipRect);
        v.draw(destCanvas);
        destCanvas.restore();
    }

    private Bitmap createDragOutline(View view, Canvas canvas, int i) {
        int color = getResources().getColor(R.color.drag_outline_color);
        Bitmap createBitmap = Bitmap.createBitmap(view.getWidth() + i, view.getHeight() + i, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(createBitmap);
        drawDragView(view, canvas, i);
        this.mOutlineHelper.applyMediumExpensiveOutlineWithBlur(createBitmap, canvas, color, color);
        return createBitmap;
    }

    private Bitmap createDragOutline(Bitmap bitmap, Canvas canvas, int i, int i2, int i3) {
        int color = getResources().getColor(R.color.drag_outline_color);
        Bitmap createBitmap = Bitmap.createBitmap(i2, i3, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(createBitmap);
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        float min = Math.min(((float) (i2 - i)) / ((float) bitmap.getWidth()), ((float) (i3 - i)) / ((float) bitmap.getHeight()));
        int width = (int) (((float) bitmap.getWidth()) * min);
        int height = (int) (min * ((float) bitmap.getHeight()));
        Rect rect2 = new Rect(0, 0, width, height);
        rect2.offset((i2 - width) / 2, (i3 - height) / 2);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, rect, rect2, paint);
        this.mOutlineHelper.applyMediumExpensiveOutlineWithBlur(createBitmap, canvas, color, color);
        return createBitmap;
    }

    private Bitmap createExternalDragOutline(Canvas canvas, int i) {
        Resources resources = getResources();
        int color = resources.getColor(R.color.drag_outline_color);
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int dimensionPixelSize3 = resources.getDimensionPixelSize(R.dimen.external_drop_icon_rect_radius);
        int min = (int) (((float) Math.min(dimensionPixelSize, dimensionPixelSize2)) * 0.2f);
        Bitmap createBitmap = Bitmap.createBitmap(dimensionPixelSize + i, dimensionPixelSize2 + i, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(createBitmap);
        canvas.drawRoundRect(new RectF((float) min, (float) min, (float) (dimensionPixelSize - min), (float) (dimensionPixelSize2 - min)), (float) dimensionPixelSize3, (float) dimensionPixelSize3, this.mExternalDragOutlinePaint);
        this.mOutlineHelper.applyMediumExpensiveOutlineWithBlur(createBitmap, canvas, color, color);
        return createBitmap;
    }

    private Bitmap createDragBitmap(View view, Canvas canvas, int i) {
        int color = getResources().getColor(R.color.drag_outline_color);
        Bitmap createBitmap = Bitmap.createBitmap(this.mDragOutline.getWidth(), this.mDragOutline.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(createBitmap);
        canvas.drawBitmap(this.mDragOutline, 0.0f, 0.0f, (Paint) null);
        drawDragView(view, canvas, i);
        this.mOutlineHelper.applyOuterBlur(createBitmap, canvas, color);
        return createBitmap;
    }

    /* access modifiers changed from: package-private */
    public void startDrag(CellLayout.CellInfo cellInfo) {
        View child = cellInfo.cell;
        if (child.isInTouchMode()) {
            this.mDragInfo = cellInfo;
            ((CellLayout) getChildAt(cellInfo.screen)).onDragChild(child);
            child.clearFocus();
            child.setPressed(false);
            Canvas canvas = new Canvas();
            int bitmapPadding = HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS;
            this.mDragOutline = createDragOutline(child, canvas, bitmapPadding);
            Bitmap b = createDragBitmap(child, canvas, bitmapPadding);
            int bmpWidth = b.getWidth();
            int bmpHeight = b.getHeight();
            child.getLocationOnScreen(this.mTempXY);
            int screenX = this.mTempXY[0] + ((child.getWidth() - bmpWidth) / 2);
            int screenY = this.mTempXY[1] + ((child.getHeight() - bmpHeight) / 2);
            this.mLauncher.lockScreenOrientation();
            this.mDragController.startDrag(b, screenX, screenY, (DragSource) this, child.getTag(), DragController.DRAG_ACTION_MOVE);
            b.recycle();
        }
    }

    /* access modifiers changed from: package-private */
    public void addApplicationShortcut(ShortcutInfo shortcutInfo, int i, int i2, int i3, boolean z, int i4, int i5) {
        CellLayout cellLayout = (CellLayout) getChildAt(i);
        View createShortcut = this.mLauncher.createShortcut(R.layout.application, cellLayout, shortcutInfo);
        int[] iArr = new int[2];
        cellLayout.findCellForSpanThatIntersects(iArr, 1, 1, i4, i5);
        addInScreen(createShortcut, i, iArr[0], iArr[1], 1, 1, z);
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, shortcutInfo, -100, i, iArr[0], iArr[1]);
    }

    private void setPositionForDropAnimation(View view, int i, int i2, View view2, View view3) {
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view3.getLayoutParams();
        int width = ((view.getWidth() - view3.getWidth()) / 2) + i + getResources().getInteger(R.integer.config_dragViewOffsetX);
        int height = ((view.getHeight() - view3.getHeight()) / 2) + i2 + getResources().getInteger(R.integer.config_dragViewOffsetY);
        layoutParams.oldX = width - (view2.getLeft() - getScrollX());
        layoutParams.oldY = height - (view2.getTop() - getScrollY());
    }

    public void animateViewIntoPosition(final View view) {
        int i;
        CellLayout cellLayout = (CellLayout) view.getParent().getParent();
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
        final int left = cellLayout.getLeft() + layoutParams.oldX;
        final int top = layoutParams.oldY + cellLayout.getTop();
        final int i2 = layoutParams.x - layoutParams.oldX;
        final int i3 = layoutParams.y - layoutParams.oldY;
        float sqrt = (float) Math.sqrt((double) ((i2 * i2) + (i3 * i3)));
        Resources resources = getResources();
        float integer = (float) resources.getInteger(R.integer.config_dropAnimMaxDist);
        int integer2 = resources.getInteger(R.integer.config_dropAnimMaxDuration);
        if (sqrt < integer) {
            i = (int) (this.mQuintEaseOutInterpolator.getInterpolation(sqrt / integer) * ((float) integer2));
        } else {
            i = integer2;
        }
        if (this.mDropAnim != null) {
            this.mDropAnim.end();
        }
        this.mDropAnim = new ValueAnimator();
        this.mDropAnim.setInterpolator(this.mQuintEaseOutInterpolator);
        this.mDropAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                View unused = Workspace.this.mDropView = view;
            }

            public void onAnimationEnd(Animator animation) {
                if (Workspace.this.mDropView != null) {
                    Workspace.this.mDropView.setVisibility(0);
                    View unused = Workspace.this.mDropView = null;
                }
            }
        });
        this.mDropAnim.setDuration((long) i);
        this.mDropAnim.setFloatValues(new float[]{0.0f, 1.0f});
        this.mDropAnim.removeAllUpdateListeners();
        final View view2 = view;
        this.mDropAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float percent = ((Float) animation.getAnimatedValue()).floatValue();
                Workspace.this.invalidate(Workspace.this.mDropViewPos[0], Workspace.this.mDropViewPos[1], Workspace.this.mDropViewPos[0] + view2.getWidth(), Workspace.this.mDropViewPos[1] + view2.getHeight());
                Workspace.this.mDropViewPos[0] = left + ((int) ((((float) i2) * percent) + 0.5f));
                Workspace.this.mDropViewPos[1] = top + ((int) ((((float) i3) * percent) + 0.5f));
                Workspace.this.invalidate(Workspace.this.mDropViewPos[0], Workspace.this.mDropViewPos[1], Workspace.this.mDropViewPos[0] + view2.getWidth(), Workspace.this.mDropViewPos[1] + view2.getHeight());
            }
        });
        this.mDropAnim.start();
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (source != this) {
            if (this.mDragTargetLayout == null || !this.mDragTargetLayout.getAcceptsDrops()) {
                return false;
            }
            CellLayout.CellInfo dragCellInfo = this.mDragInfo;
            if (!this.mDragTargetLayout.findCellForSpanIgnoring((int[]) null, dragCellInfo == null ? 1 : dragCellInfo.spanX, dragCellInfo == null ? 1 : dragCellInfo.spanY, dragCellInfo == null ? null : dragCellInfo.cell)) {
                this.mLauncher.showOutOfSpaceMessage();
                return false;
            }
        }
        return true;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        int originY;
        int screen;
        int dragTargetIndex;
        boolean largeOrSpringLoaded = !this.mIsSmall || this.mWasSpringLoadedOnDragExit;
        int originX = largeOrSpringLoaded ? x - xOffset : (x - xOffset) + (dragView.getWidth() / 2);
        if (largeOrSpringLoaded) {
            originY = y - yOffset;
        } else {
            originY = (y - yOffset) + (dragView.getHeight() / 2);
        }
        if (this.mIsSmall || this.mIsInUnshrinkAnimation) {
            this.mTempOriginXY[0] = (float) originX;
            this.mTempOriginXY[1] = (float) originY;
            mapPointFromSelfToChild(this.mDragTargetLayout, this.mTempOriginXY);
            originX = (int) this.mTempOriginXY[0];
            originY = (int) this.mTempOriginXY[1];
            if (!largeOrSpringLoaded) {
                originX -= this.mDragTargetLayout.getCellWidth() / 2;
                originY -= this.mDragTargetLayout.getCellHeight() / 2;
            }
        }
        if (!this.mLauncher.isAllAppsVisible() && this.mCurrentPage != (dragTargetIndex = indexOfChild(this.mDragTargetLayout)) && (this.mIsSmall || this.mIsInUnshrinkAnimation)) {
            scrollToNewPageWithoutMovingPages(dragTargetIndex);
        }
        if (source != this) {
            int[] touchXY = {originX, originY};
            if ((this.mIsSmall || this.mIsInUnshrinkAnimation) && !this.mLauncher.isAllAppsVisible()) {
                ((ItemInfo) dragInfo).dropPos = touchXY;
            } else {
                onDropExternal(touchXY, dragInfo, this.mDragTargetLayout, false);
            }
        } else if (this.mDragInfo != null) {
            View cell = this.mDragInfo.cell;
            CellLayout dropTargetLayout = this.mDragTargetLayout;
            if (dropTargetLayout == null && this.mInScrollArea) {
                if (this.mPendingScrollDirection == 0) {
                    dropTargetLayout = (CellLayout) getChildAt(this.mCurrentPage - 1);
                } else if (this.mPendingScrollDirection == 1) {
                    dropTargetLayout = (CellLayout) getChildAt(this.mCurrentPage + 1);
                }
            }
            if (dropTargetLayout != null) {
                this.mTargetCell = findNearestVacantArea(originX, originY, this.mDragInfo.spanX, this.mDragInfo.spanY, cell, dropTargetLayout, this.mTargetCell);
                if (this.mTargetCell == null) {
                    screen = this.mDragInfo.screen;
                } else {
                    screen = indexOfChild(dropTargetLayout);
                }
                if (screen != this.mCurrentPage) {
                    snapToPage(screen);
                }
                if (this.mTargetCell != null) {
                    if (screen != this.mDragInfo.screen) {
                        ((CellLayout) getChildAt(this.mDragInfo.screen)).removeView(cell);
                        addInScreen(cell, screen, this.mTargetCell[0], this.mTargetCell[1], this.mDragInfo.spanX, this.mDragInfo.spanY);
                    }
                    ItemInfo info = (ItemInfo) cell.getTag();
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    dropTargetLayout.onMove(cell, this.mTargetCell[0], this.mTargetCell[1]);
                    lp.cellX = this.mTargetCell[0];
                    lp.cellY = this.mTargetCell[1];
                    cell.setId(LauncherModel.getCellLayoutChildId(-1, this.mDragInfo.screen, this.mTargetCell[0], this.mTargetCell[1], this.mDragInfo.spanX, this.mDragInfo.spanY));
                    if (cell instanceof LauncherAppWidgetHostView) {
                        final CellLayoutChildren children = dropTargetLayout.getChildrenLayout();
                        final CellLayout cellLayout = dropTargetLayout;
                        final LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) cell;
                        if (hostView.getAppWidgetInfo().resizeMode != 0) {
                            final ItemInfo itemInfo = info;
                            post(new Runnable() {
                                public void run() {
                                    children.addResizeFrame(itemInfo, hostView, cellLayout);
                                }
                            });
                        }
                    }
                    LauncherModel.moveItemInDatabase(this.mLauncher, info, -100, screen, lp.cellX, lp.cellY);
                }
            }
            CellLayout parent = (CellLayout) cell.getParent().getParent();
            setPositionForDropAnimation(dragView, originX, originY, parent, cell);
            parent.onDropChild(cell, !this.mWasSpringLoadedOnDragExit);
        }
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        this.mDragTargetLayout = null;
        if (!this.mIsSmall) {
            this.mDragTargetLayout = getCurrentDropLayout();
            this.mDragTargetLayout.onDragEnter();
            showOutlines();
        }
    }

    public DropTarget getDropTargetDelegate(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        int dragPointX;
        int dragPointY;
        if (this.mIsSmall || this.mIsInUnshrinkAnimation) {
            return null;
        }
        ItemInfo item = (ItemInfo) dragInfo;
        CellLayout currentLayout = getCurrentDropLayout();
        if (item.spanX == 1 && item.spanY == 1) {
            dragPointX = x - xOffset;
            dragPointY = y - yOffset;
        } else {
            dragPointX = x;
            dragPointY = y;
        }
        int[] cellXY = this.mTempCell;
        currentLayout.estimateDropCell(dragPointX + (getScrollX() - currentLayout.getLeft()), dragPointY + (getScrollY() - currentLayout.getTop()), item.spanX, item.spanY, cellXY);
        View child = currentLayout.getChildAt(cellXY[0], cellXY[1]);
        if (child instanceof DropTarget) {
            DropTarget target = (DropTarget) child;
            if (target.acceptDrop(source, x, y, xOffset, yOffset, dragView, dragInfo)) {
                return target;
            }
        }
        return null;
    }

    private Pair<Integer, List<InstallWidgetReceiver.WidgetMimeTypeHandlerData>> validateDrag(DragEvent dragEvent) {
        LauncherModel model = this.mLauncher.getModel();
        ClipDescription clipDescription = dragEvent.getClipDescription();
        int mimeTypeCount = clipDescription.getMimeTypeCount();
        for (int i = 0; i < mimeTypeCount; i++) {
            String mimeType = clipDescription.getMimeType(i);
            if (mimeType.equals("com.android.launcher/shortcut")) {
                return new Pair<>(Integer.valueOf(i), (List<InstallWidgetReceiver.WidgetMimeTypeHandlerData>) null);
            }
            List<InstallWidgetReceiver.WidgetMimeTypeHandlerData> resolveWidgetsForMimeType = model.resolveWidgetsForMimeType(getContext(), mimeType);
            if (resolveWidgetsForMimeType.size() > 0) {
                return new Pair<>(Integer.valueOf(i), resolveWidgetsForMimeType);
            }
        }
        return null;
    }

    public boolean onDragEvent(DragEvent dragEvent) {
        ClipDescription clipDescription = dragEvent.getClipDescription();
        CellLayout cellLayout = (CellLayout) getChildAt(this.mCurrentPage);
        int[] iArr = new int[2];
        cellLayout.getLocationOnScreen(iArr);
        int x = ((int) dragEvent.getX()) - iArr[0];
        int y = ((int) dragEvent.getY()) - iArr[1];
        switch (dragEvent.getAction()) {
            case 1:
                Pair<Integer, List<InstallWidgetReceiver.WidgetMimeTypeHandlerData>> validateDrag = validateDrag(dragEvent);
                if (validateDrag != null) {
                    if (!(validateDrag.second == null) || cellLayout.findCellForSpan(iArr, 1, 1)) {
                        this.mDragOutline = createExternalDragOutline(new Canvas(), HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS);
                        showOutlines();
                        cellLayout.setIsDragOccuring(true);
                        cellLayout.onDragEnter();
                        cellLayout.visualizeDropLocation((View) null, this.mDragOutline, x, y, 1, 1);
                        return true;
                    }
                    Toast.makeText(getContext(), getContext().getString(R.string.out_of_space), 0).show();
                    return false;
                }
                Toast.makeText(getContext(), getContext().getString(R.string.external_drop_widget_error), 0).show();
                return false;
            case 2:
                cellLayout.visualizeDropLocation((View) null, this.mDragOutline, x, y, 1, 1);
                return true;
            case DragEvent.ACTION_DROP:
                LauncherModel model = this.mLauncher.getModel();
                ClipData clipData = dragEvent.getClipData();
                iArr[0] = x;
                iArr[1] = y;
                Pair<Integer, List<InstallWidgetReceiver.WidgetMimeTypeHandlerData>> validateDrag2 = validateDrag(dragEvent);
                if (validateDrag2 != null) {
                    int intValue = ((Integer) validateDrag2.first).intValue();
                    List list = (List) validateDrag2.second;
                    boolean z = list == null;
                    String mimeType = clipDescription.getMimeType(intValue);
                    if (z) {
                        onDropExternal(new int[]{x, y}, model.infoFromShortcutIntent(getContext(), clipData.getItemAt(intValue).getIntent(), (Bitmap) null), cellLayout, false);
                    } else if (list.size() == 1) {
                        this.mLauncher.addAppWidgetFromDrop(new PendingAddWidgetInfo(((InstallWidgetReceiver.WidgetMimeTypeHandlerData) list.get(0)).widgetInfo, mimeType, clipData), this.mCurrentPage, iArr);
                    } else {
                        InstallWidgetReceiver.WidgetListAdapter widgetListAdapter = new InstallWidgetReceiver.WidgetListAdapter(this.mLauncher, mimeType, clipData, list, cellLayout, this.mCurrentPage, iArr);
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setAdapter(widgetListAdapter, widgetListAdapter);
                        builder.setCancelable(true);
                        builder.setTitle(getContext().getString(R.string.external_drop_widget_pick_title));
                        builder.setIcon(R.drawable.ic_no_applications);
                        builder.show();
                    }
                }
                return true;
            case 4:
                cellLayout.setIsDragOccuring(false);
                cellLayout.onDragExit();
                hideOutlines();
                return true;
            default:
                return super.onDragEvent(dragEvent);
        }
    }

    /* access modifiers changed from: package-private */
    public void mapPointFromSelfToChild(View v, float[] xy) {
        mapPointFromSelfToChild(v, xy, (Matrix) null);
    }

    /* access modifiers changed from: package-private */
    public void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
        if (cachedInverseMatrix == null) {
            v.getMatrix().invert(this.mTempInverseMatrix);
            cachedInverseMatrix = this.mTempInverseMatrix;
        }
        xy[0] = (xy[0] + ((float) getScrollX())) - ((float) v.getLeft());
        xy[1] = (xy[1] + ((float) getScrollY())) - ((float) v.getTop());
        cachedInverseMatrix.mapPoints(xy);
    }

    /* access modifiers changed from: package-private */
    public void mapPointFromChildToSelf(View v, float[] xy) {
        v.getMatrix().mapPoints(xy);
        xy[0] = xy[0] - ((float) (getScrollX() - v.getLeft()));
        xy[1] = xy[1] - ((float) (getScrollY() - v.getTop()));
    }

    private static float squaredDistance(float[] point1, float[] point2) {
        float distanceX = point1[0] - point2[0];
        float distanceY = point2[1] - point2[1];
        return (distanceX * distanceX) + (distanceY * distanceY);
    }

    /* access modifiers changed from: package-private */
    public boolean overlaps(CellLayout cl, DragView dragView, int dragViewX, int dragViewY, Matrix cachedInverseMatrix) {
        float[] draggedItemTopLeft = this.mTempDragCoordinates;
        draggedItemTopLeft[0] = (float) dragViewX;
        draggedItemTopLeft[1] = (float) dragViewY;
        float[] draggedItemBottomRight = this.mTempDragBottomRightCoordinates;
        draggedItemBottomRight[0] = draggedItemTopLeft[0] + ((float) dragView.getDragRegionWidth());
        draggedItemBottomRight[1] = draggedItemTopLeft[1] + ((float) dragView.getDragRegionHeight());
        mapPointFromSelfToChild(cl, draggedItemTopLeft, cachedInverseMatrix);
        float overlapRegionLeft = Math.max(0.0f, draggedItemTopLeft[0]);
        float overlapRegionTop = Math.max(0.0f, draggedItemTopLeft[1]);
        if (overlapRegionLeft <= ((float) cl.getWidth()) && overlapRegionTop >= 0.0f) {
            mapPointFromSelfToChild(cl, draggedItemBottomRight, cachedInverseMatrix);
            float overlapRegionRight = Math.min((float) cl.getWidth(), draggedItemBottomRight[0]);
            float overlapRegionBottom = Math.min((float) cl.getHeight(), draggedItemBottomRight[1]);
            if (overlapRegionRight < 0.0f || overlapRegionBottom > ((float) cl.getHeight()) || (overlapRegionRight - overlapRegionLeft) * (overlapRegionBottom - overlapRegionTop) <= 0.0f) {
                return false;
            }
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public CellLayout findMatchingPageForDragOver(DragView dragView, int i, int i2, int i3, int i4) {
        float f;
        int childCount = getChildCount();
        int i5 = 0;
        float f2 = Float.MAX_VALUE;
        CellLayout cellLayout = null;
        while (i5 < childCount) {
            CellLayout cellLayout2 = (CellLayout) getChildAt(i5);
            float[] fArr = this.mTempTouchCoordinates;
            fArr[0] = (float) (i + i3);
            fArr[1] = (float) (i2 + i4);
            cellLayout2.getMatrix().invert(this.mTempInverseMatrix);
            mapPointFromSelfToChild(cellLayout2, fArr, this.mTempInverseMatrix);
            if (fArr[0] >= 0.0f && fArr[0] <= ((float) cellLayout2.getWidth()) && fArr[1] >= 0.0f && fArr[1] <= ((float) cellLayout2.getHeight())) {
                return cellLayout2;
            }
            if (overlaps(cellLayout2, dragView, i, i2, this.mTempInverseMatrix)) {
                float[] fArr2 = this.mTempCellLayoutCenterCoordinates;
                fArr2[0] = (float) (cellLayout2.getWidth() / 2);
                fArr2[1] = (float) (cellLayout2.getHeight() / 2);
                mapPointFromChildToSelf(cellLayout2, fArr2);
                fArr[0] = (float) (i + i3);
                fArr[1] = (float) (i2 + i4);
                f = squaredDistance(fArr, fArr2);
                if (f < f2) {
                    i5++;
                    f2 = f;
                    cellLayout = cellLayout2;
                }
            }
            f = f2;
            cellLayout2 = cellLayout;
            i5++;
            f2 = f;
            cellLayout = cellLayout2;
        }
        return cellLayout;
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        boolean shrunken;
        boolean z;
        if (!this.mInScrollArea) {
            int originX = x - xOffset;
            int originY = y - yOffset;
            if (this.mIsSmall || this.mIsInUnshrinkAnimation) {
                shrunken = true;
            } else {
                shrunken = false;
            }
            if (shrunken) {
                this.mLastDragView = dragView;
                this.mLastDragOriginX = originX;
                this.mLastDragOriginY = originY;
                this.mLastDragXOffset = xOffset;
                this.mLastDragYOffset = yOffset;
                CellLayout layout = findMatchingPageForDragOver(dragView, originX, originY, xOffset, yOffset);
                if (layout != this.mDragTargetLayout) {
                    if (this.mDragTargetLayout != null) {
                        this.mDragTargetLayout.setIsDragOverlapping(false);
                        this.mSpringLoadedDragController.onDragExit();
                    }
                    this.mDragTargetLayout = layout;
                    if (this.mDragTargetLayout != null && (this.mDragTargetLayout.getAcceptsDrops() || this.mShrinkState == ShrinkState.SPRING_LOADED)) {
                        this.mDragTargetLayout.setIsDragOverlapping(true);
                        SpringLoadedDragController springLoadedDragController = this.mSpringLoadedDragController;
                        CellLayout cellLayout = this.mDragTargetLayout;
                        if (this.mShrinkState == ShrinkState.SPRING_LOADED) {
                            z = true;
                        } else {
                            z = false;
                        }
                        springLoadedDragController.onDragEnter(cellLayout, z);
                    }
                }
            } else {
                CellLayout layout2 = getCurrentDropLayout();
                if (layout2 != this.mDragTargetLayout) {
                    if (this.mDragTargetLayout != null) {
                        this.mDragTargetLayout.onDragExit();
                    }
                    layout2.onDragEnter();
                    this.mDragTargetLayout = layout2;
                }
            }
            if (!shrunken || this.mShrinkState == ShrinkState.SPRING_LOADED) {
                CellLayout layout3 = getCurrentDropLayout();
                ItemInfo item = (ItemInfo) dragInfo;
                if (dragInfo instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) dragInfo;
                    if (widgetInfo.spanX == -1) {
                        int[] spans = layout3.rectToCell(widgetInfo.minWidth, widgetInfo.minHeight, (int[]) null);
                        item.spanX = spans[0];
                        item.spanY = spans[1];
                    }
                }
                if (source instanceof AllAppsPagedView) {
                    if (!(item == null || item.spanX != 1 || layout3 == null)) {
                        originX += ((dragView.getWidth() - layout3.getCellWidth()) / 2) - dragView.getDragRegionLeft();
                        if (dragView.getDragRegionWidth() != layout3.getCellWidth()) {
                            dragView.setDragRegion(dragView.getDragRegionLeft(), dragView.getDragRegionTop(), layout3.getCellWidth(), dragView.getDragRegionHeight());
                        }
                    }
                } else if (source == this) {
                    View origView = this.mDragInfo.cell;
                    originX += (dragView.getMeasuredWidth() - origView.getWidth()) / 2;
                    originY = (int) (((float) originY) + ((float) ((dragView.getMeasuredHeight() - origView.getHeight()) / 2)) + dragView.getOffsetY());
                }
                if (this.mDragTargetLayout != null) {
                    View child = this.mDragInfo == null ? null : this.mDragInfo.cell;
                    float[] localOrigin = {(float) originX, (float) originY};
                    mapPointFromSelfToChild(this.mDragTargetLayout, localOrigin, (Matrix) null);
                    this.mDragTargetLayout.visualizeDropLocation(child, this.mDragOutline, (int) localOrigin[0], (int) localOrigin[1], item.spanX, item.spanY);
                }
            }
        }
    }

    private void doDragExit() {
        this.mWasSpringLoadedOnDragExit = this.mShrinkState == ShrinkState.SPRING_LOADED;
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.onDragExit();
        }
        if (!this.mIsPageMoving) {
            hideOutlines();
        }
        if (this.mShrinkState == ShrinkState.SPRING_LOADED) {
            this.mLauncher.exitSpringLoadedDragMode();
        }
        clearAllHovers();
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        doDragExit();
    }

    public void getHitRect(Rect outRect) {
        Display d = this.mLauncher.getWindowManager().getDefaultDisplay();
        outRect.set(0, 0, d.getWidth(), d.getHeight());
    }

    public boolean addExternalItemToScreen(ItemInfo dragInfo, CellLayout layout) {
        if (layout.findCellForSpan(this.mTempEstimate, dragInfo.spanX, dragInfo.spanY)) {
            onDropExternal(dragInfo.dropPos, dragInfo, layout, false);
            return true;
        }
        this.mLauncher.showOutOfSpaceMessage();
        return false;
    }

    private void onDropExternal(int[] iArr, Object obj, CellLayout cellLayout, boolean z) {
        View fromXml;
        ItemInfo itemInfo;
        ItemInfo itemInfo2;
        int indexOfChild = indexOfChild(cellLayout);
        if (obj instanceof PendingAddItemInfo) {
            PendingAddItemInfo pendingAddItemInfo = (PendingAddItemInfo) obj;
            switch (pendingAddItemInfo.itemType) {
                case 1:
                    this.mLauncher.processShortcutFromDrop(pendingAddItemInfo.componentName, indexOfChild, iArr);
                    break;
                case 3:
                    this.mLauncher.addLiveFolderFromDrop(pendingAddItemInfo.componentName, indexOfChild, iArr);
                    break;
                case 4:
                    this.mLauncher.addAppWidgetFromDrop((PendingAddWidgetInfo) pendingAddItemInfo, indexOfChild, iArr);
                    break;
                default:
                    throw new IllegalStateException("Unknown item type: " + pendingAddItemInfo.itemType);
            }
            cellLayout.onDragExit();
            return;
        }
        ItemInfo itemInfo3 = (ItemInfo) obj;
        switch (itemInfo3.itemType) {
            case 0:
            case 1:
                if (itemInfo3.container != -1 || !(itemInfo3 instanceof ApplicationInfo)) {
                    itemInfo2 = itemInfo3;
                } else {
                    itemInfo2 = new ShortcutInfo((ApplicationInfo) itemInfo3);
                }
                fromXml = this.mLauncher.createShortcut(R.layout.application, cellLayout, (ShortcutInfo) itemInfo2);
                itemInfo = itemInfo2;
                break;
            case 2:
                fromXml = FolderIcon.fromXml(R.layout.launcher3h_folder_icon, this.mLauncher, cellLayout, (UserFolderInfo) itemInfo3, this.mIconCache);
                itemInfo = itemInfo3;
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + itemInfo3.itemType);
        }
        this.mTargetCell = new int[2];
        if (iArr != null) {
            cellLayout.findNearestVacantArea(iArr[0], iArr[1], 1, 1, this.mTargetCell);
        } else {
            cellLayout.findCellForSpan(this.mTargetCell, 1, 1);
        }
        addInScreen(fromXml, indexOfChild(cellLayout), this.mTargetCell[0], this.mTargetCell[1], itemInfo.spanX, itemInfo.spanY, z);
        cellLayout.onDropChild(fromXml, !this.mWasSpringLoadedOnDragExit);
        cellLayout.animateDrop();
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) fromXml.getLayoutParams();
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, itemInfo, -100, indexOfChild, layoutParams.cellX, layoutParams.cellY);
    }

    /* Debug info: failed to restart local var, previous not found, register: 2 */
    public CellLayout getCurrentDropLayout() {
        return (CellLayout) getChildAt(this.mNextPage == -1 ? this.mCurrentPage : this.mNextPage);
    }

    private int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY, View ignoreView, CellLayout layout, int[] recycle) {
        return layout.findNearestVacantArea(pixelX - (layout.getLeft() - getScrollX()), pixelY - (layout.getTop() - getScrollY()), spanX, spanY, ignoreView, recycle);
    }

    /* access modifiers changed from: package-private */
    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
        this.mSpringLoadedDragController = new SpringLoadedDragController(this.mLauncher);
        this.mCustomizationDrawer = this.mLauncher.findViewById(R.id.customization_drawer);
        if (this.mCustomizationDrawer != null) {
            this.mCustomizationDrawerContent = this.mCustomizationDrawer.findViewById(16908305);
        }
    }

    public void setDragController(DragController dragController) {
        this.mDragController = dragController;
    }

    public void onDropCompleted(View target, Object dragInfo, boolean success) {
        if (success) {
            if (!(target == this || this.mDragInfo == null)) {
                ((CellLayout) getChildAt(this.mDragInfo.screen)).removeView(this.mDragInfo.cell);
                if (this.mDragInfo.cell instanceof DropTarget) {
                    this.mDragController.removeDropTarget((DropTarget) this.mDragInfo.cell);
                }
            }
        } else if (this.mDragInfo != null) {
            doDragExit();
            ((CellLayout) getChildAt(this.mDragInfo.screen)).onDropChild(this.mDragInfo.cell, false);
        }
        this.mLauncher.unlockScreenOrientation();
        this.mDragOutline = null;
        this.mDragInfo = null;
    }

    public void onDragViewVisible() {
        this.mDragInfo.cell.setVisibility(8);
    }

    public boolean isDropEnabled() {
        return true;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        Launcher.setScreen(this.mCurrentPage);
    }

    public void scrollLeft() {
        if (!this.mIsSmall && !this.mIsInUnshrinkAnimation) {
            super.scrollLeft();
        }
    }

    public void scrollRight() {
        if (!this.mIsSmall && !this.mIsInUnshrinkAnimation) {
            super.scrollRight();
        }
    }

    public void onEnterScrollArea(int direction) {
        if (!this.mIsSmall && !this.mIsInUnshrinkAnimation) {
            this.mInScrollArea = true;
            this.mPendingScrollDirection = direction;
            CellLayout layout = (CellLayout) getChildAt(this.mCurrentPage + (direction == 0 ? -1 : 1));
            if (layout != null) {
                layout.setIsDragOverlapping(true);
                if (this.mDragTargetLayout != null) {
                    this.mDragTargetLayout.onDragExit();
                    this.mDragTargetLayout = null;
                }
                if (getHeight() > getWidth()) {
                    invalidate();
                }
            }
        }
    }

    private void clearAllHovers() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((CellLayout) getChildAt(i)).setIsDragOverlapping(false);
        }
        this.mSpringLoadedDragController.onDragExit();
        if (getHeight() > getWidth()) {
            invalidate();
        }
    }

    public void onExitScrollArea() {
        if (this.mInScrollArea) {
            this.mInScrollArea = false;
            this.mPendingScrollDirection = -1;
            clearAllHovers();
        }
    }

    public Folder getFolderForTag(Object tag) {
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            ViewGroup currentScreen = ((CellLayout) getChildAt(screen)).getChildrenLayout();
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                if (lp.cellHSpan == 4 && lp.cellVSpan == 4 && (child instanceof Folder)) {
                    Folder f = (Folder) child;
                    if (f.getInfo() == tag && f.getInfo().opened) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public View getViewForTag(Object tag) {
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            ViewGroup currentScreen = ((CellLayout) getChildAt(screen)).getChildrenLayout();
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                if (child.getTag() == tag) {
                    return child;
                }
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public void removeItems(ArrayList<ApplicationInfo> apps) {
        int screenCount = getChildCount();
        final PackageManager manager = getContext().getPackageManager();
        final AppWidgetManager widgets = AppWidgetManager.getInstance(getContext());
        final HashSet<String> packageNames = new HashSet<>();
        int appCount = apps.size();
        for (int i = 0; i < appCount; i++) {
            packageNames.add(apps.get(i).componentName.getPackageName());
        }
        for (int i2 = 0; i2 < screenCount; i2++) {
            final CellLayout layoutParent = (CellLayout) getChildAt(i2);
            final ViewGroup layout = layoutParent.getChildrenLayout();
            post(new Runnable() {
                public void run() {
                    Folder folder;
                    ArrayList<View> childrenToRemove = new ArrayList<>();
                    childrenToRemove.clear();
                    int childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        View view = layout.getChildAt(j);
                        Object tag = view.getTag();
                        if (tag instanceof ShortcutInfo) {
                            ShortcutInfo info = (ShortcutInfo) tag;
                            Intent intent = info.intent;
                            ComponentName name = intent.getComponent();
                            if ("android.intent.action.MAIN".equals(intent.getAction()) && name != null) {
                                Iterator i$ = packageNames.iterator();
                                while (i$.hasNext()) {
                                    if (((String) i$.next()).equals(name.getPackageName())) {
                                        LauncherModel.deleteItemFromDatabase(Workspace.this.mLauncher, info);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        } else if (tag instanceof UserFolderInfo) {
                            ArrayList<ShortcutInfo> contents = ((UserFolderInfo) tag).contents;
                            ArrayList arrayList = new ArrayList(1);
                            int contentsCount = contents.size();
                            boolean removedFromFolder = false;
                            for (int k = 0; k < contentsCount; k++) {
                                ShortcutInfo appInfo = contents.get(k);
                                Intent intent2 = appInfo.intent;
                                ComponentName name2 = intent2.getComponent();
                                if ("android.intent.action.MAIN".equals(intent2.getAction()) && name2 != null) {
                                    Iterator i$2 = packageNames.iterator();
                                    while (i$2.hasNext()) {
                                        if (((String) i$2.next()).equals(name2.getPackageName())) {
                                            arrayList.add(appInfo);
                                            LauncherModel.deleteItemFromDatabase(Workspace.this.mLauncher, appInfo);
                                            removedFromFolder = true;
                                        }
                                    }
                                }
                            }
                            contents.removeAll(arrayList);
                            if (removedFromFolder && (folder = Workspace.this.getOpenFolder()) != null) {
                                folder.notifyDataSetChanged();
                            }
                        } else if (tag instanceof LiveFolderInfo) {
                            LiveFolderInfo info2 = (LiveFolderInfo) tag;
                            ProviderInfo providerInfo = manager.resolveContentProvider(info2.uri.getAuthority(), 0);
                            if (providerInfo != null) {
                                Iterator i$3 = packageNames.iterator();
                                while (i$3.hasNext()) {
                                    if (((String) i$3.next()).equals(providerInfo.packageName)) {
                                        LauncherModel.deleteItemFromDatabase(Workspace.this.mLauncher, info2);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        } else if (tag instanceof LauncherAppWidgetInfo) {
                            LauncherAppWidgetInfo info3 = (LauncherAppWidgetInfo) tag;
                            AppWidgetProviderInfo provider = widgets.getAppWidgetInfo(info3.appWidgetId);
                            if (provider != null) {
                                Iterator i$4 = packageNames.iterator();
                                while (i$4.hasNext()) {
                                    if (((String) i$4.next()).equals(provider.provider.getPackageName())) {
                                        LauncherModel.deleteItemFromDatabase(Workspace.this.mLauncher, info3);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        }
                    }
                    int childCount2 = childrenToRemove.size();
                    for (int j2 = 0; j2 < childCount2; j2++) {
                        View child = childrenToRemove.get(j2);
                        layoutParent.removeViewInLayout(child);
                        if (child instanceof DropTarget) {
                            Workspace.this.mDragController.removeDropTarget((DropTarget) child);
                        }
                    }
                    if (childCount2 > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                    }
                }
            });
        }
    }

    /* access modifiers changed from: package-private */
    public void updateShortcuts(ArrayList<ApplicationInfo> arrayList) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            CellLayoutChildren childrenLayout = ((CellLayout) getChildAt(i)).getChildrenLayout();
            int childCount2 = childrenLayout.getChildCount();
            for (int i2 = 0; i2 < childCount2; i2++) {
                View childAt = childrenLayout.getChildAt(i2);
                Object tag = childAt.getTag();
                if (tag instanceof ShortcutInfo) {
                    ShortcutInfo shortcutInfo = (ShortcutInfo) tag;
                    Intent intent = shortcutInfo.intent;
                    ComponentName component = intent.getComponent();
                    if (shortcutInfo.itemType == 0 && "android.intent.action.MAIN".equals(intent.getAction()) && component != null) {
                        int size = arrayList.size();
                        for (int i3 = 0; i3 < size; i3++) {
                            if (arrayList.get(i3).componentName.equals(component)) {
                                shortcutInfo.setIcon(this.mIconCache.getIcon(shortcutInfo.intent));
                                ((TextView) childAt).setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new FastBitmapDrawable(shortcutInfo.getIcon(this.mIconCache)), (Drawable) null, (Drawable) null);
                            }
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void moveToDefaultScreen(boolean animate) {
        if (this.mIsSmall || this.mIsInUnshrinkAnimation) {
            this.mLauncher.showWorkspace(animate, (CellLayout) getChildAt(this.mDefaultPage));
        } else if (animate) {
            snapToPage(this.mDefaultPage);
        } else {
            setCurrentPage(this.mDefaultPage);
        }
        getChildAt(this.mDefaultPage).requestFocus();
    }

    /* access modifiers changed from: package-private */
    public void setIndicators(Drawable previous, Drawable next) {
        this.mPreviousIndicator = previous;
        this.mNextIndicator = next;
        previous.setLevel(this.mCurrentPage);
        next.setLevel(this.mCurrentPage);
    }

    public void syncPages() {
    }

    public void syncPageItems(int page) {
    }
}
