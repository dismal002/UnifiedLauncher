package com.android.launcher2;

import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;

public class LauncherApplication extends Application {
    private static boolean sIsScreenXLarge;
    private static float sScreenDensity;
    // Shared singleton used when running inside a host app (not as Application)
    private static LauncherApplication sInstance;

    public IconCache mIconCache;
    public LauncherModel mModel;

    /**
     * Returns the singleton LauncherApplication, initialising it from the given
     * Activity context if this class is not the process Application.
     */
    public static LauncherApplication getSharedInstance(Context activityContext) {
        if (sInstance != null) return sInstance;
        // Create a wrapper that delegates Context calls to the activity's app context
        Context appContext = activityContext.getApplicationContext();
        sInstance = new LauncherApplication() {
            @Override public android.content.res.Resources getResources() { return appContext.getResources(); }
            @Override public android.content.pm.PackageManager getPackageManager() { return appContext.getPackageManager(); }
            @Override public android.content.ContentResolver getContentResolver() { return appContext.getContentResolver(); }
            @Override public Object getSystemService(String name) { return appContext.getSystemService(name); }
            @Override public String getPackageName() { return appContext.getPackageName(); }
            @Override public android.content.res.AssetManager getAssets() { return appContext.getAssets(); }
        };
        sIsScreenXLarge = true;
        sScreenDensity = appContext.getResources().getDisplayMetrics().density;
        sInstance.mIconCache = new IconCache(sInstance);
        sInstance.mModel = new LauncherModel(sInstance, sInstance.mIconCache);
        IntentFilter pkgFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        pkgFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        pkgFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        pkgFilter.addDataScheme("package");
        appContext.registerReceiver(sInstance.mModel, pkgFilter);
        IntentFilter sysFilter = new IntentFilter();
        sysFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sysFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        sysFilter.addAction("android.intent.action.LOCALE_CHANGED");
        sysFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        appContext.registerReceiver(sInstance.mModel, sysFilter);
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (sInstance != null) return; // already initialised via getSharedInstance
        sInstance = this;
        sIsScreenXLarge = true;
        sScreenDensity = getResources().getDisplayMetrics().density;
        this.mIconCache = new IconCache(this);
        this.mModel = new LauncherModel(this, this.mIconCache);
        IntentFilter pkgFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        pkgFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        pkgFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        pkgFilter.addDataScheme("package");
        registerReceiver(this.mModel, pkgFilter);
        IntentFilter sysFilter = new IntentFilter();
        sysFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sysFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        sysFilter.addAction("android.intent.action.LOCALE_CHANGED");
        sysFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        registerReceiver(this.mModel, sysFilter);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(this.mModel);
    }

    public LauncherModel setLauncher(Launcher launcher) {
        this.mModel.initialize(launcher);
        return this.mModel;
    }

    public IconCache getIconCache() {
        return this.mIconCache;
    }

    public LauncherModel getModel() {
        return this.mModel;
    }

    public static boolean isInPlaceRotationEnabled() {
        return false;
    }

    public static boolean isScreenXLarge() {
        return sIsScreenXLarge;
    }

    public static float getScreenDensity() {
        return sScreenDensity;
    }
}
