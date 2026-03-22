package com.android.launcher2;
import com.launcher3h.R;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Checkable;
import android.widget.TextView;
import com.android.launcher2.PagedViewCellLayout;
import com.android.launcher2.PagedViewIconCache;
import com.android.launcher2.Workspace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

public class AllAppsPagedView extends PagedViewWithDraggableItems implements View.OnClickListener, AllAppsView, DragSource, DropTarget {
    private boolean mAllowHardwareLayerCreation;
    private int mAppFilter;
    private ArrayList<ApplicationInfo> mApps;
    private DragController mDragController;
    private ArrayList<ApplicationInfo> mFilteredApps;
    private final LayoutInflater mInflater;
    private int mLastMeasureHeight;
    private int mLastMeasureWidth;
    /* access modifiers changed from: private */
    public Launcher mLauncher;
    private int mMaxCellCountY;
    private int mPageContentWidth;
    private boolean mWaitingToDetermineRowsAndColumns;
    private boolean mWaitingToInitPages;
    private float mZoom;

    public AllAppsPagedView(Context context) {
        this(context, (AttributeSet) null);
    }

    public AllAppsPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAppFilter = -1;
        this.mLastMeasureWidth = -1;
        this.mLastMeasureHeight = -1;
        this.mWaitingToInitPages = true;
        this.mWaitingToDetermineRowsAndColumns = true;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedView, defStyle, 0);
        this.mInflater = LayoutInflater.from(context);
        this.mApps = new ArrayList<>();
        this.mFilteredApps = new ArrayList<>();
        a.recycle();
        setSoundEffectsEnabled(false);
        Resources r = context.getResources();
        setDragSlopeThreshold(((float) r.getInteger(R.integer.config_allAppsDrawerDragSlopeThreshold)) / 100.0f);
        PagedViewCellLayout layout = new PagedViewCellLayout(getContext());
        setupPage(layout);
        this.mPageContentWidth = layout.getContentWidth();
        this.mMaxCellCountY = r.getInteger(R.integer.all_apps_view_maxCellCountY);
    }

    /* access modifiers changed from: protected */
    public void init() {
        super.init();
        this.mCenterPagesVertically = false;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if (!(this.mLastMeasureWidth == width && this.mLastMeasureHeight == height)) {
            PagedViewCellLayout layout = new PagedViewCellLayout(getContext());
            setupPage(layout);
            this.mPageContentWidth = layout.getContentWidth();
            this.mCellCountX = determineCellCountX(width, layout);
            this.mCellCountY = determineCellCountY(height, layout);
            this.mLastMeasureWidth = width;
            this.mLastMeasureHeight = height;
            postInvalidatePageData(true);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mWaitingToDetermineRowsAndColumns) {
            this.mWaitingToDetermineRowsAndColumns = false;
            postInvalidatePageData(false);
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    private int determineCellCountX(int availableWidth, PagedViewCellLayout layout) {
        int cellWidth = layout.getCellWidth();
        int availableWidth2 = (availableWidth - ((this.mPageLayoutPaddingLeft * 2) + (this.mPageLayoutPaddingRight * 2))) - cellWidth;
        int cellCountX = (availableWidth2 / (this.mPageLayoutWidthGap + cellWidth)) + 1;
        int availableWidth3 = availableWidth2 % (this.mPageLayoutWidthGap + cellWidth);
        int minLeftoverWidth = (int) (((float) cellWidth) * 0.6f);
        if (cellCountX <= 4) {
            int missingWidth = minLeftoverWidth - availableWidth3;
            if (missingWidth <= 0) {
                return cellCountX;
            }
            this.mPageLayoutWidthGap = (int) (((double) this.mPageLayoutWidthGap) - Math.ceil((double) ((((float) missingWidth) * 1.0f) / ((float) Math.max(1, cellCountX - 1)))));
            return cellCountX;
        } else if (cellCountX >= 8) {
            return (int) (((float) cellCountX) * 0.9f);
        } else {
            if (availableWidth3 < minLeftoverWidth) {
                return cellCountX - 1;
            }
            return cellCountX;
        }
    }

    private int determineCellCountY(int availableHeight, PagedViewCellLayout layout) {
        int cellHeight = layout.getCellHeight();
        int screenHeight = this.mLauncher.getResources().getDisplayMetrics().heightPixels;
        int availableHeight2 = (int) (((float) ((availableHeight - (this.mPageLayoutPaddingTop + this.mPageLayoutPaddingBottom)) - cellHeight)) - (((float) screenHeight) * (((float) getContext().getResources().getInteger(R.integer.config_allAppsZoomScaleFactor)) / 100.0f)));
        if (availableHeight2 > 0) {
            return Math.min(this.mMaxCellCountY, (availableHeight2 / (this.mPageLayoutHeightGap + cellHeight)) + 1);
        }
        return 0;
    }

    /* access modifiers changed from: package-private */
    public int getCellCountX() {
        return this.mCellCountX;
    }

    /* access modifiers changed from: package-private */
    public int getCellCountY() {
        return this.mCellCountY;
    }

    /* access modifiers changed from: package-private */
    public void allowHardwareLayerCreation() {
        if (!this.mAllowHardwareLayerCreation) {
            this.mAllowHardwareLayerCreation = true;
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((PagedViewCellLayout) getChildAt(i)).allowHardwareLayerCreation();
            }
        }
    }

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
        this.mLauncher.setAllAppsPagedView(this);
    }

    public void setDragController(DragController dragger) {
        this.mDragController = dragger;
    }

    public void setAppFilter(int filterType) {
        this.mAppFilter = filterType;
        if (this.mApps != null) {
            this.mFilteredApps = rebuildFilteredApps(this.mApps);
            setCurrentPage(0);
            invalidatePageData();
        }
    }

    public void zoom(float zoom, boolean animate) {
        this.mZoom = zoom;
        cancelLongPress();
        if (isVisible()) {
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
            this.mZoom = 0.0f;
            endChoiceMode();
        } else {
            this.mZoom = 1.0f;
        }
        if (this.mLauncher != null) {
            this.mLauncher.zoomed(this.mZoom);
        }
    }

    private int getChildIndexForGrandChild(View v) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (((Page) getChildAt(i)).indexOfChildOnPage(v) > -1) {
                return i;
            }
        }
        return -1;
    }

    public void onClick(View v) {
        boolean z;
        if ((v instanceof Checkable) && !isChoiceMode(0)) {
            Checkable c = (Checkable) v;
            if (isChoiceMode(1)) {
                boolean wasChecked = c.isChecked();
                resetCheckedGrandchildren();
                if (!wasChecked) {
                    z = true;
                } else {
                    z = false;
                }
                c.setChecked(z);
            } else {
                c.toggle();
            }
            if (getCheckedGrandchildren().size() == 0) {
                endChoiceMode();
            }
        } else if (getChildIndexForGrandChild(v) == getCurrentPage()) {
            final ApplicationInfo app = (ApplicationInfo) v.getTag();
            animateClickFeedback(v, new Runnable() {
                public void run() {
                    AllAppsPagedView.this.mLauncher.startActivitySafely(app.intent, app);
                }
            });
            endChoiceMode();
        }
    }

    private void setupDragMode(ApplicationInfo info) {
        this.mLauncher.getWorkspace().shrink(Workspace.ShrinkState.BOTTOM_VISIBLE);
        if ((info.flags & 1) != 0) {
            DeleteZone allAppsDeleteZone = (DeleteZone) this.mLauncher.findViewById(R.id.all_apps_delete_zone);
            allAppsDeleteZone.setDragAndDropEnabled(true);
            if ((info.flags & 2) != 0) {
                allAppsDeleteZone.setText(R.string.delete_zone_label_all_apps_system_app);
            } else {
                allAppsDeleteZone.setText(R.string.delete_zone_label_all_apps);
            }
        }
        ((ApplicationInfoDropTarget) this.mLauncher.findViewById(R.id.all_apps_info_target)).setDragAndDropEnabled(true);
    }

    private void tearDownDragMode() {
        post(new Runnable() {
            public void run() {
                DeleteZone allAppsDeleteZone = (DeleteZone) AllAppsPagedView.this.mLauncher.findViewById(R.id.all_apps_delete_zone);
                if (allAppsDeleteZone != null) {
                    allAppsDeleteZone.setDragAndDropEnabled(false);
                }
                ApplicationInfoDropTarget allAppsInfoButton = (ApplicationInfoDropTarget) AllAppsPagedView.this.mLauncher.findViewById(R.id.all_apps_info_target);
                if (allAppsInfoButton != null) {
                    allAppsInfoButton.setDragAndDropEnabled(false);
                }
            }
        });
        resetCheckedGrandchildren();
        this.mDragController.removeDropTarget(this);
    }

    /* access modifiers changed from: protected */
    public boolean beginDragging(View v) {
        if (!v.isInTouchMode()) {
            return false;
        }
        if (!super.beginDragging(v)) {
            return false;
        }
        ApplicationInfo app = new ApplicationInfo((ApplicationInfo) v.getTag());
        setupDragMode(app);
        Drawable icon = ((TextView) v).getCompoundDrawables()[1];
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate((float) ((v.getWidth() - icon.getIntrinsicWidth()) / 2), (float) v.getPaddingTop());
        icon.draw(c);
        if (v instanceof Checkable) {
            resetCheckedGrandchildren();
            ((Checkable) v).toggle();
        }
        this.mLauncher.lockScreenOrientation();
        this.mLauncher.getWorkspace().onDragStartedWithItemSpans(1, 1, b);
        this.mDragController.startDrag(v, b, (DragSource) this, (Object) app, DragController.DRAG_ACTION_COPY, (Rect) null);
        b.recycle();
        return true;
    }

    public void onDragViewVisible() {
    }

    public void onDropCompleted(View target, Object dragInfo, boolean success) {
        if (target != this) {
            endChoiceMode();
        }
        tearDownDragMode();
        this.mLauncher.getWorkspace().onDragStopped(success);
        this.mLauncher.unlockScreenOrientation();
    }

    /* access modifiers changed from: package-private */
    public int getPageContentWidth() {
        return this.mPageContentWidth;
    }

    public boolean isVisible() {
        return this.mZoom > 0.001f;
    }

    public boolean isAnimating() {
        return getAnimation() != null;
    }

    private ArrayList<ApplicationInfo> rebuildFilteredApps(ArrayList<ApplicationInfo> apps) {
        ArrayList<ApplicationInfo> filteredApps = new ArrayList<>();
        if (this.mAppFilter == -1) {
            return apps;
        }
        int length = apps.size();
        for (int i = 0; i < length; i++) {
            ApplicationInfo info = apps.get(i);
            if ((info.flags & this.mAppFilter) > 0) {
                filteredApps.add(info);
            }
        }
        Collections.sort(filteredApps, LauncherModel.APP_INSTALL_TIME_COMPARATOR);
        return filteredApps;
    }

    public void setApps(ArrayList<ApplicationInfo> list) {
        this.mApps = list;
        Collections.sort(this.mApps, LauncherModel.APP_NAME_COMPARATOR);
        this.mFilteredApps = rebuildFilteredApps(this.mApps);
        mPageViewIconCache.retainAllApps(list);
        this.mWaitingToInitPages = false;
        invalidatePageData();
    }

    private void addAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        int count = list.size();
        for (int i = 0; i < count; i++) {
            ApplicationInfo info = list.get(i);
            int index = Collections.binarySearch(this.mApps, info, LauncherModel.APP_NAME_COMPARATOR);
            if (index < 0) {
                this.mApps.add(-(index + 1), info);
            } else {
                this.mApps.add(index, info);
            }
        }
        this.mFilteredApps = rebuildFilteredApps(this.mApps);
    }

    public void addApps(ArrayList<ApplicationInfo> list) {
        addAppsWithoutInvalidate(list);
        invalidatePageData();
    }

    private void removeAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        ArrayList<Checkable> checkedList = getCheckedGrandchildren();
        HashSet<ApplicationInfo> checkedAppInfos = new HashSet<>();
        Iterator i$ = checkedList.iterator();
        while (i$.hasNext()) {
            checkedAppInfos.add((ApplicationInfo) ((PagedViewIcon) i$.next()).getTag());
        }
        Iterator i$2 = list.iterator();
        while (true) {
            if (i$2.hasNext()) {
                if (checkedAppInfos.contains(i$2.next())) {
                    endChoiceMode();
                    break;
                }
            } else {
                break;
            }
        }
        int length = list.size();
        for (int i = 0; i < length; i++) {
            ApplicationInfo info = list.get(i);
            int removeIndex = findAppByComponent(this.mApps, info);
            if (removeIndex > -1) {
                this.mApps.remove(removeIndex);
                mPageViewIconCache.removeOutline(new PagedViewIconCache.Key(info));
            }
        }
        this.mFilteredApps = rebuildFilteredApps(this.mApps);
    }

    public void removeApps(ArrayList<ApplicationInfo> list) {
        removeAppsWithoutInvalidate(list);
        invalidatePageData();
    }

    public void updateApps(ArrayList<ApplicationInfo> list) {
        removeAppsWithoutInvalidate(list);
        addAppsWithoutInvalidate(list);
        invalidatePageData();
    }

    private int findAppByComponent(ArrayList<ApplicationInfo> list, ApplicationInfo item) {
        if (!(item == null || item.intent == null)) {
            ComponentName removeComponent = item.intent.getComponent();
            int length = list.size();
            for (int i = 0; i < length; i++) {
                if (list.get(i).intent.getComponent().equals(removeComponent)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void dumpState() {
        ApplicationInfo.dumpApplicationInfoList("AllAppsPagedView", "mApps", this.mApps);
    }

    public void surrender() {
    }

    private void setupPage(PagedViewCellLayout layout) {
        layout.setCellCount(this.mCellCountX, this.mCellCountY);
        layout.setPadding(this.mPageLayoutPaddingLeft, this.mPageLayoutPaddingTop, this.mPageLayoutPaddingRight, this.mPageLayoutPaddingBottom);
        layout.setGap(this.mPageLayoutWidthGap, this.mPageLayoutHeightGap);
    }

    /* access modifiers changed from: protected */
    public void invalidatePageData() {
        if (!this.mWaitingToDetermineRowsAndColumns && !this.mWaitingToInitPages && this.mCellCountX > 0 && this.mCellCountY > 0) {
            super.invalidatePageData();
        }
    }

    public void syncPages() {
        int numPages;
        if (this.mCellCountX <= 0 || this.mCellCountY <= 0) {
            numPages = 1;
        } else {
            numPages = Math.max(1, (int) Math.ceil((double) (((float) this.mFilteredApps.size()) / ((float) (this.mCellCountX * this.mCellCountY)))));
        }
        int curNumPages = getChildCount();
        int extraPageDiff = curNumPages - numPages;
        for (int i = 0; i < extraPageDiff; i++) {
            removeViewAt(numPages);
        }
        for (int i2 = curNumPages; i2 < numPages; i2++) {
            PagedViewCellLayout layout = new PagedViewCellLayout(getContext());
            if (this.mAllowHardwareLayerCreation) {
                layout.allowHardwareLayerCreation();
            }
            setupPage(layout);
            addView(layout);
        }
        setCurrentPage(Math.max(0, Math.min(numPages - 1, getCurrentPage())));
    }

    public void syncPageItems(int page) {
        if (this.mCellCountX <= 0 || this.mCellCountY <= 0) {
            return;
        }
        int cellsPerPage = this.mCellCountX * this.mCellCountY;
        int startIndex = page * cellsPerPage;
        int endIndex = Math.min(startIndex + cellsPerPage, this.mFilteredApps.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getChildAt(page);
        if (!this.mFilteredApps.isEmpty()) {
            int curNumPageItems = layout.getPageChildCount();
            int numPageItems = endIndex - startIndex;
            boolean wasEmptyPage = false;
            if (curNumPageItems == 1 && layout.getChildOnPageAt(0).getTag() == null) {
                wasEmptyPage = true;
            }
            if (wasEmptyPage) {
                curNumPageItems = 0;
                layout.removeAllViewsOnPage();
            } else {
                int extraPageItemsDiff = curNumPageItems - numPageItems;
                for (int i = 0; i < extraPageItemsDiff; i++) {
                    layout.removeViewOnPageAt(numPageItems);
                }
            }
            for (int i2 = curNumPageItems; i2 < numPageItems; i2++) {
                TextView text = (TextView) this.mInflater.inflate(R.layout.all_apps_paged_view_application, layout, false);
                text.setOnClickListener(this);
                text.setOnLongClickListener(this);
                text.setOnTouchListener(this);
                layout.addViewToCellLayout(text, -1, i2, new PagedViewCellLayout.LayoutParams(0, 0, 1, 1));
            }
            int numPages = getPageCount();
            for (int i3 = startIndex; i3 < endIndex; i3++) {
                int index = i3 - startIndex;
                ApplicationInfo info = this.mFilteredApps.get(i3);
                PagedViewIcon icon = (PagedViewIcon) layout.getChildOnPageAt(index);
                icon.applyFromApplicationInfo(info, mPageViewIconCache, true, numPages > 1);
                PagedViewCellLayout.LayoutParams params = (PagedViewCellLayout.LayoutParams) icon.getLayoutParams();
                params.cellX = index % this.mCellCountX;
                params.cellY = index / this.mCellCountX;
            }
            layout.enableCenteredContent(false);
        } else {
            TextView icon2 = (TextView) this.mInflater.inflate(R.layout.all_apps_no_items_placeholder, layout, false);
            switch (this.mAppFilter) {
                case 1:
                    icon2.setText(getContext().getString(R.string.all_apps_no_downloads));
                    break;
            }
            layout.enableCenteredContent(true);
            layout.removeAllViewsOnPage();
            layout.addViewToCellLayout(icon2, -1, 0, new PagedViewCellLayout.LayoutParams(0, 0, 4, 1));
        }
        layout.createHardwareLayers();
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        return false;
    }

    public DropTarget getDropTargetDelegate(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        return null;
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public boolean isDropEnabled() {
        return true;
    }
}
