package com.android.launcher2;
import com.launcher3h.R;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class AllApps2D extends RelativeLayout implements View.OnKeyListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, AllAppsView, DragSource {
    private ArrayList<ApplicationInfo> mAllAppsList;
    private AppsAdapter mAppsAdapter;
    private AppType mCurrentFilter;
    private DragController mDragController;
    private GridView mGrid;
    /* access modifiers changed from: private */
    public Launcher mLauncher;
    private ArrayList<ApplicationInfo> mVisibleAppsList;
    private float mZoom;

    public enum AppType {
        APP,
        GAME,
        DOWNLOADED,
        ALL
    }

    public static class HomeButton extends ImageButton {
        public HomeButton(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public View focusSearch(int direction) {
            if (direction == 33) {
                return super.focusSearch(direction);
            }
            return null;
        }
    }

    public class AppsAdapter extends ArrayAdapter<ApplicationInfo> {
        private final LayoutInflater mInflater;

        public AppsAdapter(Context context, ArrayList<ApplicationInfo> apps) {
            super(context, 0, apps);
            this.mInflater = LayoutInflater.from(context);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ApplicationInfo info = (ApplicationInfo) getItem(position);
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.application_boxed, parent, false);
            }
            TextView textView = (TextView) convertView;
            info.iconBitmap.setDensity(0);
            textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new BitmapDrawable(info.iconBitmap), (Drawable) null, (Drawable) null);
            textView.setText(info.title);
            return convertView;
        }
    }

    public AllApps2D(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAllAppsList = new ArrayList<>();
        this.mVisibleAppsList = new ArrayList<>();
        this.mCurrentFilter = AppType.ALL;
        setVisibility(8);
        setSoundEffectsEnabled(false);
        this.mAppsAdapter = new AppsAdapter(getContext(), this.mVisibleAppsList);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        try {
            this.mGrid = (GridView) findViewWithTag("all_apps_2d_grid");
            if (this.mGrid == null) {
                throw new Resources.NotFoundException();
            }
            this.mGrid.setOnItemClickListener(this);
            this.mGrid.setOnItemLongClickListener(this);
            ImageButton homeButton = (ImageButton) findViewWithTag("all_apps_2d_home");
            if (homeButton != null) {
                homeButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        AllApps2D.this.mLauncher.closeAllApps(true);
                    }
                });
            }
            setOnKeyListener(this);
        } catch (Resources.NotFoundException e) {
            Resources.NotFoundException notFoundException = e;
            Log.e("Launcher.AllApps2D", "Can't find necessary layout elements for AllApps2D");
        }
    }

    public AllApps2D(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs);
    }

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (!isVisible()) {
            return false;
        }
        switch (keyCode) {
            case 4:
                this.mLauncher.closeAllApps(true);
                return true;
            default:
                return false;
        }
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
        this.mLauncher.startActivitySafely(app.intent, app);
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!view.isInTouchMode()) {
            return false;
        }
        this.mDragController.startDrag(view, this, new ApplicationInfo((ApplicationInfo) parent.getItemAtPosition(position)), DragController.DRAG_ACTION_COPY);
        this.mLauncher.closeAllApps(true);
        return true;
    }

    /* access modifiers changed from: protected */
    public void onFocusChanged(boolean gainFocus, int direction, Rect prev) {
        if (gainFocus) {
            this.mGrid.requestFocus();
        }
    }

    public void setDragController(DragController dragger) {
        this.mDragController = dragger;
    }

    public void onDragViewVisible() {
    }

    public void onDropCompleted(View target, Object dragInfo, boolean success) {
    }

    public void zoom(float zoom, boolean animate) {
        cancelLongPress();
        this.mZoom = zoom;
        if (isVisible()) {
            getParent().bringChildToFront(this);
            setVisibility(0);
            this.mGrid.setAdapter(this.mAppsAdapter);
            if (animate) {
                startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.all_apps_2d_fade_in));
            } else {
                onAnimationEnd();
            }
        } else if (animate) {
            startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.all_apps_2d_fade_out));
        } else {
            onAnimationEnd();
        }
    }

    /* access modifiers changed from: protected */
    public void onAnimationEnd() {
        if (!isVisible()) {
            setVisibility(8);
            this.mGrid.setAdapter((ListAdapter) null);
            this.mZoom = 0.0f;
        } else {
            this.mZoom = 1.0f;
        }
        this.mLauncher.zoomed(this.mZoom);
    }

    public boolean isVisible() {
        return this.mZoom > 0.001f;
    }

    public boolean isAnimating() {
        return getAnimation() != null;
    }

    public void setApps(ArrayList<ApplicationInfo> list) {
        this.mAllAppsList.clear();
        addApps(list);
        filterApps(this.mCurrentFilter);
    }

    public void addApps(ArrayList<ApplicationInfo> list) {
        int N = list.size();
        for (int i = 0; i < N; i++) {
            ApplicationInfo item = list.get(i);
            int index = Collections.binarySearch(this.mAllAppsList, item, LauncherModel.APP_NAME_COMPARATOR);
            if (index < 0) {
                index = -(index + 1);
            }
            this.mAllAppsList.add(index, item);
        }
        filterApps(this.mCurrentFilter);
    }

    public void removeApps(ArrayList<ApplicationInfo> list) {
        int N = list.size();
        for (int i = 0; i < N; i++) {
            ApplicationInfo item = list.get(i);
            int index = findAppByComponent(this.mAllAppsList, item);
            if (index >= 0) {
                this.mAllAppsList.remove(index);
            } else {
                Log.w("Launcher.AllApps2D", "couldn't find a match for item \"" + item + "\"");
            }
        }
        filterApps(this.mCurrentFilter);
    }

    public void updateApps(ArrayList<ApplicationInfo> list) {
        removeApps(list);
        addApps(list);
    }

    public void filterApps(AppType appType) {
        this.mCurrentFilter = appType;
        this.mAppsAdapter.setNotifyOnChange(false);
        this.mVisibleAppsList.clear();
        if (appType == AppType.ALL) {
            this.mVisibleAppsList.addAll(this.mAllAppsList);
        } else if (appType == AppType.DOWNLOADED) {
            Iterator<ApplicationInfo> it = this.mAllAppsList.iterator();
            while (it.hasNext()) {
                ApplicationInfo info = it.next();
                if ((info.flags & 1) != 0) {
                    this.mVisibleAppsList.add(info);
                }
            }
        }
        this.mAppsAdapter.notifyDataSetChanged();
    }

    private static int findAppByComponent(ArrayList<ApplicationInfo> list, ApplicationInfo item) {
        ComponentName component = item.intent.getComponent();
        int N = list.size();
        for (int i = 0; i < N; i++) {
            if (list.get(i).intent.getComponent().equals(component)) {
                return i;
            }
        }
        return -1;
    }

    public void dumpState() {
        ApplicationInfo.dumpApplicationInfoList("Launcher.AllApps2D", "mAllAppsList", this.mAllAppsList);
    }

    public void surrender() {
    }
}
