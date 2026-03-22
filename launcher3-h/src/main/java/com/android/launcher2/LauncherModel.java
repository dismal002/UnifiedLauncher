package com.android.launcher2;
import com.launcher3h.R;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.content.ContentProviderClient;
import android.os.SystemClock;
import android.util.Log;
import com.android.launcher2.InstallWidgetReceiver;
import com.android.launcher2.LauncherSettings;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

public class LauncherModel extends BroadcastReceiver {
    public static final Comparator<ApplicationInfo> APP_INSTALL_TIME_COMPARATOR = new Comparator<ApplicationInfo>() {
        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            if (a.firstInstallTime < b.firstInstallTime) {
                return 1;
            }
            if (a.firstInstallTime > b.firstInstallTime) {
                return -1;
            }
            return 0;
        }
    };
    public static final Comparator<ApplicationInfo> APP_NAME_COMPARATOR = new Comparator<ApplicationInfo>() {
        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            return LauncherModel.sCollator.compare(a.title.toString(), b.title.toString());
        }
    };
    /* access modifiers changed from: private */
    public static int mCellCountX;
    /* access modifiers changed from: private */
    public static int mCellCountY;
    protected static Configuration previousConfig = new Configuration();
    /* access modifiers changed from: private */
    public static final Collator sCollator = Collator.getInstance();
    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());
    /* access modifiers changed from: private */
    public AllAppsList mAllAppsList;
    /* access modifiers changed from: private */
    public int mAllAppsLoadDelay;
    /* access modifiers changed from: private */
    public boolean mAllAppsLoaded;
    /* access modifiers changed from: private */
    public boolean mAllowLoadFromCache = false;
    /* access modifiers changed from: private */
    public final LauncherApplication mApp;
    final ArrayList<LauncherAppWidgetInfo> mAppWidgets = new ArrayList<>();
    private final boolean mAppsCanBeOnExternalStorage;
    /* access modifiers changed from: private */
    public int mBatchSize;
    /* access modifiers changed from: private */
    public WeakReference<Callbacks> mCallbacks;
    private Bitmap mDefaultIcon;
    final HashMap<Long, FolderInfo> mFolders = new HashMap<>();
    /* access modifiers changed from: private */
    public DeferredHandler mHandler = new DeferredHandler();
    /* access modifiers changed from: private */
    public IconCache mIconCache;
    final HashMap<Long, ItemInfo> mItemsIdMap = new HashMap<>();
    final HashMap<Object, byte[]> mDbIconCache = new HashMap<>();
    final ArrayList<ItemInfo> mItems = new ArrayList<>();
    /* access modifiers changed from: private */
    public LoaderTask mLoaderTask;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    /* access modifiers changed from: private */
    public boolean mWorkspaceLoaded;

    public interface Callbacks {
        void bindAllApplications(ArrayList<ApplicationInfo> arrayList);

        void bindAppWidget(LauncherAppWidgetInfo launcherAppWidgetInfo);

        void bindAppsAdded(ArrayList<ApplicationInfo> arrayList);

        void bindAppsRemoved(ArrayList<ApplicationInfo> arrayList, boolean z);

        void bindAppsUpdated(ArrayList<ApplicationInfo> arrayList);

        void bindFolders(HashMap<Long, FolderInfo> hashMap);

        void bindItems(ArrayList<ItemInfo> arrayList, int i, int i2);

        void bindPackagesUpdated();

        void finishBindingItems();

        int getCurrentWorkspaceScreen();

        boolean isAllAppsVisible();

        boolean setLoadOnResume();

        void startBinding();
    }

    LauncherModel(LauncherApplication app, IconCache iconCache) {
        this.mAppsCanBeOnExternalStorage = !Environment.isExternalStorageEmulated();
        this.mApp = app;
        this.mAllAppsList = new AllAppsList(iconCache);
        this.mIconCache = iconCache;
        this.mDefaultIcon = Utilities.createIconBitmap(this.mIconCache.getFullResDefaultActivityIcon(), app);
        this.mAllAppsLoadDelay = app.getResources().getInteger(R.integer.config_allAppsBatchLoadDelay);
        this.mBatchSize = app.getResources().getInteger(R.integer.config_allAppsBatchSize);
    }

    public Bitmap getFallbackIcon() {
        return Bitmap.createBitmap(this.mDefaultIcon);
    }

    static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY) {
        if (item.container == -1) {
            addItemToDatabase(context, item, container, screen, cellX, cellY, false);
        } else {
            moveItemInDatabase(context, item, container, screen, cellX, cellY);
        }
    }

    static void moveItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;
        final Uri uri = LauncherSettings.Favorites.getContentUri(item.id, false);
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        values.put("container", Long.valueOf(item.container));
        values.put("cellX", Integer.valueOf(cellX));
        values.put("cellY", Integer.valueOf(cellY));
        values.put("screen", Integer.valueOf(item.screen));
        sWorker.post(new Runnable() {
            public void run() {
                cr.update(uri, values, (String) null, (String[]) null);
            }
        });
    }

    static void resizeItemInDatabase(Context context, ItemInfo item, int cellX, int cellY, int spanX, int spanY) {
        item.spanX = spanX;
        item.spanY = spanY;
        item.cellX = cellX;
        item.cellY = cellY;
        final Uri uri = LauncherSettings.Favorites.getContentUri(item.id, false);
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        values.put("container", Long.valueOf(item.container));
        values.put("spanX", Integer.valueOf(spanX));
        values.put("spanY", Integer.valueOf(spanY));
        values.put("cellX", Integer.valueOf(cellX));
        values.put("cellY", Integer.valueOf(cellY));
        sWorker.post(new Runnable() {
            public void run() {
                cr.update(uri, values, (String) null, (String[]) null);
            }
        });
    }

    static boolean shortcutExists(Context context, String title, Intent intent) {
        Cursor c = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"title", "intent"}, "title=? and intent=?", new String[]{title, intent.toUri(0)}, (String) null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    static ArrayList<ItemInfo> getItemsInLocalCoordinates(Context context) {
        ArrayList<ItemInfo> items = new ArrayList<>();
        Cursor c = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"itemType", "container", "screen", "cellX", "cellY", "spanX", "spanY"}, (String) null, (String[]) null, (String) null);
        int itemTypeIndex = c.getColumnIndexOrThrow("itemType");
        int containerIndex = c.getColumnIndexOrThrow("container");
        int screenIndex = c.getColumnIndexOrThrow("screen");
        int cellXIndex = c.getColumnIndexOrThrow("cellX");
        int cellYIndex = c.getColumnIndexOrThrow("cellY");
        int spanXIndex = c.getColumnIndexOrThrow("spanX");
        int spanYIndex = c.getColumnIndexOrThrow("spanY");
        while (c.moveToNext()) {
            try {
                ItemInfo item = new ItemInfo();
                item.cellX = c.getInt(cellXIndex);
                item.cellY = c.getInt(cellYIndex);
                item.spanX = c.getInt(spanXIndex);
                item.spanY = c.getInt(spanYIndex);
                item.container = (long) c.getInt(containerIndex);
                item.itemType = c.getInt(itemTypeIndex);
                item.screen = c.getInt(screenIndex);
                items.add(item);
            } catch (Exception e) {
                items.clear();
            } finally {
                c.close();
            }
        }
        return items;
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: package-private */
    public FolderInfo getFolderById(Context context, HashMap<Long, FolderInfo> folderList, long id) {
        Cursor c = context.getContentResolver().query(LauncherSettings.Favorites.CONTENT_URI, (String[]) null, "_id=? and (itemType=? or itemType=?)", new String[]{String.valueOf(id), String.valueOf(2), String.valueOf(3)}, (String) null);
        try {
            if (c.moveToFirst()) {
                int itemTypeIndex = c.getColumnIndexOrThrow("itemType");
                int titleIndex = c.getColumnIndexOrThrow("title");
                int containerIndex = c.getColumnIndexOrThrow("container");
                int screenIndex = c.getColumnIndexOrThrow("screen");
                int cellXIndex = c.getColumnIndexOrThrow("cellX");
                int cellYIndex = c.getColumnIndexOrThrow("cellY");
                FolderInfo folderInfo = null;
                switch (c.getInt(itemTypeIndex)) {
                    case 2:
                        folderInfo = findOrMakeUserFolder(folderList, id);
                        break;
                    case 3:
                        folderInfo = findOrMakeLiveFolder(folderList, id);
                        break;
                }
                folderInfo.title = c.getString(titleIndex);
                folderInfo.id = id;
                folderInfo.container = (long) c.getInt(containerIndex);
                folderInfo.screen = c.getInt(screenIndex);
                folderInfo.cellX = c.getInt(cellXIndex);
                folderInfo.cellY = c.getInt(cellYIndex);
                c.close();
                return folderInfo;
            }
            c.close();
            return null;
        } catch (Throwable th) {
            c.close();
            throw th;
        }
    }

    static void addItemToDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY, boolean notify) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;
        ContentValues values = new ContentValues();
        ContentResolver cr = context.getContentResolver();
        item.onAddToDatabase(values);
        item.updateValuesWithCoordinates(values, cellX, cellY);
        Uri result = cr.insert(notify ? LauncherSettings.Favorites.CONTENT_URI : LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values);
        if (result != null) {
            item.id = (long) Integer.parseInt(result.getPathSegments().get(1));
        }
    }

    static int getCellLayoutChildId(int cellId, int screen, int localCellX, int localCellY, int spanX, int spanY) {
        return ((cellId & 255) << 24) | ((screen & 255) << 16) | ((localCellX & 255) << 8) | (localCellY & 255);
    }

    static int getCellCountX() {
        return mCellCountX;
    }

    static int getCellCountY() {
        return mCellCountY;
    }

    static void updateWorkspaceLayoutCells(int shortAxisCellCount, int longAxisCellCount) {
        mCellCountX = shortAxisCellCount;
        mCellCountY = longAxisCellCount;
    }

    static void updateItemInDatabase(Context context, ItemInfo item) {
        ContentValues values = new ContentValues();
        ContentResolver cr = context.getContentResolver();
        item.onAddToDatabase(values);
        item.updateValuesWithCoordinates(values, item.cellX, item.cellY);
        cr.update(LauncherSettings.Favorites.getContentUri(item.id, false), values, (String) null, (String[]) null);
    }

    static void deleteItemFromDatabase(Context context, ItemInfo item) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uriToDelete = LauncherSettings.Favorites.getContentUri(item.id, false);
        sWorker.post(new Runnable() {
            public void run() {
                cr.delete(uriToDelete, (String) null, (String[]) null);
            }
        });
    }

    static void deleteUserFolderContentsFromDatabase(Context context, UserFolderInfo info) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(LauncherSettings.Favorites.getContentUri(info.id, false), (String) null, (String[]) null);
        cr.delete(LauncherSettings.Favorites.CONTENT_URI, "container=" + info.id, (String[]) null);
    }

    public void initialize(Callbacks callbacks) {
        synchronized (this.mLock) {
            this.mCallbacks = new WeakReference<>(callbacks);
        }
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.PACKAGE_CHANGED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
            int op = 0;
            if (packageName != null && packageName.length() != 0) {
                if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                    op = 2;
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    if (!replacing) {
                        op = 3;
                    }
                } else if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                    op = !replacing ? 1 : 2;
                }
                if (op != 0) {
                    enqueuePackageUpdated(new PackageUpdatedTask(op, new String[]{packageName}));
                }
            }
        } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
            enqueuePackageUpdated(new PackageUpdatedTask(1, intent.getStringArrayExtra("android.intent.extra.changed_package_list")));
            setAllowLoadFromCache();
            startLoaderFromBackground();
        } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
            enqueuePackageUpdated(new PackageUpdatedTask(4, intent.getStringArrayExtra("android.intent.extra.changed_package_list")));
        } else if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
            this.mAllAppsLoaded = false;
            startLoaderFromBackground();
        } else if ("android.intent.action.CONFIGURATION_CHANGED".equals(action)) {
            Configuration currentConfig = context.getResources().getConfiguration();
            if (currentConfig.mcc != previousConfig.mcc) {
                this.mAllAppsLoaded = false;
                startLoaderFromBackground();
            }
            previousConfig = currentConfig;
        }
    }

    public void startLoaderFromBackground() {
        Callbacks callbacks;
        boolean runLoader = false;
        if (!(this.mCallbacks == null || (callbacks = this.mCallbacks.get()) == null || callbacks.setLoadOnResume())) {
            runLoader = true;
        }
        if (runLoader) {
            startLoader(this.mApp, false);
        }
    }

    public void startLoader(Context context, boolean isLaunching) {
        synchronized (this.mLock) {
            if (!(this.mCallbacks == null || this.mCallbacks.get() == null)) {
                LoaderTask oldTask = this.mLoaderTask;
                if (oldTask != null) {
                    if (oldTask.isLaunching()) {
                        isLaunching = true;
                    }
                    oldTask.stopLocked();
                }
                this.mLoaderTask = new LoaderTask(context, isLaunching);
                sWorker.post(this.mLoaderTask);
            }
        }
    }

    public void stopLoader() {
        synchronized (this.mLock) {
            if (this.mLoaderTask != null) {
                this.mLoaderTask.stopLocked();
            }
        }
        this.mItems.clear();
        this.mAppWidgets.clear();
        this.mFolders.clear();
    }

    private class LoaderTask implements Runnable {
        private Context mContext;
        private boolean mIsLaunching;
        /* access modifiers changed from: private */
        public boolean mLoadAndBindStepFinished;
        private boolean mStopped;
        private Thread mWaitThread;

        LoaderTask(Context context, boolean isLaunching) {
            this.mContext = context;
            this.mIsLaunching = isLaunching;
        }

        /* access modifiers changed from: package-private */
        public boolean isLaunching() {
            return this.mIsLaunching;
        }

        private void loadAndBindWorkspace() {
            loadWorkspace();
            if (!this.mStopped) {
                boolean unused = LauncherModel.this.mWorkspaceLoaded = true;
                bindWorkspace();
            }
        }

        private void waitForIdle() {
            synchronized (this) {
                LauncherModel.this.mHandler.postIdle(new Runnable() {
                    public void run() {
                        synchronized (LoaderTask.this) {
                            boolean unused = LoaderTask.this.mLoadAndBindStepFinished = true;
                            LoaderTask.this.notify();
                        }
                    }
                });
                while (!this.mStopped && !this.mLoadAndBindStepFinished) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        public void run() {
            int i;
            Callbacks cbk = (Callbacks) LauncherModel.this.mCallbacks.get();
            boolean loadWorkspaceFirst = cbk != null ? !cbk.isAllAppsVisible() : true;
            synchronized (LauncherModel.this.mLock) {
                if (this.mIsLaunching) {
                    i = 0;
                } else {
                    i = 10;
                }
                Process.setThreadPriority(i);
            }
            if (loadWorkspaceFirst) {
                loadAndBindWorkspace();
            } else {
                loadAndBindAllApps();
            }
            if (!this.mStopped) {
                synchronized (LauncherModel.this.mLock) {
                    if (this.mIsLaunching) {
                        Process.setThreadPriority(10);
                    }
                }
                waitForIdle();
                if (loadWorkspaceFirst) {
                    loadAndBindAllApps();
                } else {
                    loadAndBindWorkspace();
                }
            }
            this.mContext = null;
            synchronized (LauncherModel.this.mLock) {
                if (LauncherModel.this.mLoaderTask == this) {
                    LoaderTask unused = LauncherModel.this.mLoaderTask = null;
                }
            }
            if (this.mStopped) {
                LauncherModel.this.mHandler.post(new Runnable() {
                    public void run() {
                        System.gc();
                    }
                });
            } else {
                LauncherModel.this.mHandler.postIdle(new Runnable() {
                    public void run() {
                        System.gc();
                    }
                });
            }
        }

        public void stopLocked() {
            synchronized (this) {
                this.mStopped = true;
                notify();
            }
        }

        /* access modifiers changed from: package-private */
        public Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
            synchronized (LauncherModel.this.mLock) {
                if (this.mStopped) {
                    return null;
                }
                if (LauncherModel.this.mCallbacks == null) {
                    return null;
                }
                Callbacks callbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                }
                if (callbacks != null) {
                    return callbacks;
                }
                Log.w("Launcher.Model", "no mCallbacks");
                return null;
            }
        }

        private boolean checkItemPlacement(ItemInfo[][][] occupied, ItemInfo item) {
            if (item.container != -100) {
                return true;
            }
            if (item.screen < 0 || item.screen >= occupied.length) return false;
            int lenX = occupied[item.screen].length;
            if (lenX == 0) return false;
            int lenY = occupied[item.screen][0].length;
            
            for (int x = item.cellX; x < item.cellX + item.spanX; x++) {
                for (int y = item.cellY; y < item.cellY + item.spanY; y++) {
                    if (x < 0 || y < 0 || x >= lenX || y >= lenY) {
                         Log.w("Launcher.Model", "Item out of bounds: " + item);
                         return false;
                    }
                    if (occupied[item.screen][x][y] != null) {
                        Log.e("Launcher.Model", "Error loading shortcut " + item + " into cell occupied by " + occupied[item.screen][x][y]);
                        return false;
                    }
                }
            }
            for (int x2 = item.cellX; x2 < item.cellX + item.spanX; x2++) {
                for (int y2 = item.cellY; y2 < item.cellY + item.spanY; y2++) {
                    if (x2 >= 0 && y2 >= 0 && x2 < lenX && y2 < lenY) {
                        occupied[item.screen][x2][y2] = item;
                    }
                }
            }
            return true;
        }

        /* Debug info: failed to restart local var, previous not found, register: 60 */
        /* JADX WARNING: Can't fix incorrect switch cases order */
        /* Code decompiled incorrectly, please refer to instructions dump. */
private void loadWorkspace() {
            final long t = false ? SystemClock.uptimeMillis() : 0;

            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager manager = context.getPackageManager();
            final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            final boolean isSafeMode = manager.isSafeMode();

            LauncherModel.this.mItems.clear();
            LauncherModel.this.mAppWidgets.clear();
            LauncherModel.this.mFolders.clear();
            mItemsIdMap.clear();
            mDbIconCache.clear();

            final ArrayList<Long> itemsToRemove = new ArrayList<Long>();

            final Cursor c = contentResolver.query(
                    LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);

            // +1 for the hotseat (it can be larger than the workspace)
            // Load workspace in reverse order to ensure that latest items are loaded first (and
            // before any earlier duplicates)
            final ItemInfo occupied[][][] =
                    new ItemInfo[5 + 1][mCellCountX + 1][mCellCountY + 1];

            try {
                final int idIndex = c.getColumnIndexOrThrow("_id");
                final int intentIndex = c.getColumnIndexOrThrow
                        ("intent");
                final int titleIndex = c.getColumnIndexOrThrow
                        ("title");
                final int iconTypeIndex = c.getColumnIndexOrThrow(
                        "iconType");
                final int iconIndex = c.getColumnIndexOrThrow("icon");
                final int iconPackageIndex = c.getColumnIndexOrThrow(
                        "iconPackage");
                final int iconResourceIndex = c.getColumnIndexOrThrow(
                        "iconResource");
                final int containerIndex = c.getColumnIndexOrThrow(
                        "container");
                final int itemTypeIndex = c.getColumnIndexOrThrow(
                        "itemType");
                final int appWidgetIdIndex = c.getColumnIndexOrThrow(
                        "appWidgetId");
                final int screenIndex = c.getColumnIndexOrThrow(
                        "screen");
                final int cellXIndex = c.getColumnIndexOrThrow
                        ("cellX");
                final int cellYIndex = c.getColumnIndexOrThrow
                        ("cellY");
                final int spanXIndex = c.getColumnIndexOrThrow
                        ("spanX");
                final int spanYIndex = c.getColumnIndexOrThrow(
                        "spanY");
                final int uriIndex = c.getColumnIndexOrThrow("uri");
                final int displayModeIndex = c.getColumnIndexOrThrow(
                        "displayMode");

                ShortcutInfo info;
                String intentDescription;
                LauncherAppWidgetInfo appWidgetInfo;
                int container;
                long id;
                Intent intent;

                while (!mStopped && c.moveToNext()) {
                    try {
                        int itemType = c.getInt(itemTypeIndex);

                        switch (itemType) {
                        case 0:
                        case 1:
                            intentDescription = c.getString(intentIndex);
                            try {
                                intent = Intent.parseUri(intentDescription, 0);
                            } catch (URISyntaxException e) {
                                continue;
                            }

                            if (itemType == 0) {
                                info = getShortcutInfo(manager, intent, context, c, iconIndex,
                                        titleIndex);
                            } else {
                                info = getShortcutInfo(c, context, iconTypeIndex,
                                        iconPackageIndex, iconResourceIndex, iconIndex,
                                        titleIndex);
                            }

                            if (info != null) {
                                info.intent = intent;
                                info.id = c.getLong(idIndex);
                                container = c.getInt(containerIndex);
                                info.container = container;
                                info.screen = c.getInt(screenIndex);
                                info.cellX = c.getInt(cellXIndex);
                                info.cellY = c.getInt(cellYIndex);

                                // check & update map of what's occupied
                                if (!checkItemPlacement(occupied, info)) {
                                    break;
                                }

                                switch (container) {
                                case -100:
                                case -101:
                                    LauncherModel.this.mItems.add(info);
                                    break;
                                default:
                                    // Item is in a user folder
                                    UserFolderInfo folderInfo =
                                            findOrMakeUserFolder(LauncherModel.this.mFolders, container);
                                    folderInfo.add(info);
                                    break;
                                }
                                mItemsIdMap.put(info.id, info);

                                // now that we've loaded everthing re-save it with the
                                // icon in case it disappears somehow.
                                
                            } else {
                                // Failed to load the shortcut, probably because the
                                // activity manager couldn't resolve it (maybe the app
                                // was uninstalled), or the db row was somehow screwed up.
                                // Delete it.
                                id = c.getLong(idIndex);
                                Log.e("Launcher.Model", "Error loading shortcut " + id + ", removing it");
                                contentResolver.delete(LauncherSettings.Favorites.getContentUri(
                                            id, false), null, null);
                            }
                            break;

                        case 2:
                            id = c.getLong(idIndex);
                            UserFolderInfo folderInfo = findOrMakeUserFolder(LauncherModel.this.mFolders, id);

                            folderInfo.title = c.getString(titleIndex);
                            folderInfo.id = id;
                            container = c.getInt(containerIndex);
                            folderInfo.container = container;
                            folderInfo.screen = c.getInt(screenIndex);
                            folderInfo.cellX = c.getInt(cellXIndex);
                            folderInfo.cellY = c.getInt(cellYIndex);

                            // check & update map of what's occupied
                            if (!checkItemPlacement(occupied, folderInfo)) {
                                break;
                            }
                            switch (container) {
                                case -100:
                                case -101:
                                    LauncherModel.this.mItems.add(folderInfo);
                                    break;
                            }

                            mItemsIdMap.put(folderInfo.id, folderInfo);
                            LauncherModel.this.mFolders.put(folderInfo.id, folderInfo);
                            break;

                        case 4:
                            // Read all Launcher-specific widget details
                            int appWidgetId = c.getInt(appWidgetIdIndex);
                            id = c.getLong(idIndex);

                            final AppWidgetProviderInfo provider =
                                    widgets.getAppWidgetInfo(appWidgetId);

                            if (!isSafeMode && (provider == null || provider.provider == null ||
                                    provider.provider.getPackageName() == null)) {
                                String log = "Deleting widget that isn't installed anymore: id="
                                    + id + " appWidgetId=" + appWidgetId;
                                Log.e("Launcher.Model", log); 
                                
                                itemsToRemove.add(id);
                            } else {
                                appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId);
                                appWidgetInfo.id = id;
                                appWidgetInfo.screen = c.getInt(screenIndex);
                                appWidgetInfo.cellX = c.getInt(cellXIndex);
                                appWidgetInfo.cellY = c.getInt(cellYIndex);
                                appWidgetInfo.spanX = c.getInt(spanXIndex);
                                appWidgetInfo.spanY = c.getInt(spanYIndex);

                                container = c.getInt(containerIndex);
                                if (container != -100 &&
                                    container != -101) {
                                    Log.e("Launcher.Model", "Widget found where container "
                                        + "!= CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
                                    continue;
                                }
                                appWidgetInfo.container = c.getInt(containerIndex);

                                // check & update map of what's occupied
                                if (!checkItemPlacement(occupied, appWidgetInfo)) {
                                    break;
                                }
                                mItemsIdMap.put(appWidgetInfo.id, appWidgetInfo);
                                LauncherModel.this.mAppWidgets.add(appWidgetInfo);
                            }
                            break;
                        }
                    } catch (Exception e) {
                        Log.w("Launcher.Model", "Desktop items loading interrupted:", e);
                    }
                }
            } finally {
                c.close();
            }

            if (itemsToRemove.size() > 0) {
                ContentProviderClient client = contentResolver.acquireContentProviderClient(
                                LauncherSettings.Favorites.CONTENT_URI);
                // Remove dead items
                for (long id : itemsToRemove) {
                    if (false) {
                        Log.d("Launcher.Model", "Removed id = " + id);
                    }
                    // Don't notify content observers
                    try {
                        client.delete(LauncherSettings.Favorites.getContentUri(id, false),
                                null, null);
                    } catch (RemoteException e) {
                        Log.w("Launcher.Model", "Could not remove id = " + id);
                    }
                }
            }

            if (false) {
                Log.d("Launcher.Model", "loaded workspace in " + (SystemClock.uptimeMillis()-t) + "ms");
                Log.d("Launcher.Model", "workspace layout: ");
                for (int y = 0; y < mCellCountY; y++) {
                    String line = "";
                    for (int s = 0; s < 5; s++) {
                        if (s > 0) {
                            line += " | ";
                        }
                        for (int x = 0; x < mCellCountX; x++) {
                            line += ((occupied[s][x][y] != null) ? "#" : ".");
                        }
                    }
                    Log.d("Launcher.Model", "[ " + line + " ]");
                }
            }
        }

        private void bindWorkspace() {
            final long t = SystemClock.uptimeMillis();
            final Callbacks oldCallbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
            if (oldCallbacks == null) {
                Log.w("Launcher.Model", "LoaderTask running with no launcher");
                return;
            }
            LauncherModel.this.mHandler.post(new Runnable() {
                public void run() {
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            });
            int N = LauncherModel.this.mItems.size();
            for (int i = 0; i < N; i += 6) {
                final int start = i;
                final int chunkSize = i + 6 <= N ? 6 : N - i;
                LauncherModel.this.mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindItems(LauncherModel.this.mItems, start, start + chunkSize);
                        }
                    }
                });
            }
            LauncherModel.this.mHandler.post(new Runnable() {
                public void run() {
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindFolders(LauncherModel.this.mFolders);
                    }
                }
            });
            LauncherModel.this.mHandler.post(new Runnable() {
                public void run() {
                }
            });
            int currentScreen = oldCallbacks.getCurrentWorkspaceScreen();
            int N2 = LauncherModel.this.mAppWidgets.size();
            for (int i2 = 0; i2 < N2; i2++) {
                final LauncherAppWidgetInfo widget = LauncherModel.this.mAppWidgets.get(i2);
                if (widget.screen == currentScreen) {
                    LauncherModel.this.mHandler.post(new Runnable() {
                        public void run() {
                            Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                            if (callbacks != null) {
                                callbacks.bindAppWidget(widget);
                            }
                        }
                    });
                }
            }
            for (int i3 = 0; i3 < N2; i3++) {
                final LauncherAppWidgetInfo widget2 = LauncherModel.this.mAppWidgets.get(i3);
                if (widget2.screen != currentScreen) {
                    LauncherModel.this.mHandler.post(new Runnable() {
                        public void run() {
                            Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                            if (callbacks != null) {
                                callbacks.bindAppWidget(widget2);
                            }
                        }
                    });
                }
            }
            LauncherModel.this.mHandler.post(new Runnable() {
                public void run() {
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.finishBindingItems();
                    }
                }
            });
            LauncherModel.this.mHandler.post(new Runnable() {
                public void run() {
                }
            });
        }

        private void loadAndBindAllApps() {
            if (!LauncherModel.this.mAllowLoadFromCache || !LauncherModel.this.mAllAppsLoaded) {
                loadAllAppsByBatch();
                if (!this.mStopped) {
                    boolean unused = LauncherModel.this.mAllAppsLoaded = true;
                    return;
                }
                return;
            }
            boolean unused2 = LauncherModel.this.mAllowLoadFromCache = false;
            onlyBindAllApps();
        }

        private void onlyBindAllApps() {
            final Callbacks oldCallbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
            if (oldCallbacks == null) {
                Log.w("Launcher.Model", "LoaderTask running with no launcher (onlyBindAllApps)");
                return;
            }
            final ArrayList<ApplicationInfo> list = (ArrayList) LauncherModel.this.mAllAppsList.data.clone();
            LauncherModel.this.mHandler.post(new Runnable() {
                public void run() {
                    long uptimeMillis = SystemClock.uptimeMillis();
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAllApplications(list);
                    }
                }
            });
        }

        private void loadAllAppsByBatch() {
            Callbacks oldCallbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
            if (oldCallbacks == null) {
                Log.w("Launcher.Model", "LoaderTask running with no launcher (loadAllAppsByBatch)");
                return;
            }
            Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
            intent.addCategory("android.intent.category.LAUNCHER");
            PackageManager packageManager = this.mContext.getPackageManager();
            List<ResolveInfo> apps = null;
            int N = Integer.MAX_VALUE;
            int i = 0;
            int batchSize = -1;
            while (i < N && !this.mStopped) {
                if (i == 0) {
                    LauncherModel.this.mAllAppsList.clear();
                    apps = packageManager.queryIntentActivities(intent, 0);
                    if (apps != null && (N = apps.size()) != 0) {
                        if (LauncherModel.this.mBatchSize == 0) {
                            batchSize = N;
                        } else {
                            batchSize = LauncherModel.this.mBatchSize;
                        }
                        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(packageManager));
                    } else {
                        return;
                    }
                }
                int i2 = i;
                int j = 0;
                while (i < N && j < batchSize) {
                    LauncherModel.this.mAllAppsList.add(new ApplicationInfo(packageManager, apps.get(i), LauncherModel.this.mIconCache));
                    i++;
                    j++;
                }
                boolean first = i <= batchSize;
                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                ArrayList<ApplicationInfo> added = LauncherModel.this.mAllAppsList.added;
                LauncherModel.this.mAllAppsList.added = new ArrayList<>();
                final Callbacks callbacks2 = callbacks;
                final boolean z = first;
                final ArrayList<ApplicationInfo> arrayList = added;
                LauncherModel.this.mHandler.post(new Runnable() {
                    public void run() {
                        long uptimeMillis = SystemClock.uptimeMillis();
                        if (callbacks2 == null) {
                            Log.i("Launcher.Model", "not binding apps: no Launcher activity");
                        } else if (z) {
                            callbacks2.bindAllApplications(arrayList);
                        } else {
                            callbacks2.bindAppsAdded(arrayList);
                        }
                    }
                });
                if (LauncherModel.this.mAllAppsLoadDelay > 0 && i < N) {
                    try {
                        Thread.sleep((long) LauncherModel.this.mAllAppsLoadDelay);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        public void dumpState() {
            Log.d("Launcher.Model", "mLoaderTask.mContext=" + this.mContext);
            Log.d("Launcher.Model", "mLoaderTask.mWaitThread=" + this.mWaitThread);
            Log.d("Launcher.Model", "mLoaderTask.mIsLaunching=" + this.mIsLaunching);
            Log.d("Launcher.Model", "mLoaderTask.mStopped=" + this.mStopped);
            Log.d("Launcher.Model", "mLoaderTask.mLoadAndBindStepFinished=" + this.mLoadAndBindStepFinished);
        }
    }

    /* access modifiers changed from: package-private */
    public void enqueuePackageUpdated(PackageUpdatedTask task) {
        sWorker.post(task);
    }

    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;

        public PackageUpdatedTask(int op, String[] packages) {
            this.mOp = op;
            this.mPackages = packages;
        }

        public void run() {
            boolean permanent;
            LauncherApplication access$1800 = LauncherModel.this.mApp;
            String[] packages = this.mPackages;
            switch (this.mOp) {
                case 1:
                    for (String addPackage : packages) {
                        LauncherModel.this.mAllAppsList.addPackage(access$1800, addPackage);
                    }
                    break;
                case 2:
                    for (String updatePackage : packages) {
                        LauncherModel.this.mAllAppsList.updatePackage(access$1800, updatePackage);
                    }
                    break;
                case 3:
                case 4:
                    for (String removePackage : packages) {
                        LauncherModel.this.mAllAppsList.removePackage(removePackage);
                    }
                    break;
            }
            ArrayList<ApplicationInfo> added = null;
            ArrayList<ApplicationInfo> removed = null;
            ArrayList<ApplicationInfo> modified = null;
            if (LauncherModel.this.mAllAppsList.added.size() > 0) {
                added = LauncherModel.this.mAllAppsList.added;
                LauncherModel.this.mAllAppsList.added = new ArrayList<>();
            }
            if (LauncherModel.this.mAllAppsList.removed.size() > 0) {
                removed = LauncherModel.this.mAllAppsList.removed;
                LauncherModel.this.mAllAppsList.removed = new ArrayList<>();
                Iterator<ApplicationInfo> it = removed.iterator();
                while (it.hasNext()) {
                    LauncherModel.this.mIconCache.remove(it.next().intent.getComponent());
                }
            }
            if (LauncherModel.this.mAllAppsList.modified.size() > 0) {
                modified = LauncherModel.this.mAllAppsList.modified;
                LauncherModel.this.mAllAppsList.modified = new ArrayList<>();
            }
            Callbacks callbacks = LauncherModel.this.mCallbacks != null ? (Callbacks) LauncherModel.this.mCallbacks.get() : null;
            if (callbacks == null) {
                Log.w("Launcher.Model", "Nobody to tell about the new app.  Launcher is probably loading.");
                return;
            }
            if (added != null) {
                final Callbacks callbacks2 = callbacks;
                final ArrayList<ApplicationInfo> arrayList = added;
                LauncherModel.this.mHandler.post(new Runnable() {
                    public void run() {
                        if (callbacks2 == LauncherModel.this.mCallbacks.get()) {
                            callbacks2.bindAppsAdded(arrayList);
                        }
                    }
                });
            }
            if (modified != null) {
                final Callbacks callbacks3 = callbacks;
                final ArrayList<ApplicationInfo> arrayList2 = modified;
                LauncherModel.this.mHandler.post(new Runnable() {
                    public void run() {
                        if (callbacks3 == LauncherModel.this.mCallbacks.get()) {
                            callbacks3.bindAppsUpdated(arrayList2);
                        }
                    }
                });
            }
            if (removed != null) {
                if (this.mOp != 4) {
                    permanent = true;
                } else {
                    permanent = false;
                }
                final Callbacks callbacks4 = callbacks;
                final ArrayList<ApplicationInfo> arrayList3 = removed;
                final boolean z = permanent;
                LauncherModel.this.mHandler.post(new Runnable() {
                    public void run() {
                        if (callbacks4 == LauncherModel.this.mCallbacks.get()) {
                            callbacks4.bindAppsRemoved(arrayList3, z);
                        }
                    }
                });
            }
            final Callbacks callbacks5 = callbacks;
            LauncherModel.this.mHandler.post(new Runnable() {
                public void run() {
                    if (callbacks5 == LauncherModel.this.mCallbacks.get()) {
                        callbacks5.bindPackagesUpdated();
                    }
                }
            });
        }
    }

    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context) {
        return getShortcutInfo(manager, intent, context, (Cursor) null, -1, -1);
    }

    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context, Cursor c, int iconIndex, int titleIndex) {
        Bitmap icon = null;
        ShortcutInfo info = new ShortcutInfo();
        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            return null;
        }
        ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);
        if (resolveInfo != null) {
            icon = this.mIconCache.getIcon(componentName, resolveInfo);
        }
        if (icon == null && c != null) {
            icon = getIconFromCursor(c, iconIndex);
        }
        if (icon == null) {
            icon = getFallbackIcon();
            info.usingFallbackIcon = true;
        }
        info.setIcon(icon);
        if (resolveInfo != null) {
            info.title = resolveInfo.activityInfo.loadLabel(manager);
        }
        if (info.title == null && c != null) {
            info.title = c.getString(titleIndex);
        }
        if (info.title == null) {
            info.title = componentName.getClassName();
        }
        info.itemType = 0;
        return info;
    }

    /* access modifiers changed from: private */
    public ShortcutInfo getShortcutInfo(Cursor c, Context context, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex, int titleIndex) {
        Bitmap icon = null;
        ShortcutInfo info = new ShortcutInfo();
        info.itemType = 1;
        info.title = c.getString(titleIndex);
        switch (c.getInt(iconTypeIndex)) {
            case 0:
                String packageName = c.getString(iconPackageIndex);
                String resourceName = c.getString(iconResourceIndex);
                PackageManager packageManager = context.getPackageManager();
                info.customIcon = false;
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    if (resources != null) {
                        icon = Utilities.createIconBitmap(this.mIconCache.getFullResIcon(resources, resources.getIdentifier(resourceName, (String) null, (String) null)), context);
                    }
                } catch (Exception e) {
                }
                if (icon == null) {
                    icon = getIconFromCursor(c, iconIndex);
                }
                if (icon == null) {
                    icon = getFallbackIcon();
                    info.usingFallbackIcon = true;
                    break;
                }
                break;
            case 1:
                icon = getIconFromCursor(c, iconIndex);
                if (icon != null) {
                    info.customIcon = true;
                    break;
                } else {
                    icon = getFallbackIcon();
                    info.customIcon = false;
                    info.usingFallbackIcon = true;
                    break;
                }
            default:
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
                info.customIcon = false;
                break;
        }
        info.setIcon(icon);
        return info;
    }

    /* access modifiers changed from: package-private */
    public Bitmap getIconFromCursor(Cursor c, int iconIndex) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            Exception exc = e;
            return null;
        }
    }

    /* access modifiers changed from: package-private */
    public ShortcutInfo addShortcut(Context context, Intent data, int screen, int cellX, int cellY, boolean notify) {
        ShortcutInfo info = infoFromShortcutIntent(context, data, (Bitmap) null);
        addItemToDatabase(context, info, -100, screen, cellX, cellY, notify);
        return info;
    }

    /* access modifiers changed from: package-private */
    public List<InstallWidgetReceiver.WidgetMimeTypeHandlerData> resolveWidgetsForMimeType(Context context, String mimeType) {
        PackageManager packageManager = context.getPackageManager();
        List<InstallWidgetReceiver.WidgetMimeTypeHandlerData> supportedConfigurationActivities = new ArrayList<>();
        Intent supportsIntent = new Intent("com.android.launcher.action.SUPPORTS_CLIPDATA_MIMETYPE");
        supportsIntent.setType(mimeType);
        List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(context).getInstalledProviders();
        HashMap<ComponentName, AppWidgetProviderInfo> configurationComponentToWidget = new HashMap<>();
        for (AppWidgetProviderInfo info : widgets) {
            configurationComponentToWidget.put(info.configure, info);
        }
        for (ResolveInfo info2 : packageManager.queryIntentActivities(supportsIntent, 65536)) {
            ActivityInfo activityInfo = info2.activityInfo;
            ComponentName infoComponent = new ComponentName(activityInfo.packageName, activityInfo.name);
            if (configurationComponentToWidget.containsKey(infoComponent)) {
                supportedConfigurationActivities.add(new InstallWidgetReceiver.WidgetMimeTypeHandlerData(info2, configurationComponentToWidget.get(infoComponent)));
            }
        }
        return supportedConfigurationActivities;
    }

    /* access modifiers changed from: package-private */
    public ShortcutInfo infoFromShortcutIntent(Context context, Intent data, Bitmap fallbackIcon) {
        Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        Parcelable bitmap = data.getParcelableExtra("android.intent.extra.shortcut.ICON");
        Bitmap icon = null;
        boolean customIcon = false;
        Intent.ShortcutIconResource iconResource = null;
        if (bitmap == null || !(bitmap instanceof Bitmap)) {
            Parcelable extra = data.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
            if (extra != null && (extra instanceof Intent.ShortcutIconResource)) {
                try {
                    iconResource = (Intent.ShortcutIconResource) extra;
                    Resources resources = context.getPackageManager().getResourcesForApplication(iconResource.packageName);
                    icon = Utilities.createIconBitmap(this.mIconCache.getFullResIcon(resources, resources.getIdentifier(iconResource.resourceName, (String) null, (String) null)), context);
                } catch (Exception e) {
                    Exception exc = e;
                    Log.w("Launcher.Model", "Could not load shortcut icon: " + extra);
                }
            }
        } else {
            icon = Utilities.createIconBitmap(new FastBitmapDrawable((Bitmap) bitmap), context);
            customIcon = true;
        }
        ShortcutInfo info = new ShortcutInfo();
        if (icon == null) {
            if (fallbackIcon != null) {
                icon = fallbackIcon;
            } else {
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
            }
        }
        info.setIcon(icon);
        info.title = name;
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;
        return info;
    }

    /* access modifiers changed from: private */
    public void loadLiveFolderIcon(Context context, Cursor c, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, LiveFolderInfo liveFolderInfo) {
        switch (c.getInt(iconTypeIndex)) {
            case 0:
                String packageName = c.getString(iconPackageIndex);
                String resourceName = c.getString(iconResourceIndex);
                try {
                    Resources appResources = context.getPackageManager().getResourcesForApplication(packageName);
                    liveFolderInfo.icon = Utilities.createIconBitmap(this.mIconCache.getFullResIcon(appResources, appResources.getIdentifier(resourceName, (String) null, (String) null)), context);
                } catch (Exception e) {
                    Exception exc = e;
                    liveFolderInfo.icon = Utilities.createIconBitmap(this.mIconCache.getFullResIcon(context.getResources(), R.drawable.ic_launcher_folder), context);
                }
                liveFolderInfo.iconResource = new Intent.ShortcutIconResource();
                liveFolderInfo.iconResource.packageName = packageName;
                liveFolderInfo.iconResource.resourceName = resourceName;
                return;
            default:
                liveFolderInfo.icon = Utilities.createIconBitmap(this.mIconCache.getFullResIcon(context.getResources(), R.drawable.ic_launcher_folder), context);
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public void updateSavedIcon(Context context, ShortcutInfo info, Cursor c, int iconIndex) {
        boolean needSave;
        if (this.mAppsCanBeOnExternalStorage && !info.customIcon && !info.usingFallbackIcon) {
            byte[] data = c.getBlob(iconIndex);
            if (data != null) {
                try {
                    if (!BitmapFactory.decodeByteArray(data, 0, data.length).sameAs(info.getIcon(this.mIconCache))) {
                        needSave = true;
                    } else {
                        needSave = false;
                    }
                } catch (Exception e) {
                    Exception exc = e;
                    needSave = true;
                }
            } else {
                needSave = true;
            }
            if (needSave) {
                Log.d("Launcher.Model", "going to save icon bitmap for info=" + info);
                updateItemInDatabase(context, info);
            }
        }
    }

    /* access modifiers changed from: private */
    public static UserFolderInfo findOrMakeUserFolder(HashMap<Long, FolderInfo> folders, long id) {
        FolderInfo folderInfo = folders.get(Long.valueOf(id));
        if (folderInfo == null || !(folderInfo instanceof UserFolderInfo)) {
            folderInfo = new UserFolderInfo();
            folders.put(Long.valueOf(id), folderInfo);
        }
        return (UserFolderInfo) folderInfo;
    }

    /* access modifiers changed from: private */
    public static LiveFolderInfo findOrMakeLiveFolder(HashMap<Long, FolderInfo> folders, long id) {
        FolderInfo folderInfo = folders.get(Long.valueOf(id));
        if (folderInfo == null || !(folderInfo instanceof LiveFolderInfo)) {
            folderInfo = new LiveFolderInfo();
            folders.put(Long.valueOf(id), folderInfo);
        }
        return (LiveFolderInfo) folderInfo;
    }

    public void dumpState() {
        Log.d("Launcher.Model", "mCallbacks=" + this.mCallbacks);
        ApplicationInfo.dumpApplicationInfoList("Launcher.Model", "mAllAppsList.data", this.mAllAppsList.data);
        ApplicationInfo.dumpApplicationInfoList("Launcher.Model", "mAllAppsList.added", this.mAllAppsList.added);
        ApplicationInfo.dumpApplicationInfoList("Launcher.Model", "mAllAppsList.removed", this.mAllAppsList.removed);
        ApplicationInfo.dumpApplicationInfoList("Launcher.Model", "mAllAppsList.modified", this.mAllAppsList.modified);
        Log.d("Launcher.Model", "mItems size=" + this.mItems.size());
        if (this.mLoaderTask != null) {
            this.mLoaderTask.dumpState();
        } else {
            Log.d("Launcher.Model", "mLoaderTask=null");
        }
    }

    public void setAllowLoadFromCache() {
        this.mAllowLoadFromCache = true;
    }
}
