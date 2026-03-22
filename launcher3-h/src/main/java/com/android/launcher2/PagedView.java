package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.Checkable;
import android.widget.Scroller;
import java.util.ArrayList;

public abstract class PagedView extends ViewGroup {
    protected static PagedViewIconCache mPageViewIconCache = new PagedViewIconCache();
    private ActionMode mActionMode;
    protected int mActivePointerId;
    protected boolean mAllowLongPress;
    protected boolean mAllowOverScroll;
    protected int mCellCountX;
    protected int mCellCountY;
    protected boolean mCenterPagesVertically;
    protected int mChoiceMode;
    protected boolean mContentIsRefreshable;
    protected int mCurrentPage;
    protected boolean mDeferScrollUpdate;
    private boolean mDirtyPageAlpha;
    private ArrayList<Boolean> mDirtyPageContent;
    private float mDownMotionX;
    protected boolean mFadeInAdjacentScreens;
    protected boolean mFirstLayout;
    protected boolean mIsPageMoving;
    protected float mLastMotionX;
    protected float mLastMotionXRemainder;
    protected float mLastMotionY;
    private int mLastScreenCenter;
    protected float mLayoutScale;
    protected View.OnLongClickListener mLongClickListener;
    protected int mMaxScrollX;
    private int mMaximumVelocity;
    private int mMinimumWidth;
    protected int mNextPage;
    protected int mPageLayoutHeightGap;
    protected int mPageLayoutMaxHeight;
    protected int mPageLayoutPaddingBottom;
    protected int mPageLayoutPaddingLeft;
    protected int mPageLayoutPaddingRight;
    protected int mPageLayoutPaddingTop;
    protected int mPageLayoutWidthGap;
    protected int mPageSpacing;
    private PageSwitchListener mPageSwitchListener;
    private int mPagingTouchSlop;
    protected int mRestorePage;
    protected Scroller mScroller;
    protected float mSmoothingTime;
    protected int mSnapVelocity;
    protected float mTotalMotionX;
    protected int mTouchSlop;
    protected int mTouchState;
    protected float mTouchX;
    protected int mUnboundedScrollX;
    protected boolean mUsePagingTouchSlop;
    private VelocityTracker mVelocityTracker;

    public interface PageSwitchListener {
        void onPageSwitch(View view, int i);
    }

    public abstract void syncPageItems(int i);

    public abstract void syncPages();

    public PagedView(Context context) {
        this(context, (AttributeSet) null);
    }

