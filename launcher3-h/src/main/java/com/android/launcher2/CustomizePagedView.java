package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher2.PagedViewCellLayout;
import com.android.launcher2.PagedViewIconCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CustomizePagedView extends PagedViewWithDraggableItems implements ActionMode.Callback, View.OnClickListener, DragSource {
    private final float ANIMATION_SCALE;
    private final int DROP_ANIM_DURATION;
    private final int TRANSLATE_ANIM_DURATION;
    private AllAppsPagedView mAllAppsPagedView;
    private List<ApplicationInfo> mApps;
    private final Canvas mCanvas;
    private int mChoiceModeTitleText;
    private CustomizationType mCustomizationType;
    private Bitmap mDragBitmap;
    private DragController mDragController;
    private int[] mDragViewOrigin;
    private boolean mFirstMeasure;
    /* access modifiers changed from: private */
    public final LayoutInflater mInflater;
    /* access modifiers changed from: private */
    public Launcher mLauncher;
    private int mMaxCellCountY;
    private int mMaxWallpaperCellHSpan;
    private int mMaxWidgetPreviewDim;
    /* access modifiers changed from: private */
    public int mMaxWidgetWidth;
    private int mMaxWidgetsCellHSpan;
    private int mMinPageWidth;
    private int mMinWidgetPreviewDim;
    /* access modifiers changed from: private */
    public PackageManager mPackageManager;
    private int mPageContentHeight;
    private int mPageContentWidth;
    private TimeInterpolator mQuintEaseOutInterpolator;
    private List<ResolveInfo> mShortcutList;
    private final float[] mTmpFloatPos;
    private boolean mWaitingToDetermineRowsAndColumns;
    private boolean mWaitingToInitPages;
    private int mWallpaperCellHSpan;
    private List<ResolveInfo> mWallpaperList;
    private List<AppWidgetProviderInfo> mWidgetList;
    private ArrayList<ArrayList<AppWidgetProviderInfo>> mWidgetPages;
    private PagedViewCellLayout mWorkspaceWidgetLayout;

    public enum CustomizationType {
        WidgetCustomization,
        ShortcutCustomization,
        WallpaperCustomization,
        ApplicationCustomization
    }

    public CustomizePagedView(Context context) {
        this(context, (AttributeSet) null, 0);
    }

    public CustomizePagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomizePagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCanvas = new Canvas();
        this.mFirstMeasure = true;
        this.mTmpFloatPos = new float[2];
        this.ANIMATION_SCALE = 0.5f;
        this.TRANSLATE_ANIM_DURATION = 400;
        this.DROP_ANIM_DURATION = 200;
        this.mQuintEaseOutInterpolator = new DecelerateInterpolator(2.5f);
        this.mDragViewOrigin = new int[2];
        this.mPageContentWidth = -1;
        this.mPageContentHeight = -1;
        this.mWaitingToInitPages = true;
        this.mWaitingToDetermineRowsAndColumns = true;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomizePagedView, defStyle, 0);
        this.mWallpaperCellHSpan = a.getInt(R.styleable.CustomizePagedView_wallpaperCellSpanX, 4);
        this.mMaxWallpaperCellHSpan = a.getInt(R.styleable.CustomizePagedView_wallpaperCellCountX, 8);
        this.mMaxWidgetsCellHSpan = a.getInt(R.styleable.CustomizePagedView_widgetCellCountX, 8);
        a.recycle();
        this.mCustomizationType = CustomizationType.WidgetCustomization;
        this.mWidgetPages = new ArrayList<>();
        this.mWorkspaceWidgetLayout = new PagedViewCellLayout(context);
        this.mInflater = LayoutInflater.from(context);
        Resources r = context.getResources();
        setDragSlopeThreshold(((float) r.getInteger(R.integer.config_customizationDrawerDragSlopeThreshold)) / 100.0f);
        this.mMaxCellCountY = r.getInteger(R.integer.customization_drawer_contents_maxCellCountY);
        setVisibility(8);
        setSoundEffectsEnabled(false);
        setupWorkspaceLayout();
    }

    /* access modifiers changed from: protected */
    public void init() {
        super.init();
        this.mCenterPagesVertically = false;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthSpec, int heightSpec) {
        int cellCountX = this.mAllAppsPagedView.getCellCountX();
        int cellCountY = Math.min(this.mAllAppsPagedView.getCellCountY(), this.mMaxCellCountY);
        if (!(cellCountX == this.mCellCountX && cellCountY == this.mCellCountY)) {
            this.mCellCountX = cellCountX;
            this.mCellCountY = cellCountY;
            PagedViewCellLayout layout = new PagedViewCellLayout(getContext());
            setupPage(layout);
            this.mPageContentWidth = layout.getContentWidth();
            this.mPageContentHeight = layout.getContentHeight();
            this.mMinPageWidth = layout.getWidthBeforeFirstLayout();
            postInvalidatePageData(true);
        }
        if (this.mPageContentHeight > 0) {
            heightSpec = View.MeasureSpec.makeMeasureSpec(this.mPageContentHeight + this.mPageLayoutPaddingTop + this.mPageLayoutPaddingBottom, 1073741824);
        }
        super.onMeasure(widthSpec, heightSpec);
        this.mFirstMeasure = false;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mWaitingToDetermineRowsAndColumns) {
            this.mWaitingToDetermineRowsAndColumns = false;
            postInvalidatePageData(false);
        }
        super.onLayout(changed, left, top, right, bottom);
        this.mFirstLayout = false;
    }

    public void setLauncher(Launcher launcher) {
        Context context = getContext();
        this.mLauncher = launcher;
        this.mPackageManager = context.getPackageManager();
    }

    public void setAllAppsPagedView(AllAppsPagedView view) {
        this.mAllAppsPagedView = view;
    }

    public void setApps(ArrayList<ApplicationInfo> list) {
        this.mApps = list;
        Collections.sort(this.mApps, LauncherModel.APP_NAME_COMPARATOR);
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
            }
        }
    }

    public void addApps(ArrayList<ApplicationInfo> list) {
        addAppsWithoutInvalidate(list);
        invalidatePageData();
    }

    private void removeAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        int length = list.size();
        for (int i = 0; i < length; i++) {
            ApplicationInfo info = list.get(i);
            int removeIndex = findAppByComponent(this.mApps, info);
            if (removeIndex > -1) {
                this.mApps.remove(removeIndex);
                mPageViewIconCache.removeOutline(new PagedViewIconCache.Key(info));
            }
        }
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

    private int findAppByComponent(List<ApplicationInfo> list, ApplicationInfo item) {
        ComponentName removeComponent = item.intent.getComponent();
        int length = list.size();
        for (int i = 0; i < length; i++) {
            if (list.get(i).intent.getComponent().equals(removeComponent)) {
                return i;
            }
        }
        return -1;
    }

    public void update() {
        this.mWidgetList = AppWidgetManager.getInstance(this.mLauncher).getInstalledProviders();
        Collections.sort(this.mWidgetList, new Comparator<AppWidgetProviderInfo>() {
            public int compare(AppWidgetProviderInfo object1, AppWidgetProviderInfo object2) {
                return object1.label.compareTo(object2.label);
            }
        });
        Comparator<ResolveInfo> resolveInfoComparator = new Comparator<ResolveInfo>() {
            public int compare(ResolveInfo object1, ResolveInfo object2) {
                return object1.loadLabel(CustomizePagedView.this.mPackageManager).toString().compareTo(object2.loadLabel(CustomizePagedView.this.mPackageManager).toString());
            }
        };
        this.mShortcutList = this.mPackageManager.queryIntentActivities(new Intent("android.intent.action.CREATE_SHORTCUT"), 0);
        Collections.sort(this.mShortcutList, resolveInfoComparator);
        this.mWallpaperList = this.mPackageManager.queryIntentActivities(new Intent("android.intent.action.SET_WALLPAPER"), 128);
        Collections.sort(this.mWallpaperList, resolveInfoComparator);
        ArrayList<ResolveInfo> retainShortcutList = new ArrayList<>(this.mShortcutList);
        retainShortcutList.addAll(this.mWallpaperList);
        mPageViewIconCache.retainAllShortcuts(retainShortcutList);
        mPageViewIconCache.retainAllAppWidgets(this.mWidgetList);
        invalidatePageData();
    }

    public void setDragController(DragController dragger) {
        this.mDragController = dragger;
    }

    public void setCustomizationFilter(CustomizationType filterType) {
        cancelDragging();
        this.mCustomizationType = filterType;
        if (getChildCount() > 0) {
            setCurrentPage(0);
            updateCurrentPageScroll();
            invalidatePageData();
            endChoiceMode();
        }
    }

    public CustomizationType getCustomizationFilter() {
        return this.mCustomizationType;
    }

    /* access modifiers changed from: private */
    public void resetCheckedItem(boolean animated) {
        Checkable checkable = getSingleCheckedGrandchild();
        if (checkable == null) {
            return;
        }
        if (checkable instanceof PagedViewWidget) {
            ((PagedViewWidget) checkable).setChecked(false, animated);
        } else {
            ((PagedViewIcon) checkable).setChecked(false, animated);
        }
    }

    public void onDropCompleted(View target, Object dragInfo, boolean success) {
        final DragLayer dragLayer = (DragLayer) this.mLauncher.findViewById(R.id.drag_layer);
        int[] pos = this.mDragController.getDragView().getPosition((int[]) null);
        final View animView = dragLayer.createDragView(this.mDragBitmap, pos[0], pos[1]);
        animView.setVisibility(0);
        if (success) {
            resetCheckedItem(true);
            animateDropOntoScreen(animView, (ItemInfo) dragInfo, 200, 0);
        } else {
            animateIntoPosition(animView, (float) this.mDragViewOrigin[0], (float) this.mDragViewOrigin[1], new Runnable() {
                public void run() {
                    CustomizePagedView.this.resetCheckedItem(false);
                    dragLayer.removeView(animView);
                }
            });
        }
        this.mLauncher.getWorkspace().onDragStopped(success);
        this.mLauncher.unlockScreenOrientation();
        this.mDragBitmap = null;
    }

    public void onDragViewVisible() {
    }

    /* access modifiers changed from: private */
    public void animateItemOntoScreen(View dragView, CellLayout layout, ItemInfo info) {
        this.mTmpFloatPos[0] = (float) (layout.getWidth() / 2);
        this.mTmpFloatPos[1] = (float) (layout.getHeight() / 2);
        this.mLauncher.getWorkspace().mapPointFromChildToSelf(layout, this.mTmpFloatPos);
        int dragViewWidth = dragView.getMeasuredWidth();
        int dragViewHeight = dragView.getMeasuredHeight();
        float heightOffset = 0.0f;
        float widthOffset = 0.0f;
        if (dragView instanceof ImageView) {
            Drawable d = ((ImageView) dragView).getDrawable();
            int width = d.getIntrinsicWidth();
            int height = d.getIntrinsicHeight();
            if ((1.0d * ((double) width)) / ((double) height) >= ((double) ((1.0f * ((float) dragViewWidth)) / ((float) dragViewHeight)))) {
                heightOffset = (0.5f * (((float) dragViewHeight) - (((float) height) * (((float) dragViewWidth) / (((float) width) * 1.0f))))) / 2.0f;
            } else {
                widthOffset = (0.5f * (((float) dragViewWidth) - (((float) width) * (((float) dragViewHeight) / (((float) height) * 1.0f))))) / 2.0f;
            }
        }
        float toX = (this.mTmpFloatPos[0] - ((float) (dragView.getMeasuredWidth() / 2))) + widthOffset;
        float toY = (this.mTmpFloatPos[1] - ((float) (dragView.getMeasuredHeight() / 2))) + heightOffset;
        View dragCopy = ((DragLayer) this.mLauncher.findViewById(R.id.drag_layer)).createDragView(dragView);
        dragCopy.setAlpha(1.0f);
        animateIntoPosition(dragCopy, toX, toY, (Runnable) null);
        animateDropOntoScreen(dragCopy, info, 200, 200);
    }

    private void animateDropOntoScreen(View view, ItemInfo info, int duration, int delay) {
        final DragLayer dragLayer = (DragLayer) this.mLauncher.findViewById(R.id.drag_layer);
        final CellLayout layout = this.mLauncher.getWorkspace().getCurrentDropLayout();
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view, new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("alpha", new float[]{1.0f, 0.0f}), PropertyValuesHolder.ofFloat("scaleX", new float[]{0.5f}), PropertyValuesHolder.ofFloat("scaleY", new float[]{0.5f})});
        anim.setInterpolator(new LinearInterpolator());
        if (delay > 0) {
            anim.setStartDelay((long) delay);
        }
        anim.setDuration((long) duration);
        final View view2 = view;
        final ItemInfo itemInfo = info;
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                dragLayer.removeView(view2);
                CustomizePagedView.this.mLauncher.addExternalItemToScreen(itemInfo, layout);
                itemInfo.dropPos = null;
            }
        });
        anim.start();
    }

    private void animateIntoPosition(View view, float toX, float toY, final Runnable endRunnable) {
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view, new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("x", new float[]{toX}), PropertyValuesHolder.ofFloat("y", new float[]{toY})});
        anim.setInterpolator(this.mQuintEaseOutInterpolator);
        anim.setDuration(400);
        if (endRunnable != null) {
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    endRunnable.run();
                }
            });
        }
        anim.start();
    }

    public void onClick(View v) {
        if (v.isInTouchMode() && this.mNextPage == -1) {
            boolean enterChoiceMode = false;
            switch (AnonymousClass9.$SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[this.mCustomizationType.ordinal()]) {
                case 1:
                    this.mChoiceModeTitleText = R.string.cab_widget_selection_text;
                    enterChoiceMode = true;
                    break;
                case 2:
                    this.mChoiceModeTitleText = R.string.cab_app_selection_text;
                    enterChoiceMode = true;
                    break;
                case 3:
                    this.mChoiceModeTitleText = R.string.cab_shortcut_selection_text;
                    enterChoiceMode = true;
                    break;
            }
            if (enterChoiceMode) {
                final ItemInfo itemInfo = (ItemInfo) v.getTag();
                final CellLayout cl = (CellLayout) this.mLauncher.getWorkspace().getChildAt(this.mLauncher.getCurrentWorkspaceScreen());
                final View dragView = getDragView(v);
                animateClickFeedback(v, new Runnable() {
                    public void run() {
                        cl.calculateSpans(itemInfo);
                        if (cl.findCellForSpan((int[]) null, itemInfo.spanX, itemInfo.spanY)) {
                            CustomizePagedView.this.animateItemOntoScreen(dragView, cl, itemInfo);
                        } else {
                            CustomizePagedView.this.mLauncher.showOutOfSpaceMessage();
                        }
                    }
                });
                return;
            }
            switch (AnonymousClass9.$SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[this.mCustomizationType.ordinal()]) {
                case 4:
                    final View clickView = v;
                    animateClickFeedback(v, new Runnable() {
                        public void run() {
                            ResolveInfo info = (ResolveInfo) clickView.getTag();
                            Intent createWallpapersIntent = new Intent("android.intent.action.SET_WALLPAPER");
                            createWallpapersIntent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                            CustomizePagedView.this.mLauncher.processWallpaper(createWallpapersIntent);
                        }
                    });
                    return;
                default:
                    return;
            }
        }
    }

    /* renamed from: com.android.launcher2.CustomizePagedView$9  reason: invalid class name */
    static /* synthetic */ class AnonymousClass9 {
        static final /* synthetic */ int[] $SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType = new int[CustomizationType.values().length];

        static {
            try {
                $SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[CustomizationType.WidgetCustomization.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[CustomizationType.ApplicationCustomization.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[CustomizationType.ShortcutCustomization.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[CustomizationType.WallpaperCustomization.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private Bitmap drawableToBitmap(Drawable d, float scaleX, float scaleY) {
        Rect bounds = d.getBounds();
        int w = bounds.width();
        int h = bounds.height();
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        renderDrawableToBitmap(d, b, 0, 0, w, h, scaleX, scaleY);
        return b;
    }

    private View getDragView(View v) {
        return this.mCustomizationType == CustomizationType.WidgetCustomization ? v.findViewById(R.id.widget_preview) : v;
    }

    /* access modifiers changed from: protected */
    public boolean beginDragging(View v) {
        if (!v.isInTouchMode() || !super.beginDragging(v)) {
            return false;
        }
        if (isChoiceMode(1)) {
            endChoiceMode();
        }
        Workspace workspace = this.mLauncher.getWorkspace();
        boolean result = false;
        this.mLauncher.lockScreenOrientation();
        switch (AnonymousClass9.$SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[this.mCustomizationType.ordinal()]) {
            case 1:
                if (v instanceof PagedViewWidget) {
                    ImageView i = (ImageView) ((LinearLayout) v).findViewById(R.id.widget_preview);
                    RectF rectF = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
                    i.getImageMatrix().mapRect(rectF);
                    this.mDragBitmap = drawableToBitmap(i.getDrawable(), rectF.right, rectF.bottom);
                    i.getLocationOnScreen(this.mDragViewOrigin);
                    PendingAddWidgetInfo createWidgetInfo = (PendingAddWidgetInfo) v.getTag();
                    int[] spanXY = CellLayout.rectToCell(getResources(), createWidgetInfo.minWidth, createWidgetInfo.minHeight, (int[]) null);
                    createWidgetInfo.spanX = spanXY[0];
                    createWidgetInfo.spanY = spanXY[1];
                    workspace.onDragStartedWithItemSpans(spanXY[0], spanXY[1], this.mDragBitmap);
                    this.mDragController.startDrag((View) i, this.mDragBitmap, (DragSource) this, (Object) createWidgetInfo, DragController.DRAG_ACTION_COPY, (Rect) null);
                    result = true;
                    break;
                }
                break;
            case 2:
            case 3:
                if (v instanceof PagedViewIcon) {
                    Drawable icon = ((TextView) v).getCompoundDrawables()[1];
                    this.mDragBitmap = drawableToBitmap(icon, 1.0f, 1.0f);
                    Object dragInfo = v.getTag();
                    if (this.mCustomizationType == CustomizationType.ApplicationCustomization) {
                        dragInfo = new ApplicationInfo((ApplicationInfo) dragInfo);
                    }
                    workspace.onDragStartedWithItemSpans(1, 1, this.mDragBitmap);
                    v.getLocationOnScreen(this.mDragViewOrigin);
                    int[] iArr = this.mDragViewOrigin;
                    iArr[0] = iArr[0] + ((v.getWidth() - icon.getIntrinsicWidth()) / 2);
                    int[] iArr2 = this.mDragViewOrigin;
                    iArr2[1] = iArr2[1] + v.getPaddingTop();
                    this.mDragController.startDrag(this.mDragBitmap, this.mDragViewOrigin[0], this.mDragViewOrigin[1], (DragSource) this, dragInfo, DragController.DRAG_ACTION_COPY);
                    result = true;
                    break;
                }
                break;
        }
        if (result && (v instanceof Checkable)) {
            resetCheckedGrandchildren();
            ((Checkable) v).toggle();
        }
        return result;
    }

    private int relayoutWidgets() {
        if (this.mWidgetList.isEmpty()) {
            return 0;
        }
        ArrayList<AppWidgetProviderInfo> newPage = new ArrayList<>();
        this.mWidgetPages.clear();
        this.mWidgetPages.add(newPage);
        int maxNumCellsPerRow = this.mMaxWidgetsCellHSpan;
        int widgetCount = this.mWidgetList.size();
        int numCellsInRow = 0;
        for (int i = 0; i < widgetCount; i++) {
            AppWidgetProviderInfo info = this.mWidgetList.get(i);
            int cellSpanX = Math.max(2, Math.min(4, this.mWorkspaceWidgetLayout.estimateCellHSpan(info.minWidth)));
            if (numCellsInRow + cellSpanX > maxNumCellsPerRow) {
                numCellsInRow = 0;
                newPage = new ArrayList<>();
                this.mWidgetPages.add(newPage);
            }
            newPage.add(info);
            numCellsInRow += cellSpanX;
        }
        return this.mWidgetPages.size();
    }

    private void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h, float scaleX, float scaleY) {
        if (bitmap != null) {
            this.mCanvas.setBitmap(bitmap);
        }
        this.mCanvas.save();
        this.mCanvas.scale(scaleX, scaleY);
        Rect oldBounds = d.copyBounds();
        d.setBounds(x, y, x + w, y + h);
        d.draw(this.mCanvas);
        d.setBounds(oldBounds);
        this.mCanvas.restore();
    }

    private Drawable parseWallpaperPreviewXml(ComponentName componentName, ResolveInfo resolveInfo) {
        android.content.res.XmlResourceParser parser = null;
        try {
            parser = resolveInfo.activityInfo.loadXmlMetaData(this.mPackageManager, "android.wallpaper.preview");
            if (parser == null) {
                Log.w("CustomizeWorkspace",
                        "No android.wallpaper.preview meta-data for wallpaper provider '" + componentName + "'");
                return null;
            }

            android.util.AttributeSet attrs = android.util.Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != org.xmlpull.v1.XmlPullParser.START_TAG
                    && type != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                // Skip until start tag.
            }
            if (type != org.xmlpull.v1.XmlPullParser.START_TAG) {
                return null;
            }
            if (!"wallpaper-preview".equals(parser.getName())) {
                Log.w("CustomizeWorkspace",
                        "Meta-data does not start with wallpaper-preview tag for wallpaper provider '" + componentName + "'");
                return null;
            }

            Resources res = this.mPackageManager.getResourcesForApplication(resolveInfo.activityInfo.applicationInfo);
            TypedArray a = res.obtainAttributes(attrs, new int[] { android.R.attr.src });
            int previewResId = a.getResourceId(0, 0);
            a.recycle();
            if (previewResId == 0) {
                return null;
            }
            return res.getDrawable(previewResId);
        } catch (Throwable t) {
            Log.w("CustomizeWorkspace", "XML parsing failed for wallpaper provider '" + componentName + "'", t);
            return null;
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private FastBitmapDrawable getWallpaperPreview(ResolveInfo resolveInfo) {
        Drawable drawable;
        boolean z = true;
        int estimateCellWidth = this.mWorkspaceWidgetLayout.estimateCellWidth(1);
        int estimateCellWidth2 = this.mWorkspaceWidgetLayout.estimateCellWidth(this.mWallpaperCellHSpan);
        Resources resources = this.mLauncher.getResources();
        int i = (int) (((float) estimateCellWidth2) * 0.75f);
        int i2 = (int) (((float) estimateCellWidth2) * 0.75f);
        Bitmap createBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        Drawable parseWallpaperPreviewXml = parseWallpaperPreviewXml(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name), resolveInfo);
        if (parseWallpaperPreviewXml == null) {
            z = false;
        }
        if (!z) {
            drawable = resources.getDrawable(R.drawable.default_widget_preview);
        } else {
            drawable = parseWallpaperPreviewXml;
        }
        renderDrawableToBitmap(drawable, createBitmap, 0, 0, i, i2, 1.0f, 1.0f);
        if (!z) {
            try {
                int i3 = estimateCellWidth / 2;
                int i4 = i3 / 4;
                renderDrawableToBitmap(new FastBitmapDrawable(Utilities.createIconBitmap(LauncherApplication.getSharedInstance(this.mLauncher).getIconCache().getFullResIcon(resolveInfo, this.mPackageManager), getContext())), (Bitmap) null, i4, i4, i3, i3, 1.0f, 1.0f);
            } catch (Resources.NotFoundException e) {
            }
        }
        FastBitmapDrawable fastBitmapDrawable = new FastBitmapDrawable(createBitmap);
        fastBitmapDrawable.setBounds(0, 0, fastBitmapDrawable.getIntrinsicWidth(), fastBitmapDrawable.getIntrinsicHeight());
        return fastBitmapDrawable;
    }

    /* access modifiers changed from: private */
    public Bitmap getWidgetPreview(AppWidgetProviderInfo appWidgetProviderInfo) {
        Drawable drawable;
        Drawable drawable2;
        Drawable drawable3;
        PackageManager packageManager = this.mPackageManager;
        String packageName = appWidgetProviderInfo.provider.getPackageName();
        if (appWidgetProviderInfo.previewImage != 0) {
            Drawable drawable4 = packageManager.getDrawable(packageName, appWidgetProviderInfo.previewImage, (android.content.pm.ApplicationInfo) null);
            if (drawable4 == null) {
                Log.w("CustomizeWorkspace", "Can't load icon drawable 0x" + Integer.toHexString(appWidgetProviderInfo.icon) + " for provider: " + appWidgetProviderInfo.provider);
            }
            drawable = drawable4;
        } else {
            drawable = null;
        }
        if (drawable == null) {
            Resources resources = this.mLauncher.getResources();
            int max = (int) (((float) Math.max(this.mMinWidgetPreviewDim, Math.min(this.mMaxWidgetPreviewDim, appWidgetProviderInfo.minWidth))) * 0.75f);
            int max2 = (int) (((float) Math.max(this.mMinWidgetPreviewDim, Math.min(this.mMaxWidgetPreviewDim, appWidgetProviderInfo.minHeight))) * 0.75f);
            Bitmap createBitmap = Bitmap.createBitmap(max, max2, Bitmap.Config.ARGB_8888);
            renderDrawableToBitmap(resources.getDrawable(R.drawable.default_widget_preview), createBitmap, 0, 0, max, max2, 1.0f, 1.0f);
            try {
                if (appWidgetProviderInfo.icon > 0) {
                    drawable2 = packageManager.getDrawable(packageName, appWidgetProviderInfo.icon, (android.content.pm.ApplicationInfo) null);
                } else {
                    drawable2 = null;
                }
                if (drawable2 == null) {
                    drawable3 = resources.getDrawable(R.drawable.ic_launcher_application);
                } else {
                    drawable3 = drawable2;
                }
                int i = this.mMinWidgetPreviewDim / 2;
                int i2 = i / 4;
                renderDrawableToBitmap(drawable3, (Bitmap) null, i2, i2, i, i, 1.0f, 1.0f);
            } catch (Resources.NotFoundException e) {
            }
            return createBitmap;
        }
        float intrinsicWidth = (float) drawable.getIntrinsicWidth();
        float intrinsicHeight = (float) drawable.getIntrinsicHeight();
        int max3 = (int) (Math.max((float) this.mMinWidgetPreviewDim, Math.min((float) this.mMaxWidgetPreviewDim, intrinsicWidth)) * 0.75f);
        int max4 = (int) (Math.max((float) this.mMinWidgetPreviewDim, Math.min((float) this.mMaxWidgetPreviewDim, intrinsicHeight)) * 0.75f);
        if (intrinsicWidth / intrinsicHeight >= 1.0f) {
            max4 = (int) ((((float) max3) / intrinsicWidth) * intrinsicHeight);
        } else {
            max3 = (int) (intrinsicWidth * (((float) max4) / intrinsicHeight));
        }
        Bitmap createBitmap2 = Bitmap.createBitmap(max3, max4, Bitmap.Config.ARGB_8888);
        renderDrawableToBitmap(drawable, createBitmap2, 0, 0, max3, max4, 1.0f, 1.0f);
        return createBitmap2;
    }

    private void setupPage(PagedViewCellLayout layout) {
        layout.setCellCount(this.mCellCountX, this.mCellCountY);
        layout.setPadding(this.mPageLayoutPaddingLeft, this.mPageLayoutPaddingTop, this.mPageLayoutPaddingRight, this.mPageLayoutPaddingBottom);
        layout.setGap(this.mPageLayoutWidthGap, this.mPageLayoutHeightGap);
    }

    private void setupWorkspaceLayout() {
        this.mWorkspaceWidgetLayout.setCellCount(this.mCellCountX, this.mCellCountY);
        this.mWorkspaceWidgetLayout.setPadding(20, 10, 20, 0);
        this.mMaxWidgetWidth = this.mWorkspaceWidgetLayout.estimateCellWidth(4);
        this.mMinWidgetPreviewDim = this.mWorkspaceWidgetLayout.estimateCellWidth(1);
        this.mMaxWidgetPreviewDim = this.mWorkspaceWidgetLayout.estimateCellWidth(3);
    }

    private void syncWidgetPages() {
        if (this.mWidgetList != null) {
            removeAllViews();
            int numPages = relayoutWidgets();
            for (int i = 0; i < numPages; i++) {
                PagedViewExtendedLayout layout = new PagedViewExtendedLayout(getContext());
                layout.setGravity(1);
                layout.setPadding(this.mPageLayoutPaddingLeft, this.mPageLayoutPaddingTop, this.mPageLayoutPaddingRight, this.mPageLayoutPaddingBottom);
                if (i < numPages - 1) {
                    layout.setHasFixedWidth(true);
                    layout.setMinimumWidth(this.mMinPageWidth);
                }
                addView(layout, new LinearLayout.LayoutParams(-2, -1));
            }
        }
    }

    private static class AppWidgetsPageToSync {
        public ArrayList<Bitmap> mAppWidgetBitmaps;
        public ArrayList<AppWidgetProviderInfo> mAppWidgets;
        public LinearLayout mLayout;

        public AppWidgetsPageToSync(LinearLayout layout, ArrayList<AppWidgetProviderInfo> appWidgets) {
            this.mLayout = layout;
            this.mAppWidgets = (ArrayList) appWidgets.clone();
            this.mAppWidgetBitmaps = new ArrayList<>(appWidgets.size());
        }
    }

    private void syncWidgetPageItems(int page) {
        LinearLayout layout = (LinearLayout) getChildAt(page);
        layout.removeAllViews();
        AppWidgetsPageToSync pageToSync = new AppWidgetsPageToSync(layout, this.mWidgetPages.get(page));
        new SyncWidgetPageItemsTask().execute(new AppWidgetsPageToSync[]{pageToSync});
    }

    private class SyncWidgetPageItemsTask extends AsyncTask<AppWidgetsPageToSync, Void, AppWidgetsPageToSync> {
        private SyncWidgetPageItemsTask() {
        }

        /* access modifiers changed from: protected */
        public AppWidgetsPageToSync doInBackground(AppWidgetsPageToSync... args) {
            if (args.length != 1) {
                throw new RuntimeException("Wrong number of args to SyncWidgetPageItemsTask");
            }
            AppWidgetsPageToSync pageToSync = args[0];
            synchronized (CustomizePagedView.this) {
                int numWidgets = pageToSync.mAppWidgets.size();
                for (int i = 0; i < numWidgets; i++) {
                    pageToSync.mAppWidgetBitmaps.add(CustomizePagedView.this.getWidgetPreview(pageToSync.mAppWidgets.get(i)));
                }
            }
            return pageToSync;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(AppWidgetsPageToSync pageToSync) {
            LinearLayout layout = pageToSync.mLayout;
            int numPages = CustomizePagedView.this.getPageCount();
            int numWidgets = pageToSync.mAppWidgets.size();
            for (int i = 0; i < numWidgets; i++) {
                AppWidgetProviderInfo info = pageToSync.mAppWidgets.get(i);
                PendingAddWidgetInfo createItemInfo = new PendingAddWidgetInfo(info, (String) null, (Parcelable) null);
                int[] cellSpans = CellLayout.rectToCell(CustomizePagedView.this.getResources(), info.minWidth, info.minHeight, (int[]) null);
                FastBitmapDrawable icon = new FastBitmapDrawable(pageToSync.mAppWidgetBitmaps.get(i));
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                PagedViewWidget l = (PagedViewWidget) CustomizePagedView.this.mInflater.inflate(R.layout.customize_paged_view_widget, layout, false);
                l.applyFromAppWidgetProviderInfo(info, icon, CustomizePagedView.this.mMaxWidgetWidth, cellSpans, PagedView.mPageViewIconCache, numPages > 1);
                l.setTag(createItemInfo);
                l.setOnClickListener(CustomizePagedView.this);
                l.setOnTouchListener(CustomizePagedView.this);
                l.setOnLongClickListener(CustomizePagedView.this);
                layout.addView(l);
            }
        }
    }

    private void syncWallpaperPages() {
        if (this.mWallpaperList != null) {
            removeAllViews();
            int numPages = (int) Math.ceil((double) (((float) (this.mWallpaperList.size() * this.mWallpaperCellHSpan)) / ((float) this.mMaxWallpaperCellHSpan)));
            for (int i = 0; i < numPages; i++) {
                PagedViewExtendedLayout layout = new PagedViewExtendedLayout(getContext());
                layout.setGravity(1);
                layout.setPadding(this.mPageLayoutPaddingLeft, this.mPageLayoutPaddingTop, this.mPageLayoutPaddingRight, this.mPageLayoutPaddingBottom);
                if (i < numPages - 1) {
                    layout.setHasFixedWidth(true);
                    layout.setMinimumWidth(this.mMinPageWidth);
                }
                addView(layout, new LinearLayout.LayoutParams(-2, -1));
            }
        }
    }

    private void syncWallpaperPageItems(int i) {
        boolean z;
        LinearLayout linearLayout = (LinearLayout) getChildAt(i);
        linearLayout.removeAllViews();
        int size = this.mWallpaperList.size();
        int pageCount = getPageCount();
        int i2 = this.mMaxWallpaperCellHSpan / this.mWallpaperCellHSpan;
        int i3 = i * i2;
        int min = Math.min(size, i2 + i3);
        for (int i4 = i3; i4 < min; i4++) {
            ResolveInfo resolveInfo = this.mWallpaperList.get(i4);
            FastBitmapDrawable wallpaperPreview = getWallpaperPreview(resolveInfo);
            PagedViewWidget pagedViewWidget = (PagedViewWidget) this.mInflater.inflate(R.layout.customize_paged_view_wallpaper, linearLayout, false);
            PackageManager packageManager = this.mPackageManager;
            int i5 = this.mMaxWidgetWidth;
            PagedViewIconCache pagedViewIconCache = mPageViewIconCache;
            if (pageCount > 1) {
                z = true;
            } else {
                z = false;
            }
            pagedViewWidget.applyFromWallpaperInfo(resolveInfo, packageManager, wallpaperPreview, i5, pagedViewIconCache, z);
            pagedViewWidget.setTag(resolveInfo);
            pagedViewWidget.setOnClickListener(this);
            linearLayout.addView(pagedViewWidget);
        }
    }

    private void syncListPages(List<ResolveInfo> list) {
        removeAllViews();
        int numPages = (int) Math.ceil((double) (((float) list.size()) / ((float) (this.mCellCountX * this.mCellCountY))));
        for (int i = 0; i < numPages; i++) {
            PagedViewCellLayout layout = new PagedViewCellLayout(getContext());
            setupPage(layout);
            addView(layout);
        }
    }

    private void syncListPageItems(int i, List<ResolveInfo> list) {
        int pageCount = getPageCount();
        int i2 = this.mCellCountX * this.mCellCountY;
        int i3 = i * i2;
        int min = Math.min(i2 + i3, list.size());
        PagedViewCellLayout pagedViewCellLayout = (PagedViewCellLayout) getChildAt(i);
        pagedViewCellLayout.removeAllViewsOnPage();
        for (int i4 = i3; i4 < min; i4++) {
            ResolveInfo resolveInfo = list.get(i4);
            PendingAddItemInfo pendingAddItemInfo = new PendingAddItemInfo();
            PagedViewIcon pagedViewIcon = (PagedViewIcon) this.mInflater.inflate(R.layout.customize_paged_view_item, pagedViewCellLayout, false);
            pagedViewIcon.applyFromResolveInfo(resolveInfo, this.mPackageManager, mPageViewIconCache, LauncherApplication.getSharedInstance(this.mLauncher).getIconCache(), pageCount > 1);
            switch (AnonymousClass9.$SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[this.mCustomizationType.ordinal()]) {
                case 3:
                    pendingAddItemInfo.itemType = 1;
                    pendingAddItemInfo.componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                    pagedViewIcon.setTag(pendingAddItemInfo);
                    pagedViewIcon.setOnClickListener(this);
                    pagedViewIcon.setOnTouchListener(this);
                    pagedViewIcon.setOnLongClickListener(this);
                    break;
                case 4:
                    pagedViewIcon.setOnClickListener(this);
                    break;
            }
            int i5 = i4 - i3;
            setupPage(pagedViewCellLayout);
            pagedViewCellLayout.addViewToCellLayout(pagedViewIcon, -1, i4, new PagedViewCellLayout.LayoutParams(i5 % this.mCellCountX, i5 / this.mCellCountX, 1, 1));
        }
    }

    private void syncAppPages() {
        if (this.mApps != null) {
            removeAllViews();
            int numPages = (int) Math.ceil((double) (((float) this.mApps.size()) / ((float) (this.mCellCountX * this.mCellCountY))));
            for (int i = 0; i < numPages; i++) {
                PagedViewCellLayout layout = new PagedViewCellLayout(getContext());
                setupPage(layout);
                addView(layout);
            }
        }
    }

    private void syncAppPageItems(int i) {
        boolean z;
        if (this.mApps != null) {
            int pageCount = getPageCount();
            int i2 = this.mCellCountX * this.mCellCountY;
            int i3 = i * i2;
            int min = Math.min(i2 + i3, this.mApps.size());
            PagedViewCellLayout pagedViewCellLayout = (PagedViewCellLayout) getChildAt(i);
            pagedViewCellLayout.removeAllViewsOnPage();
            for (int i4 = i3; i4 < min; i4++) {
                ApplicationInfo applicationInfo = this.mApps.get(i4);
                PagedViewIcon pagedViewIcon = (PagedViewIcon) this.mInflater.inflate(R.layout.all_apps_paged_view_application, pagedViewCellLayout, false);
                PagedViewIconCache pagedViewIconCache = mPageViewIconCache;
                if (pageCount > 1) {
                    z = true;
                } else {
                    z = false;
                }
                pagedViewIcon.applyFromApplicationInfo(applicationInfo, pagedViewIconCache, true, z);
                pagedViewIcon.setOnClickListener(this);
                pagedViewIcon.setOnTouchListener(this);
                pagedViewIcon.setOnLongClickListener(this);
                int i5 = i4 - i3;
                setupPage(pagedViewCellLayout);
                pagedViewCellLayout.addViewToCellLayout(pagedViewIcon, -1, i4, new PagedViewCellLayout.LayoutParams(i5 % this.mCellCountX, i5 / this.mCellCountX, 1, 1));
            }
        }
    }

    /* access modifiers changed from: protected */
    public void invalidatePageData() {
        if (!this.mWaitingToDetermineRowsAndColumns && !this.mWaitingToInitPages && this.mCellCountX > 0 && this.mCellCountY > 0) {
            super.invalidatePageData();
        }
    }

    public void syncPages() {
        int i;
        boolean enforceMinimumPagedWidths = false;
        boolean centerPagedViewCellLayouts = false;
        switch (AnonymousClass9.$SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[this.mCustomizationType.ordinal()]) {
            case 1:
                syncWidgetPages();
                enforceMinimumPagedWidths = true;
                break;
            case 2:
                syncAppPages();
                centerPagedViewCellLayouts = false;
                break;
            case 3:
                syncListPages(this.mShortcutList);
                centerPagedViewCellLayouts = true;
                break;
            case 4:
                syncWallpaperPages();
                enforceMinimumPagedWidths = true;
                break;
            default:
                removeAllViews();
                setCurrentPage(0);
                break;
        }
        final int childCount = getChildCount();
        if (centerPagedViewCellLayouts) {
            if (childCount == 1) {
                ((PagedViewCellLayout) getChildAt(0)).enableCenteredContent(true);
            } else {
                for (int i2 = 0; i2 < childCount; i2++) {
                    ((PagedViewCellLayout) getChildAt(i2)).enableCenteredContent(false);
                }
            }
        }
        if (enforceMinimumPagedWidths) {
            if (childCount > 1) {
                i = this.mMinPageWidth;
            } else {
                i = 0;
            }
            setMinimumWidthOverride(i);
        }
        requestLayout();
        post(new Runnable() {
            public void run() {
                CustomizePagedView.this.setCurrentPage(Math.max(0, Math.min(childCount - 1, CustomizePagedView.this.getCurrentPage())));
                CustomizePagedView.this.forceUpdateAdjacentPagesAlpha();
            }
        });
    }

    public void syncPageItems(int page) {
        switch (AnonymousClass9.$SwitchMap$com$android$launcher2$CustomizePagedView$CustomizationType[this.mCustomizationType.ordinal()]) {
            case 1:
                syncWidgetPageItems(page);
                return;
            case 2:
                syncAppPageItems(page);
                return;
            case 3:
                syncListPageItems(page, this.mShortcutList);
                return;
            case 4:
                syncWallpaperPageItems(page);
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public int getPageContentWidth() {
        return this.mPageContentWidth;
    }

    /* access modifiers changed from: protected */
    public int getAssociatedLowerPageBound(int page) {
        return Math.max(0, page - 2);
    }

    /* access modifiers changed from: protected */
    public int getAssociatedUpperPageBound(int page) {
        return Math.min(page + 2, getChildCount() - 1);
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(this.mChoiceModeTitleText);
        return true;
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    public void onDestroyActionMode(ActionMode mode) {
        endChoiceMode();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }
}
