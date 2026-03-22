package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import java.util.ArrayList;

public class AllAppsTabbed extends TabHost implements AllAppsView, LauncherTransitionable {
    /* access modifiers changed from: private */
    public AllAppsPagedView mAllApps;
    private AllAppsBackground mBackground;
    private Context mContext;
    private boolean mFirstLayout = true;
    private final LayoutInflater mInflater;
    private Launcher mLauncher;

    public AllAppsTabbed(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        setup();
        try {
            this.mAllApps = (AllAppsPagedView) findViewById(R.id.all_apps_paged_view);
            if (this.mAllApps == null) {
                throw new Resources.NotFoundException();
            }
            this.mBackground = (AllAppsBackground) findViewById(R.id.all_apps_background);
            if (this.mBackground == null) {
                throw new Resources.NotFoundException();
            }
            TabHost.TabContentFactory contentFactory = new TabHost.TabContentFactory() {
                public View createTabContent(String tag) {
                    return AllAppsTabbed.this.mAllApps;
                }
            };
            TabWidget tabWidget = (TabWidget) findViewById(16908307);
            TextView tabView = (TextView) this.mInflater.inflate(R.layout.launcher3h_tab_widget_indicator, tabWidget, false);
            tabView.setText(this.mContext.getString(R.string.all_apps_tab_all));
            addTab(newTabSpec("ALL").setIndicator(tabView).setContent(contentFactory));
            TextView tabView2 = (TextView) this.mInflater.inflate(R.layout.launcher3h_tab_widget_indicator, tabWidget, false);
            tabView2.setText(this.mContext.getString(R.string.all_apps_tab_downloaded));
            addTab(newTabSpec("DOWNLOADED").setIndicator(tabView2).setContent(contentFactory));
            setOnTabChangedListener(new TabHost.OnTabChangeListener() {
                public void onTabChanged(String tabId) {
                    final int duration = AllAppsTabbed.this.getResources().getInteger(R.integer.config_tabTransitionTime);
                    float alpha = AllAppsTabbed.this.mAllApps.getAlpha();
                    ObjectAnimator duration2 = ObjectAnimator.ofFloat(AllAppsTabbed.this.mAllApps, "alpha", new float[]{alpha, 0.0f}).setDuration((long) duration);
                    duration2.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            String tag = AllAppsTabbed.this.getCurrentTabTag();
                            if (tag == "ALL") {
                                AllAppsTabbed.this.mAllApps.setAppFilter(-1);
                            } else if (tag == "DOWNLOADED") {
                                AllAppsTabbed.this.mAllApps.setAppFilter(1);
                            }
                            float alpha = AllAppsTabbed.this.mAllApps.getAlpha();
                            ObjectAnimator.ofFloat(AllAppsTabbed.this.mAllApps, "alpha", new float[]{alpha, 1.0f}).setDuration((long) duration).start();
                        }
                    });
                    duration2.start();
                }
            });
            setVisibility(4);
        } catch (Resources.NotFoundException e) {
            Resources.NotFoundException notFoundException = e;
            Log.e("Launcher.AllAppsTabbed", "Can't find necessary layout elements for AllAppsTabbed");
        }
    }

    public void setLauncher(Launcher launcher) {
        this.mAllApps.setLauncher(launcher);
        this.mLauncher = launcher;
    }

    public void setDragController(DragController dragger) {
        this.mAllApps.setDragController(dragger);
    }

    public void zoom(float zoom, boolean animate) {
        setVisibility(zoom == 0.0f ? 8 : 0);
        this.mAllApps.zoom(zoom, animate);
    }

    public void setVisibility(int visibility) {
        boolean isVisible;
        if (visibility == 8 && this.mFirstLayout) {
            visibility = 4;
        }
        if (visibility == 0) {
            isVisible = true;
        } else {
            isVisible = false;
        }
        super.setVisibility(visibility);
        this.mAllApps.zoom(isVisible ? 1.0f : 0.0f, false);
    }

    public boolean isVisible() {
        return this.mAllApps.isVisible();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mFirstLayout) {
            this.mFirstLayout = false;
        }
        int pageWidth = this.mAllApps.getPageContentWidth();
        TabWidget tabWidget = (TabWidget) findViewById(16908307);
        View allAppsTabBar = findViewById(R.id.all_apps_tab_bar);
        if (allAppsTabBar == null) {
            throw new Resources.NotFoundException();
        }
        int tabWidgetPadding = 0;
        if (tabWidget.getChildCount() > 0) {
            tabWidgetPadding = 0 + (tabWidget.getChildAt(0).getPaddingLeft() * 2);
        }
        int newWidth = pageWidth > 0
                ? Math.min(getMeasuredWidth(), pageWidth + tabWidgetPadding)
                : getMeasuredWidth();
        if (newWidth != allAppsTabBar.getLayoutParams().width) {
            allAppsTabBar.getLayoutParams().width = newWidth;
            post(new Runnable() {
                public void run() {
                    AllAppsTabbed.this.requestLayout();
                }
            });
        }
        super.onLayout(changed, l, t, r, b);

        // TabHost is a FrameLayout — it stacks tabcontent at top=0.
        // Manually push the tabcontent down by the tab bar height so it doesn't overlap.
        int tabBarHeight = allAppsTabBar.getMeasuredHeight();
        if (tabBarHeight > 0) {
            View tabContent = findViewById(android.R.id.tabcontent);
            if (tabContent != null) {
                tabContent.layout(tabContent.getLeft(), tabBarHeight,
                        tabContent.getRight(), tabContent.getBottom());
            }
        }
    }

    public boolean isAnimating() {
        return getAnimation() != null;
    }

    public void onLauncherTransitionStart(Animator animation) {
        if (animation != null) {
            setLayerType(2, (Paint) null);
            if (this.mLauncher.getWorkspace().getBackgroundAlpha() == 0.0f) {
                this.mLauncher.getWorkspace().disableBackground();
                this.mBackground.setVisibility(0);
            }
            if (!this.mFirstLayout) {
                buildLayer();
            }
        }
    }

    public void onLauncherTransitionEnd(Animator animation) {
        if (animation != null) {
            setLayerType(0, (Paint) null);
        }
        if (this.mBackground.getVisibility() != 8) {
            this.mLauncher.getWorkspace().enableBackground();
            this.mBackground.setVisibility(8);
        }
        this.mAllApps.allowHardwareLayerCreation();
    }

    public void setApps(ArrayList<ApplicationInfo> list) {
        this.mAllApps.setApps(list);
    }

    public void addApps(ArrayList<ApplicationInfo> list) {
        this.mAllApps.addApps(list);
    }

    public void removeApps(ArrayList<ApplicationInfo> list) {
        this.mAllApps.removeApps(list);
    }

    public void updateApps(ArrayList<ApplicationInfo> list) {
        this.mAllApps.updateApps(list);
    }

    public void dumpState() {
        this.mAllApps.dumpState();
    }

    public void surrender() {
        this.mAllApps.surrender();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getY() > ((float) this.mAllApps.getBottom())) {
            return false;
        }
        return true;
    }
}