    public PagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mSnapVelocity = 500;
        this.mFirstLayout = true;
        this.mNextPage = -1;
        this.mRestorePage = -1;
        this.mLastScreenCenter = -1;
        this.mTouchState = 0;
        this.mAllowLongPress = true;
        this.mCellCountX = 0;
        this.mCellCountY = 0;
        this.mAllowOverScroll = true;
        this.mLayoutScale = 1.0f;
        this.mActivePointerId = -1;
        this.mContentIsRefreshable = true;
        this.mFadeInAdjacentScreens = true;
        this.mUsePagingTouchSlop = true;
        this.mDeferScrollUpdate = false;
        this.mIsPageMoving = false;
        this.mChoiceMode = 0;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedView, defStyle, 0);
        this.mPageSpacing = a.getDimensionPixelSize(R.styleable.PagedView_pageSpacing, 0);
        this.mPageLayoutPaddingTop = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutPaddingTop, 0);
        this.mPageLayoutPaddingBottom = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutPaddingBottom, 0);
        this.mPageLayoutPaddingLeft = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutPaddingLeft, 0);
        this.mPageLayoutPaddingRight = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutPaddingRight, 0);
        this.mPageLayoutWidthGap = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutWidthGap, -1);
        this.mPageLayoutHeightGap = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutHeightGap, -1);
        this.mPageLayoutMaxHeight = a.getDimensionPixelSize(R.styleable.PagedView_pageLayoutMaxHeight, -1);
        a.recycle();
        setHapticFeedbackEnabled(false);
        init();
    }

    /* access modifiers changed from: protected */
    public void init() {
        this.mDirtyPageContent = new ArrayList<>();
        this.mDirtyPageContent.ensureCapacity(32);
        this.mScroller = new Scroller(getContext(), new ScrollInterpolator());
        this.mCurrentPage = 0;
        this.mCenterPagesVertically = true;
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mPagingTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    /* access modifiers changed from: package-private */
    public int getCurrentPage() {
        return this.mCurrentPage;
    }

    /* access modifiers changed from: package-private */
    public int getPageCount() {
        return getChildCount();
    }

    /* access modifiers changed from: package-private */
    public View getPageAt(int index) {
        return getChildAt(index);
    }

    public int getTouchState() {
        return this.mTouchState;
    }

    /* access modifiers changed from: protected */
    public void updateCurrentPageScroll() {
        int newX = getChildOffset(this.mCurrentPage) - getRelativeChildOffset(this.mCurrentPage);
        scrollTo(newX, 0);
        this.mScroller.setFinalX(newX);
    }

    /* access modifiers changed from: package-private */
    public void setCurrentPage(int currentPage) {
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
        if (getChildCount() != 0) {
            this.mCurrentPage = Math.max(0, Math.min(currentPage, getPageCount() - 1));
            updateCurrentPageScroll();
            notifyPageSwitchListener();
            invalidate();
        }
    }

    /* access modifiers changed from: protected */
    public void notifyPageSwitchListener() {
        if (this.mPageSwitchListener != null) {
            this.mPageSwitchListener.onPageSwitch(getPageAt(this.mCurrentPage), this.mCurrentPage);
        }
    }

    /* access modifiers changed from: protected */
    public void pageBeginMoving() {
        this.mIsPageMoving = true;
        onPageBeginMoving();
    }

    /* access modifiers changed from: protected */
    public void pageEndMoving() {
        onPageEndMoving();
        this.mIsPageMoving = false;
    }

    /* access modifiers changed from: protected */
    public void onPageBeginMoving() {
    }

    /* access modifiers changed from: protected */
    public void onPageEndMoving() {
    }

    public void setOnLongClickListener(View.OnLongClickListener l) {
        this.mLongClickListener = l;
        int count = getPageCount();
        for (int i = 0; i < count; i++) {
            getPageAt(i).setOnLongClickListener(l);
        }
    }

    public void scrollBy(int x, int y) {
        scrollTo(this.mUnboundedScrollX + x, getScrollY() + y);
    }

    public void scrollTo(int x, int y) {
        this.mUnboundedScrollX = x;
        if (x < 0) {
            super.scrollTo(0, y);
            if (this.mAllowOverScroll) {
                overScroll((float) x);
            }
        } else if (x > this.mMaxScrollX) {
            super.scrollTo(this.mMaxScrollX, y);
            if (this.mAllowOverScroll) {
                overScroll((float) (x - this.mMaxScrollX));
            }
        } else {
            super.scrollTo(x, y);
        }
        this.mTouchX = (float) x;
        this.mSmoothingTime = ((float) System.nanoTime()) / 1.0E9f;
    }

    /* access modifiers changed from: protected */
    public boolean computeScrollHelper() {
        if (this.mScroller.computeScrollOffset()) {
            this.mDirtyPageAlpha = true;
            scrollTo(this.mScroller.getCurrX(), this.mScroller.getCurrY());
            invalidate();
            return true;
        } else if (this.mNextPage == -1) {
            return false;
        } else {
            this.mDirtyPageAlpha = true;
            this.mCurrentPage = Math.max(0, Math.min(this.mNextPage, getPageCount() - 1));
            this.mNextPage = -1;
            notifyPageSwitchListener();
            if (this.mTouchState == 0) {
                pageEndMoving();
            }
            return true;
        }
    }

    public void computeScroll() {
        computeScrollHelper();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childWidthMode;
        int childHeightMode;
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode != 1073741824) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int maxChildHeight = 0;
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        if (this.mPageLayoutMaxHeight != -1) {
            heightSize = Math.min(this.mPageLayoutMaxHeight, heightSize);
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp.width == -2) {
                childWidthMode = Integer.MIN_VALUE;
            } else {
                childWidthMode = 1073741824;
            }
            if (lp.height == -2) {
                childHeightMode = Integer.MIN_VALUE;
            } else {
                childHeightMode = 1073741824;
            }
            child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, childWidthMode), View.MeasureSpec.makeMeasureSpec(heightSize - verticalPadding, childHeightMode));
            maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
        }
        if (heightMode == Integer.MIN_VALUE) {
            heightSize = maxChildHeight + verticalPadding;
        }
        if (childCount > 0) {
            this.mMaxScrollX = getChildOffset(childCount - 1) - getRelativeChildOffset(childCount - 1);
        } else {
            this.mMaxScrollX = 0;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    /* access modifiers changed from: protected */
    public void scrollToNewPageWithoutMovingPages(int newCurrentPage) {
        int delta = (getChildOffset(newCurrentPage) - getRelativeChildOffset(newCurrentPage)) - getScrollX();
        int pageCount = getChildCount();
        for (int i = 0; i < pageCount; i++) {
            View page = getChildAt(i);
            page.setX(page.getX() + ((float) delta));
        }
        setCurrentPage(newCurrentPage);
    }

    public void setLayoutScale(float childrenScale) {
        this.mLayoutScale = childrenScale;
        int childCount = getChildCount();
        float[] childrenX = new float[childCount];
        float[] childrenY = new float[childCount];
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            childrenX[i] = child.getX();
            childrenY[i] = child.getY();
        }
        onLayout(false, getLeft(), getTop(), getRight(), getBottom());
        for (int i2 = 0; i2 < childCount; i2++) {
            View child2 = getChildAt(i2);
            child2.setX(childrenX[i2]);
            child2.setY(childrenY[i2]);
        }
        scrollToNewPageWithoutMovingPages(this.mCurrentPage);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mFirstLayout && this.mCurrentPage >= 0 && this.mCurrentPage < getChildCount()) {
            setHorizontalScrollBarEnabled(false);
            int newX = getChildOffset(this.mCurrentPage) - getRelativeChildOffset(this.mCurrentPage);
            scrollTo(newX, 0);
            this.mScroller.setFinalX(newX);
            setHorizontalScrollBarEnabled(true);
            this.mFirstLayout = false;
        }
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int childCount = getChildCount();
        int childLeft = 0;
        if (childCount > 0) {
            childLeft = getRelativeChildOffset(0);
        }
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                int childWidth = getScaledMeasuredWidth(child);
                int childHeight = child.getMeasuredHeight();
                int childTop = getPaddingTop();
                if (this.mCenterPagesVertically) {
                    childTop += ((getMeasuredHeight() - verticalPadding) - childHeight) / 2;
                }
                child.layout(childLeft, childTop, child.getMeasuredWidth() + childLeft, childTop + childHeight);
                childLeft += this.mPageSpacing + childWidth;
            }
        }
        if (this.mFirstLayout && this.mCurrentPage >= 0 && this.mCurrentPage < getChildCount()) {
            this.mFirstLayout = false;
        }
    }

    /* access modifiers changed from: protected */
    public void forceUpdateAdjacentPagesAlpha() {
        this.mDirtyPageAlpha = true;
        updateAdjacentPagesAlpha();
    }

    /* access modifiers changed from: protected */
    public void updateAdjacentPagesAlpha() {
        if (!this.mFadeInAdjacentScreens) {
            return;
        }
        if (this.mDirtyPageAlpha || this.mTouchState == 1 || !this.mScroller.isFinished()) {
            int screenCenter = getScrollX() + (getMeasuredWidth() / 2);
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View layout = getChildAt(i);
                int childWidth = getScaledMeasuredWidth(layout);
                int halfChildWidth = childWidth / 2;
                int childCenter = getChildOffset(i) + halfChildWidth;
                if (childWidth <= 0) {
                    int childWidth2 = getMeasuredWidth();
                    childCenter = (i * childWidth2) + (childWidth2 / 2);
                }
                int d = halfChildWidth;
                int distanceFromScreenCenter = childCenter - screenCenter;
                if (distanceFromScreenCenter > 0) {
                    if (i > 0) {
                        d += getScaledMeasuredWidth(getChildAt(i - 1)) / 2;
                    }
                } else if (i < childCount - 1) {
                    d += getScaledMeasuredWidth(getChildAt(i + 1)) / 2;
                }
                float dimAlpha = ((float) Math.abs(distanceFromScreenCenter)) / ((float) Math.max(1, d + this.mPageSpacing));
                float alpha = 1.0f - Math.max(0.0f, Math.min(1.0f, dimAlpha * dimAlpha));
                if (alpha < 1.0E-4f) {
                    alpha = 0.0f;
                } else if (alpha > 0.9999f) {
                    alpha = 1.0f;
                }
                layout.setAlpha(alpha);
            }
            this.mDirtyPageAlpha = false;
        }
    }

    /* access modifiers changed from: protected */
    public void screenScrolled(int screenCenter) {
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        int screenCenter = getScrollX() + (getMeasuredWidth() / 2);
        if (screenCenter != this.mLastScreenCenter) {
            screenScrolled(screenCenter);
            updateAdjacentPagesAlpha();
            this.mLastScreenCenter = screenCenter;
        }
        int pageCount = getChildCount();
        if (pageCount > 0) {
            int pageWidth = getScaledMeasuredWidth(getChildAt(0));
            int screenWidth = getMeasuredWidth();
            int x = getRelativeChildOffset(0) + pageWidth;
            int leftScreen = 0;
            while (x <= getScrollX()) {
                leftScreen++;
                x += getScaledMeasuredWidth(getChildAt(leftScreen)) + this.mPageSpacing;
            }
            int rightScreen = leftScreen;
            while (x < getScrollX() + screenWidth && rightScreen < pageCount) {
                rightScreen++;
                if (rightScreen < pageCount) {
                    x += getScaledMeasuredWidth(getChildAt(rightScreen)) + this.mPageSpacing;
                }
            }
            int rightScreen2 = Math.min(getChildCount() - 1, rightScreen);
            long drawingTime = getDrawingTime();
            canvas.save();
            int sx = getScrollX();
            int sy = getScrollY();
            canvas.clipRect(sx, sy, sx + getWidth(), sy + getHeight());
            for (int i = leftScreen; i <= rightScreen2; i++) {
                drawChild(canvas, getChildAt(i), drawingTime);
            }
            canvas.restore();
        }
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        int page = indexOfChild(child);
        if (page == this.mCurrentPage && this.mScroller.isFinished()) {
            return false;
        }
        snapToPage(page);
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int focusablePage;
        if (this.mNextPage != -1) {
            focusablePage = this.mNextPage;
        } else {
            focusablePage = this.mCurrentPage;
        }
        View v = getPageAt(focusablePage);
        if (v == null) {
            return false;
        }
        v.requestFocus(direction, previouslyFocusedRect);
        return false;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (direction == 17) {
            if (getCurrentPage() > 0) {
                snapToPage(getCurrentPage() - 1);
                return true;
            }
        } else if (direction == 66 && getCurrentPage() < getPageCount() - 1) {
            snapToPage(getCurrentPage() + 1);
            return true;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (this.mCurrentPage >= 0 && this.mCurrentPage < getPageCount()) {
            getPageAt(this.mCurrentPage).addFocusables(views, direction);
        }
        if (direction == 17) {
            if (this.mCurrentPage > 0) {
                getPageAt(this.mCurrentPage - 1).addFocusables(views, direction);
            }
        } else if (direction == 66 && this.mCurrentPage < getPageCount() - 1) {
            getPageAt(this.mCurrentPage + 1).addFocusables(views, direction);
        }
    }

    public void focusableViewAvailable(View focused) {
        View current = getPageAt(this.mCurrentPage);
        View v = focused;
        while (v != current) {
            if (v != this && (v.getParent() instanceof View)) {
                v = (View) v.getParent();
            } else {
                return;
            }
        }
        super.focusableViewAvailable(focused);
    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            getChildAt(this.mCurrentPage).cancelLongPress();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    /* access modifiers changed from: protected */
    public boolean hitsPreviousPage(float x, float y) {
        return x < ((float) (getRelativeChildOffset(this.mCurrentPage) - this.mPageSpacing));
    }

    /* access modifiers changed from: protected */
    public boolean hitsNextPage(float x, float y) {
        return x > ((float) ((getMeasuredWidth() - getRelativeChildOffset(this.mCurrentPage)) + this.mPageSpacing));
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */
        acquireVelocityTrackerAndAddMovement(ev);

        // Skip touch handling if there are no pages to swipe
        if (getChildCount() <= 0) {
            return super.onInterceptTouchEvent(ev);
        }

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and he is moving his finger. We want to intercept this motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (this.mTouchState == 1)) {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                if (this.mActivePointerId != -1) {
                    determineScrollingStart(ev);
                    break;
                }
                // If we missed an ACTION_DOWN, treat this MOVE as a DOWN.
            }
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                this.mDownMotionX = x;
                this.mLastMotionX = x;
                this.mLastMotionY = y;
                this.mLastMotionXRemainder = 0.0f;
                this.mTotalMotionX = 0.0f;
                this.mActivePointerId = ev.getPointerId(0);
                this.mAllowLongPress = true;

                final int xDist = Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX());
                final boolean finishedScrolling = this.mScroller.isFinished() || xDist < this.mTouchSlop;
                if (finishedScrolling) {
                    this.mTouchState = 0;
                    this.mScroller.abortAnimation();
                } else {
                    this.mTouchState = 1;
                }

                // Check if this can be the beginning of a tap on the side of the pages to scroll.
                if (this.mTouchState != 2 && this.mTouchState != 3) {
                    if (hitsPreviousPage(x, y)) {
                        this.mTouchState = 2;
                    } else if (hitsNextPage(x, y)) {
                        this.mTouchState = 3;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.mTouchState = 0;
                this.mAllowLongPress = false;
                this.mActivePointerId = -1;
                releaseVelocityTracker();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                releaseVelocityTracker();
                break;
        }

        return this.mTouchState != 0;
    }

    /* access modifiers changed from: protected */
    public void animateClickFeedback(View v, final Runnable r) {
        ObjectAnimator anim = (ObjectAnimator) AnimatorInflater.loadAnimator(getContext(), R.anim.paged_view_click_feedback);
        anim.setTarget(v);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationRepeat(Animator animation) {
                r.run();
            }
        });
        anim.start();
    }

    /* access modifiers changed from: protected */
    public void determineScrollingStart(MotionEvent ev) {
        determineScrollingStart(ev, 1.0f);
    }

    /* access modifiers changed from: protected */
    public void determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        boolean xPaged;
        boolean xMoved;
        boolean yMoved;
        int pointerIndex = ev.findPointerIndex(this.mActivePointerId);
        float x = ev.getX(pointerIndex);
        float y = ev.getY(pointerIndex);
        int xDiff = (int) Math.abs(x - this.mLastMotionX);
        int yDiff = (int) Math.abs(y - this.mLastMotionY);
        int touchSlop = Math.round(((float) this.mTouchSlop) * touchSlopScale);
        if (xDiff > this.mPagingTouchSlop) {
            xPaged = true;
        } else {
            xPaged = false;
        }
        if (xDiff > touchSlop) {
            xMoved = true;
        } else {
            xMoved = false;
        }
        if (yDiff > touchSlop) {
            yMoved = true;
        } else {
            yMoved = false;
        }
        if (xMoved || xPaged || yMoved) {
            if (!this.mUsePagingTouchSlop ? xMoved : xPaged) {
                this.mTouchState = 1;
                this.mTotalMotionX += Math.abs(this.mLastMotionX - x);
                this.mLastMotionX = x;
                this.mLastMotionXRemainder = 0.0f;
                this.mTouchX = (float) getScrollX();
                this.mSmoothingTime = ((float) System.nanoTime()) / 1.0E9f;
                pageBeginMoving();
            }
            cancelCurrentPageLongPress();
        }
    }

    /* access modifiers changed from: protected */
    public void cancelCurrentPageLongPress() {
        if (this.mAllowLongPress) {
            this.mAllowLongPress = false;
            View currentPage = getPageAt(this.mCurrentPage);
            if (currentPage != null) {
                currentPage.cancelLongPress();
            }
        }
    }

    private float overScrollInfluenceCurve(float f) {
        float f2 = f - 1.0f;
        return (f2 * f2 * f2) + 1.0f;
    }

    /* access modifiers changed from: protected */
    public void overScroll(float amount) {
        int screenSize = getMeasuredWidth();
        float f = amount / ((float) screenSize);
        if (f != 0.0f) {
            float f2 = (f / Math.abs(f)) * overScrollInfluenceCurve(Math.abs(f));
            if (Math.abs(f2) >= 1.0f) {
                f2 /= Math.abs(f2);
            }
            int overScrollAmount = Math.round(0.08f * f2 * ((float) screenSize));
            if (amount < 0.0f) {
                super.scrollTo(overScrollAmount, getScrollY());
            } else {
                super.scrollTo(this.mMaxScrollX + overScrollAmount, getScrollY());
            }
            invalidate();
        }
    }

    /* access modifiers changed from: protected */
    public float maxOverScroll() {
        return 0.08f * (1.0f / Math.abs(1.0f)) * overScrollInfluenceCurve(Math.abs(1.0f));
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean isSignificantMove;
        if (getChildCount() <= 0) {
            return super.onTouchEvent(ev);
        }
        acquireVelocityTrackerAndAddMovement(ev);
        switch (ev.getAction() & 255) {
            case 0:
                if (!this.mScroller.isFinished()) {
                    this.mScroller.abortAnimation();
                }
                float x = ev.getX();
                this.mLastMotionX = x;
                this.mDownMotionX = x;
                this.mLastMotionXRemainder = 0.0f;
                this.mTotalMotionX = 0.0f;
                this.mActivePointerId = ev.getPointerId(0);
                if (this.mTouchState == 1) {
                    pageBeginMoving();
                    break;
                }
                break;
            case 1:
                if (this.mTouchState == 1) {
                    int activePointerId = this.mActivePointerId;
                    float x2 = ev.getX(ev.findPointerIndex(activePointerId));
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity(activePointerId);
                    int deltaX = (int) (x2 - this.mDownMotionX);
                    if (Math.abs(deltaX) > 200) {
                        isSignificantMove = true;
                    } else {
                        isSignificantMove = false;
                    }
                    int snapVelocity = this.mSnapVelocity;
                    this.mTotalMotionX += Math.abs((this.mLastMotionX + this.mLastMotionXRemainder) - x2);
                    boolean returnToOriginalPage = false;
                    if (((float) Math.abs(deltaX)) > ((float) getScaledMeasuredWidth(getChildAt(this.mCurrentPage))) * 0.33f && Math.signum((float) velocityX) != Math.signum((float) deltaX)) {
                        returnToOriginalPage = true;
                    }
                    boolean isFling = this.mTotalMotionX > 25.0f && Math.abs(velocityX) > snapVelocity;
                    if (((isSignificantMove && deltaX > 0 && !isFling) || (isFling && velocityX > 0)) && this.mCurrentPage > 0) {
                        snapToPageWithVelocity(returnToOriginalPage ? this.mCurrentPage : this.mCurrentPage - 1, velocityX);
                    } else if (((!isSignificantMove || deltaX >= 0 || isFling) && (!isFling || velocityX >= 0)) || this.mCurrentPage >= getChildCount() - 1) {
                        snapToDestination();
                    } else {
                        snapToPageWithVelocity(returnToOriginalPage ? this.mCurrentPage : this.mCurrentPage + 1, velocityX);
                    }
                } else if (this.mTouchState == 2) {
                    int nextPage = Math.max(0, this.mCurrentPage - 1);
                    if (nextPage != this.mCurrentPage) {
                        snapToPage(nextPage);
                    } else {
                        snapToDestination();
                    }
                } else if (this.mTouchState == 3) {
                    int nextPage2 = Math.min(getChildCount() - 1, this.mCurrentPage + 1);
                    if (nextPage2 != this.mCurrentPage) {
                        snapToPage(nextPage2);
                    } else {
                        snapToDestination();
                    }
                } else {
                    onWallpaperTap(ev);
                }
                this.mTouchState = 0;
                this.mActivePointerId = -1;
                releaseVelocityTracker();
                break;
            case 2:
                if (this.mTouchState != 1) {
                    determineScrollingStart(ev);
                    break;
                } else {
                    float x3 = ev.getX(ev.findPointerIndex(this.mActivePointerId));
                    float deltaX2 = (this.mLastMotionX + this.mLastMotionXRemainder) - x3;
                    this.mTotalMotionX += Math.abs(deltaX2);
                    if (Math.abs(deltaX2) < 1.0f) {
                        awakenScrollBars();
                        break;
                    } else {
                        this.mTouchX += deltaX2;
                        this.mSmoothingTime = ((float) System.nanoTime()) / 1.0E9f;
                        if (!this.mDeferScrollUpdate) {
                            scrollBy((int) deltaX2, 0);
                        } else {
                            invalidate();
                        }
                        this.mLastMotionX = x3;
                        this.mLastMotionXRemainder = deltaX2 - ((float) ((int) deltaX2));
                        break;
                    }
                }
            case MotionEvent.ACTION_CANCEL:
                if (this.mTouchState == 1) {
                    snapToDestination();
                }
                this.mTouchState = 0;
                this.mActivePointerId = -1;
                releaseVelocityTracker();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        return true;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        float vscroll;
        float hscroll;
        if ((event.getSource() & 2) != 0) {
            switch (event.getAction()) {
                case 8:
                    if ((event.getMetaState() & 1) != 0) {
                        vscroll = 0.0f;
                        hscroll = event.getAxisValue(9);
                    } else {
                        vscroll = -event.getAxisValue(9);
                        hscroll = event.getAxisValue(10);
                    }
                    if (!(hscroll == 0.0f && vscroll == 0.0f)) {
                        if (hscroll > 0.0f || vscroll > 0.0f) {
                            scrollRight();
                        } else {
                            scrollLeft();
                        }
                        return true;
                    }
                    break;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = (ev.getAction() & 65280) >> 8;
        if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            float x = ev.getX(newPointerIndex);
            this.mDownMotionX = x;
            this.mLastMotionX = x;
            this.mLastMotionY = ev.getY(newPointerIndex);
            this.mLastMotionXRemainder = 0.0f;
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
        if (this.mTouchState == 0) {
            onWallpaperTap(ev);
        }
    }

    /* access modifiers changed from: protected */
    public void onWallpaperTap(MotionEvent ev) {
    }

    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        int page = indexOfChild(child);
        if (page >= 0 && !isInTouchMode()) {
            snapToPage(page);
        }
    }

    /* access modifiers changed from: protected */
    public void setMinimumWidthOverride(int minimumWidth) {
        this.mMinimumWidth = minimumWidth;
    }

    /* access modifiers changed from: protected */
    public int getChildWidth(int index) {
        return Math.max(this.mMinimumWidth, getChildAt(index).getMeasuredWidth());
    }

    /* access modifiers changed from: protected */
    public int getRelativeChildOffset(int index) {
        return (getMeasuredWidth() - getChildWidth(index)) / 2;
    }

    /* access modifiers changed from: protected */
    public int getChildOffset(int index) {
        if (getChildCount() == 0) {
            return 0;
        }
        int offset = getRelativeChildOffset(0);
        for (int i = 0; i < index; i++) {
            offset += getScaledMeasuredWidth(getChildAt(i)) + this.mPageSpacing;
        }
        return offset;
    }

    /* access modifiers changed from: protected */
    public int getScaledMeasuredWidth(View child) {
        return (int) ((((float) Math.max(this.mMinimumWidth, child.getMeasuredWidth())) * this.mLayoutScale) + 0.5f);
    }

    /* access modifiers changed from: package-private */
    public int getPageNearestToCenterOfScreen() {
        int minDistanceFromScreenCenter = getMeasuredWidth();
        int minDistanceFromScreenCenterIndex = -1;
        int screenCenter = getScrollX() + (getMeasuredWidth() / 2);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            int distanceFromScreenCenter = Math.abs((getChildOffset(i) + (getScaledMeasuredWidth(getChildAt(i)) / 2)) - screenCenter);
            if (distanceFromScreenCenter < minDistanceFromScreenCenter) {
                minDistanceFromScreenCenter = distanceFromScreenCenter;
                minDistanceFromScreenCenterIndex = i;
            }
        }
        return minDistanceFromScreenCenterIndex;
    }

    /* access modifiers changed from: protected */
    public void snapToDestination() {
        snapToPage(getPageNearestToCenterOfScreen(), 550);
    }

    private static class ScrollInterpolator implements Interpolator {
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * t2 * t2 * t2) + 1.0f;
        }
    }

    /* access modifiers changed from: package-private */
    public float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((double) ((float) (((double) (f - 0.5f)) * 0.4712389167638204d)));
    }

    /* access modifiers changed from: protected */
    public void snapToPageWithVelocity(int whichPage, int velocity) {
        int whichPage2 = Math.max(0, Math.min(whichPage, getChildCount() - 1));
        int halfScreenSize = getMeasuredWidth() / 2;
        int delta = (getChildOffset(whichPage2) - getRelativeChildOffset(whichPage2)) - this.mUnboundedScrollX;
        if (Math.abs(velocity) < 250) {
            snapToPage(whichPage2, 550);
            return;
        }
        snapToPage(whichPage2, delta, Math.round(1000.0f * Math.abs((((float) halfScreenSize) + (((float) halfScreenSize) * distanceInfluenceForSnapDuration(Math.min(1.0f, (((float) Math.abs(delta)) * 1.0f) / ((float) (halfScreenSize * 2)))))) / ((float) Math.max(2200, Math.abs(velocity))))) * 4);
    }

    /* access modifiers changed from: protected */
    public void snapToPage(int whichPage) {
        snapToPage(whichPage, 550);
    }

    /* access modifiers changed from: protected */
    public void snapToPage(int whichPage, int duration) {
        int whichPage2 = Math.max(0, Math.min(whichPage, getPageCount() - 1));
        snapToPage(whichPage2, (getChildOffset(whichPage2) - getRelativeChildOffset(whichPage2)) - this.mUnboundedScrollX, duration);
    }

    /* access modifiers changed from: protected */
    public void snapToPage(int whichPage, int delta, int duration) {
        this.mNextPage = whichPage;
        View focusedChild = getFocusedChild();
        if (!(focusedChild == null || whichPage == this.mCurrentPage || focusedChild != getChildAt(this.mCurrentPage))) {
            focusedChild.clearFocus();
        }
        pageBeginMoving();
        awakenScrollBars(duration);
        if (duration == 0) {
            duration = Math.abs(delta);
        }
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
        this.mScroller.startScroll(this.mUnboundedScrollX, 0, delta, 0, duration);
        loadAssociatedPages(this.mNextPage);
        notifyPageSwitchListener();
        invalidate();
    }

    public void scrollLeft() {
        if (this.mScroller.isFinished()) {
            if (this.mCurrentPage > 0) {
                snapToPage(this.mCurrentPage - 1);
            }
        } else if (this.mNextPage > 0) {
            snapToPage(this.mNextPage - 1);
        }
    }

    public void scrollRight() {
        if (this.mScroller.isFinished()) {
            if (this.mCurrentPage < getChildCount() - 1) {
                snapToPage(this.mCurrentPage + 1);
            }
        } else if (this.mNextPage < getChildCount() - 1) {
            snapToPage(this.mNextPage + 1);
        }
    }

    public int getPageForView(View v) {
        if (v != null) {
            ViewParent vp = v.getParent();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (vp == getChildAt(i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean allowLongPress() {
        return this.mAllowLongPress;
    }

    public void setAllowLongPress(boolean allowLongPress) {
        this.mAllowLongPress = allowLongPress;
    }

    public void loadAssociatedPages(int page) {
        int count;
        if (this.mContentIsRefreshable && page < (count = getChildCount())) {
            int lowerPageBound = getAssociatedLowerPageBound(page);
            int upperPageBound = getAssociatedUpperPageBound(page);
            for (int i = 0; i < count; i++) {
                Page layout = (Page) getChildAt(i);
                int childCount = layout.getPageChildCount();
                if (lowerPageBound > i || i > upperPageBound) {
                    if (childCount > 0) {
                        layout.removeAllViewsOnPage();
                    }
                    this.mDirtyPageContent.set(i, true);
                } else if (this.mDirtyPageContent.get(i).booleanValue()) {
                    syncPageItems(i);
                    this.mDirtyPageContent.set(i, false);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public int getAssociatedLowerPageBound(int page) {
        return Math.max(0, page - 1);
    }

    /* access modifiers changed from: protected */
    public int getAssociatedUpperPageBound(int page) {
        return Math.min(page + 1, getChildCount() - 1);
    }

    public void endChoiceMode() {
        if (!isChoiceMode(0)) {
            this.mChoiceMode = 0;
            resetCheckedGrandchildren();
            if (this.mActionMode != null) {
                this.mActionMode.finish();
            }
            this.mActionMode = null;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isChoiceMode(int mode) {
        return this.mChoiceMode == mode;
    }

    /* access modifiers changed from: protected */
    public ArrayList<Checkable> getCheckedGrandchildren() {
        ArrayList<Checkable> checked = new ArrayList<>();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            Page layout = (Page) getChildAt(i);
            int grandChildCount = layout.getPageChildCount();
            for (int j = 0; j < grandChildCount; j++) {
                View v = layout.getChildOnPageAt(j);
                if ((v instanceof Checkable) && ((Checkable) v).isChecked()) {
                    checked.add((Checkable) v);
                }
            }
        }
        return checked;
    }

    /* access modifiers changed from: protected */
    public Checkable getSingleCheckedGrandchild() {
        if (this.mChoiceMode != 2) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                Page layout = (Page) getChildAt(i);
                int grandChildCount = layout.getPageChildCount();
                for (int j = 0; j < grandChildCount; j++) {
                    View v = layout.getChildOnPageAt(j);
                    if ((v instanceof Checkable) && ((Checkable) v).isChecked()) {
                        return (Checkable) v;
                    }
                }
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void resetCheckedGrandchildren() {
        ArrayList<Checkable> checked = getCheckedGrandchildren();
        for (int i = 0; i < checked.size(); i++) {
            checked.get(i).setChecked(false);
        }
    }

    public void setRestorePage(int restorePage) {
        this.mRestorePage = restorePage;
    }

    /* access modifiers changed from: protected */
    public void postInvalidatePageData(final boolean clearViews) {
        post(new Runnable() {
            public void run() {
                if (clearViews) {
                    PagedView.this.removeAllViews();
                }
                PagedView.this.invalidatePageData();
            }
        });
    }

    /* access modifiers changed from: protected */
    public void invalidatePageData() {
        if (this.mContentIsRefreshable) {
            syncPages();
            int count = getChildCount();
            this.mDirtyPageContent.clear();
            for (int i = 0; i < count; i++) {
                this.mDirtyPageContent.add(true);
            }
            if (this.mRestorePage > -1) {
                if (this.mRestorePage >= count) {
                    this.mCurrentPage = count - 1;
                } else {
                    this.mCurrentPage = this.mRestorePage;
                }
                this.mRestorePage = -1;
            }
            loadAssociatedPages(this.mCurrentPage);
            this.mDirtyPageAlpha = true;
            updateAdjacentPagesAlpha();
            requestLayout();
        }
    }
}
