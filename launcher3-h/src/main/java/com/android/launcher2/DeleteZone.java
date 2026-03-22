package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;

public class DeleteZone extends IconDropTarget {
    private DragController mDragController;
    private int mDragTextColor;
    private Animation mHandleInAnimation;
    private Animation mHandleOutAnimation;
    private AnimationSet mInAnimation;
    private int mOrientation;
    private AnimationSet mOutAnimation;
    private final Rect mRegion;
    private final RectF mRegionF;
    private int mTextColor;
    private TransitionDrawable mTransition;

    public DeleteZone(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteZone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mRegionF = new RectF();
        this.mRegion = new Rect();
        this.mHoverPaint.setColorFilter(new PorterDuffColorFilter(context.getResources().getColor(R.color.delete_color_filter), PorterDuff.Mode.SRC_ATOP));
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DeleteZone, defStyle, 0);
        this.mOrientation = a.getInt(R.styleable.DeleteZone_deleteZoneDirection, 1);
        a.recycle();
        if (LauncherApplication.isScreenXLarge()) {
            int tb = getResources().getDimensionPixelSize(R.dimen.delete_zone_vertical_drag_padding);
            int lr = getResources().getDimensionPixelSize(R.dimen.delete_zone_horizontal_drag_padding);
            setDragPadding(tb, lr, tb, lr);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mTransition = (TransitionDrawable) getCompoundDrawables()[0];
        if (LauncherApplication.isScreenXLarge()) {
            this.mTransition.setCrossFadeEnabled(false);
        }
        Resources r = getResources();
        this.mTextColor = r.getColor(R.color.workspace_all_apps_and_delete_zone_text_color);
        this.mDragTextColor = r.getColor(R.color.workspace_delete_zone_drag_text_color);
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        return true;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (this.mDragAndDropEnabled) {
            ItemInfo item = (ItemInfo) dragInfo;
            if ((item instanceof ApplicationInfo) && LauncherApplication.isScreenXLarge()) {
                this.mLauncher.startApplicationUninstallActivity((ApplicationInfo) item);
            }
            if (item.container != -1) {
                if (item.container == -100) {
                    if (item instanceof LauncherAppWidgetInfo) {
                        this.mLauncher.removeAppWidget((LauncherAppWidgetInfo) item);
                    }
                } else if (source instanceof UserFolder) {
                    ((UserFolderInfo) ((UserFolder) source).getInfo()).remove((ShortcutInfo) item);
                }
                if (item instanceof UserFolderInfo) {
                    UserFolderInfo userFolderInfo = (UserFolderInfo) item;
                    LauncherModel.deleteUserFolderContentsFromDatabase(this.mLauncher, userFolderInfo);
                    this.mLauncher.removeFolder(userFolderInfo);
                } else if (item instanceof LauncherAppWidgetInfo) {
                    final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
                    final LauncherAppWidgetHost appWidgetHost = this.mLauncher.getAppWidgetHost();
                    if (appWidgetHost != null) {
                        new Thread("deleteAppWidgetId") {
                            public void run() {
                                appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                            }
                        }.start();
                    }
                }
                LauncherModel.deleteItemFromDatabase(this.mLauncher, item);
            }
        }
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (this.mDragAndDropEnabled) {
            this.mTransition.reverseTransition(getTransitionAnimationDuration());
            setTextColor(this.mDragTextColor);
            super.onDragEnter(source, x, y, xOffset, yOffset, dragView, dragInfo);
        }
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (this.mDragAndDropEnabled) {
            this.mTransition.reverseTransition(getTransitionAnimationDuration());
            setTextColor(this.mTextColor);
            super.onDragExit(source, x, y, xOffset, yOffset, dragView, dragInfo);
        }
    }

    public void onDragStart(DragSource source, Object info, int dragAction) {
        if (((ItemInfo) info) != null && this.mDragAndDropEnabled) {
            this.mActive = true;
            getHitRect(this.mRegion);
            this.mRegionF.set(this.mRegion);
            if (LauncherApplication.isScreenXLarge()) {
                this.mRegionF.top = (float) this.mLauncher.getWorkspace().getTop();
                this.mRegionF.right = (float) this.mLauncher.getWorkspace().getRight();
            }
            this.mDragController.setDeleteRegion(this.mRegionF);
            this.mTransition.resetTransition();
            createAnimations();
            startAnimation(this.mInAnimation);
            if (this.mOverlappingViews != null) {
                for (View view : this.mOverlappingViews) {
                    view.startAnimation(this.mHandleOutAnimation);
                }
            }
            setVisibility(0);
        }
    }

