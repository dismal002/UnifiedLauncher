package com.gingerbread.launcher2;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;

public class LauncherApplication extends Application {
    public LauncherModel mModel;
    public IconCache mIconCache;

    /**
     * Called by GingerAppState to initialise the model from an arbitrary Context.
     */
    public static void initFromContext(Context context) {
        IconCache iconCache = new IconCache(context);
        LauncherModel model = new LauncherModel(context, iconCache);

        IntentFilter pkgFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkgFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pkgFilter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(model, pkgFilter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(model, pkgFilter);
        }

        IntentFilter sysFilter = new IntentFilter();
        sysFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        sysFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(model, sysFilter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(model, sysFilter);
        }

        context.getContentResolver().registerContentObserver(
                LauncherSettings.Favorites.CONTENT_URI, true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        model.startLoader(context, false);
                    }
                });

        GingerState.init(model, iconCache);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Standalone build only — in unified build, GingerAppState calls initFromContext.
        mIconCache = new IconCache(this);
        mModel = new LauncherModel(this, mIconCache);
    }

    LauncherModel setLauncher(Launcher launcher) {
        LauncherModel model = GingerState.getModel();
        if (model != null) {
            model.initialize(launcher);
            return model;
        }
        mModel.initialize(launcher);
        return mModel;
    }

    IconCache getIconCache() {
        IconCache cache = GingerState.getIconCache();
        return cache != null ? cache : mIconCache;
    }

    LauncherModel getModel() {
        LauncherModel model = GingerState.getModel();
        return model != null ? model : mModel;
    }
}
