package com.android.launcher2;
import com.launcher3h.R;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class LauncherAppWidgetHostView extends AppWidgetHostView {
    /* access modifiers changed from: private */
    public boolean mHasPerformedLongPress;
    private LayoutInflater mInflater;
    private VisibilityChangedListener mOnVisibilityChangedListener;
    private CheckForLongPress mPendingCheckForLongPress;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
    }

    /* access modifiers changed from: protected */
    public View getErrorView() {
        return this.mInflater.inflate(R.layout.launcher3h_appwidget_error, this, false);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mHasPerformedLongPress) {
            this.mHasPerformedLongPress = false;
            return true;
        }
        switch (ev.getAction()) {
            case 0:
                postCheckForLongClick();
                break;
            case 1:
            case MotionEvent.ACTION_CANCEL:
                this.mHasPerformedLongPress = false;
                if (this.mPendingCheckForLongPress != null) {
                    removeCallbacks(this.mPendingCheckForLongPress);
                    break;
                }
                break;
        }
        return false;
    }

    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        CheckForLongPress() {
        }

        public void run() {
            if (LauncherAppWidgetHostView.this.getParent() != null && LauncherAppWidgetHostView.this.hasWindowFocus() && this.mOriginalWindowAttachCount == LauncherAppWidgetHostView.this.getWindowAttachCount() && !LauncherAppWidgetHostView.this.mHasPerformedLongPress && LauncherAppWidgetHostView.this.performLongClick()) {
                boolean unused = LauncherAppWidgetHostView.this.mHasPerformedLongPress = true;
            }
        }

        public void rememberWindowAttachCount() {
            this.mOriginalWindowAttachCount = LauncherAppWidgetHostView.this.getWindowAttachCount();
        }
    }

    private void postCheckForLongClick() {
        this.mHasPerformedLongPress = false;
        if (this.mPendingCheckForLongPress == null) {
            this.mPendingCheckForLongPress = new CheckForLongPress();
        }
        this.mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(this.mPendingCheckForLongPress, (long) ViewConfiguration.getLongPressTimeout());
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        this.mHasPerformedLongPress = false;
        if (this.mPendingCheckForLongPress != null) {
            removeCallbacks(this.mPendingCheckForLongPress);
        }
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        if (this.mOnVisibilityChangedListener != null) {
            this.mOnVisibilityChangedListener.receiveVisibilityChangedMessage(this);
        }
        super.onVisibilityChanged(changedView, visibility);
    }
}
