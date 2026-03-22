package com.android.launcher2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class PagedViewExtendedLayout extends LinearLayout implements Page {
    float mChildrenAlpha;
    private boolean mHasFixedWidth;

    public PagedViewExtendedLayout(Context context) {
        this(context, (AttributeSet) null);
    }

    public PagedViewExtendedLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewExtendedLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mChildrenAlpha = 1.0f;
    }

    public void setHasFixedWidth(boolean hasFixedWidth) {
        this.mHasFixedWidth = hasFixedWidth;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mHasFixedWidth) {
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(getSuggestedMinimumWidth(), 1073741824);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!super.onTouchEvent(event)) {
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean onSetAlpha(int alpha) {
        return true;
    }

    public void setAlpha(float alpha) {
        this.mChildrenAlpha = alpha;
        setChildrenAlpha(alpha);
        super.setAlpha(alpha);
    }

    private void setChildrenAlpha(float alpha) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).setAlpha(alpha);
        }
    }

    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        child.setAlpha(this.mChildrenAlpha);
    }

    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
        child.setAlpha(this.mChildrenAlpha);
    }

    public void addView(View child, int index) {
        super.addView(child, index);
        child.setAlpha(this.mChildrenAlpha);
    }

    public void addView(View child) {
        super.addView(child);
        child.setAlpha(this.mChildrenAlpha);
    }

    public void addView(View child, int width, int height) {
        super.addView(child, width, height);
        child.setAlpha(this.mChildrenAlpha);
    }

    public void removeAllViewsOnPage() {
        removeAllViews();
    }

    public int getPageChildCount() {
        return getChildCount();
    }

    public View getChildOnPageAt(int i) {
        return getChildAt(i);
    }

    public int indexOfChildOnPage(View v) {
        return indexOfChild(v);
    }
}
