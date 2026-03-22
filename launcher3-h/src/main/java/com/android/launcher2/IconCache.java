package com.android.launcher2;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.android.launcher2.Utilities;
import java.util.HashMap;

public class IconCache {
    private final Utilities.BubbleText mBubble;
    private final HashMap<ComponentName, CacheEntry> mCache = new HashMap<>(50);
    private final LauncherApplication mContext;
    private final Bitmap mDefaultIcon;
    private int mIconDpi;
    private final PackageManager mPackageManager;

    private static class CacheEntry {
        public Bitmap icon;
        public String title;
        public Bitmap titleBitmap;

        private CacheEntry() {
        }
    }

    public IconCache(LauncherApplication context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mBubble = new Utilities.BubbleText(context);
        if (LauncherApplication.isScreenXLarge()) {
            this.mIconDpi = 240;
        } else {
            this.mIconDpi = context.getResources().getDisplayMetrics().densityDpi;
        }
        this.mDefaultIcon = makeDefaultIcon();
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), 17629184);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) throws Resources.NotFoundException {
        return resources.getDrawableForDensity(iconId, this.mIconDpi);
    }

    public Drawable getFullResIcon(ResolveInfo info, PackageManager packageManager) throws Resources.NotFoundException {
        Resources resources;
        int iconId;
        try {
            resources = packageManager.getResourcesForApplication(info.activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            PackageManager.NameNotFoundException nameNotFoundException = e;
            resources = null;
        }
        if (resources == null || (iconId = info.activityInfo.getIconResource()) == 0) {
            return getFullResDefaultActivityIcon();
        }
        return getFullResIcon(resources, iconId);
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = getFullResDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1), Math.max(d.getIntrinsicHeight(), 1), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        return b;
    }

    public void remove(ComponentName componentName) {
        synchronized (this.mCache) {
            this.mCache.remove(componentName);
        }
    }

    public void flush() {
        synchronized (this.mCache) {
            this.mCache.clear();
        }
    }

    public void getTitleAndIcon(ApplicationInfo application, ResolveInfo info) {
        synchronized (this.mCache) {
            CacheEntry entry = cacheLocked(application.componentName, info);
            if (entry.titleBitmap == null) {
                entry.titleBitmap = this.mBubble.createTextBitmap(entry.title);
            }
            application.title = entry.title;
            application.titleBitmap = entry.titleBitmap;
            application.iconBitmap = entry.icon;
        }
    }

    public Bitmap getIcon(Intent intent) {
        synchronized (this.mCache) {
            ResolveInfo resolveInfo = this.mPackageManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();
            if (resolveInfo == null || component == null) {
                Bitmap bitmap = this.mDefaultIcon;
                return bitmap;
            }
            Bitmap bitmap2 = cacheLocked(component, resolveInfo).icon;
            return bitmap2;
        }
    }

    public Bitmap getIcon(ComponentName component, ResolveInfo resolveInfo) {
        synchronized (this.mCache) {
            if (resolveInfo == null || component == null) {
                return null;
            }
            Bitmap bitmap = cacheLocked(component, resolveInfo).icon;
            return bitmap;
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return this.mDefaultIcon == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info) {
        Drawable icon;
        CacheEntry entry = this.mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();
            this.mCache.put(componentName, entry);
            entry.title = info.loadLabel(this.mPackageManager).toString();
            if (entry.title == null) {
                entry.title = info.activityInfo.name;
            }
            try {
                icon = getFullResIcon(info, this.mPackageManager);
            } catch (Resources.NotFoundException e) {
                Resources.NotFoundException notFoundException = e;
                icon = getFullResDefaultActivityIcon();
            }
            entry.icon = Utilities.createIconBitmap(icon, this.mContext);
        }
        return entry;
    }
}
