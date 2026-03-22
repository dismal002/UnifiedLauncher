package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.AttributeSet;
import android.view.View;

public class ApplicationInfoDropTarget extends IconDropTarget {
    /* access modifiers changed from: private */
    public AnimatorSet mFadeAnimator;

    public ApplicationInfoDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ApplicationInfoDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHoverPaint.setColorFilter(new PorterDuffColorFilter(getContext().getResources().getColor(R.color.app_info_filter), PorterDuff.Mode.SRC_ATOP));
        if (LauncherApplication.isScreenXLarge()) {
            int tb = getResources().getDimensionPixelSize(R.dimen.delete_zone_vertical_drag_padding);
            setDragPadding(tb, getResources().getDimensionPixelSize(R.dimen.delete_zone_horizontal_drag_padding), tb, 0);
        }
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (getVisibility() != 0) {
            return false;
        }
        ComponentName componentName = null;
        if (dragInfo instanceof ApplicationInfo) {
            componentName = ((ApplicationInfo) dragInfo).componentName;
        } else if (dragInfo instanceof ShortcutInfo) {
            componentName = ((ShortcutInfo) dragInfo).intent.getComponent();
        }
        this.mLauncher.startApplicationDetailsActivity(componentName);
        return false;
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (this.mDragAndDropEnabled) {
            dragView.setPaint(this.mHoverPaint);
        }
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (this.mDragAndDropEnabled) {
            dragView.setPaint((Paint) null);
        }
    }

    public void onDragStart(DragSource source, Object info, int dragAction) {
        boolean z;
        if (info != null && this.mDragAndDropEnabled) {
            if (((ItemInfo) info).itemType == 0) {
                z = true;
            } else {
                z = false;
            }
            this.mActive = z;
            if (this.mActive) {
                if (this.mFadeAnimator != null) {
                    this.mFadeAnimator.cancel();
                }
                this.mFadeAnimator = new AnimatorSet();
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, "alpha", new float[]{0.0f, 1.0f});
                ofFloat.setDuration(200);
                this.mFadeAnimator.play(ofFloat);
                setVisibility(0);
                if (this.mOverlappingViews != null) {
                    for (View view : this.mOverlappingViews) {
                        ObjectAnimator oa = ObjectAnimator.ofFloat(view, "alpha", new float[]{0.0f});
                        oa.setDuration(100);
                        this.mFadeAnimator.play(oa);
                    }
                    this.mFadeAnimator.addListener(new Animator.AnimatorListener() {
                        public void onAnimationStart(Animator animation) {
                        }

                        public void onAnimationRepeat(Animator animation) {
                        }

                        public void onAnimationEnd(Animator animation) {
                            onEndOrCancel();
                        }

                        public void onAnimationCancel(Animator animation) {
                            onEndOrCancel();
                        }

                        private void onEndOrCancel() {
                            for (View view : ApplicationInfoDropTarget.this.mOverlappingViews) {
                                view.setVisibility(4);
                            }
                            AnimatorSet unused = ApplicationInfoDropTarget.this.mFadeAnimator = null;
                        }
                    });
                }
                this.mFadeAnimator.start();
            }
        }
    }

    public void onDragEnd() {
        if (this.mDragAndDropEnabled) {
            if (this.mActive) {
                this.mActive = false;
            }
            if (this.mFadeAnimator != null) {
                this.mFadeAnimator.cancel();
            }
            this.mFadeAnimator = new AnimatorSet();
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, "alpha", new float[]{0.0f});
            ofFloat.setDuration(100);
            this.mFadeAnimator.addListener(new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    onEndOrCancel();
                }

                public void onAnimationCancel(Animator animation) {
                    onEndOrCancel();
                }

                private void onEndOrCancel() {
                    ApplicationInfoDropTarget.this.setVisibility(8);
                    AnimatorSet unused = ApplicationInfoDropTarget.this.mFadeAnimator = null;
                }
            });
            this.mFadeAnimator.play(ofFloat);
            if (this.mOverlappingViews != null) {
                for (View view : this.mOverlappingViews) {
                    ObjectAnimator oa = ObjectAnimator.ofFloat(view, "alpha", new float[]{1.0f});
                    oa.setDuration(200);
                    this.mFadeAnimator.play(oa);
                    view.setVisibility(0);
                }
            }
            this.mFadeAnimator.start();
        }
    }
}