    public void onDragEnd() {
        if (this.mActive && this.mDragAndDropEnabled) {
            this.mActive = false;
            this.mDragController.setDeleteRegion((RectF) null);
            if (this.mOutAnimation != null) {
                startAnimation(this.mOutAnimation);
            }
            if (!(this.mHandleInAnimation == null || this.mOverlappingViews == null)) {
                for (View view : this.mOverlappingViews) {
                    view.startAnimation(this.mHandleInAnimation);
                }
            }
            setVisibility(8);
        }
    }

    private void createAnimations() {
        int duration = getAnimationDuration();
        if (this.mHandleInAnimation == null) {
            this.mHandleInAnimation = new AlphaAnimation(0.0f, 1.0f);
            this.mHandleInAnimation.setDuration((long) duration);
        }
        if (this.mInAnimation == null) {
            this.mInAnimation = new FastAnimationSet();
            if (!LauncherApplication.isScreenXLarge()) {
                AnimationSet animationSet = this.mInAnimation;
                animationSet.setInterpolator(new AccelerateInterpolator());
                animationSet.addAnimation(new AlphaAnimation(0.0f, 1.0f));
                if (this.mOrientation == 1) {
                    animationSet.addAnimation(new TranslateAnimation(0, 0.0f, 0, 0.0f, 1, 1.0f, 1, 0.0f));
                } else {
                    animationSet.addAnimation(new TranslateAnimation(1, 1.0f, 1, 0.0f, 0, 0.0f, 0, 0.0f));
                }
                animationSet.setDuration((long) duration);
            } else {
                this.mInAnimation.addAnimation(this.mHandleInAnimation);
            }
        }
        if (this.mHandleOutAnimation == null) {
            this.mHandleOutAnimation = new AlphaAnimation(1.0f, 0.0f);
            this.mHandleOutAnimation.setFillAfter(true);
            this.mHandleOutAnimation.setDuration((long) duration);
        }
        if (this.mOutAnimation == null) {
            this.mOutAnimation = new FastAnimationSet();
            if (!LauncherApplication.isScreenXLarge()) {
                AnimationSet animationSet2 = this.mOutAnimation;
                animationSet2.setInterpolator(new AccelerateInterpolator());
                animationSet2.addAnimation(new AlphaAnimation(1.0f, 0.0f));
                if (this.mOrientation == 1) {
                    animationSet2.addAnimation(new FastTranslateAnimation(0, 0.0f, 0, 0.0f, 1, 0.0f, 1, 1.0f));
                } else {
                    animationSet2.addAnimation(new FastTranslateAnimation(1, 0.0f, 1, 1.0f, 0, 0.0f, 0, 0.0f));
                }
                animationSet2.setDuration((long) duration);
                return;
            }
            this.mOutAnimation.addAnimation(this.mHandleOutAnimation);
        }
    }

    /* access modifiers changed from: package-private */
    public void setDragController(DragController dragController) {
        this.mDragController = dragController;
    }

    private int getTransitionAnimationDuration() {
        return LauncherApplication.isScreenXLarge() ? 150 : 250;
    }

    private int getAnimationDuration() {
        return LauncherApplication.isScreenXLarge() ? 200 : 200;
    }

    private static class FastTranslateAnimation extends TranslateAnimation {
        public FastTranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue, int fromYType, float fromYValue, int toYType, float toYValue) {
            super(fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue, toYType, toYValue);
        }

        public boolean willChangeTransformationMatrix() {
            return true;
        }

        public boolean willChangeBounds() {
            return false;
        }
    }

    private static class FastAnimationSet extends AnimationSet {
        FastAnimationSet() {
            super(false);
        }

        public boolean willChangeTransformationMatrix() {
            return true;
        }

        public boolean willChangeBounds() {
            return false;
        }
    }
}
