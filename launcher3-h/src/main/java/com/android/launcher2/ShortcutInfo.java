package com.android.launcher2;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;

class ShortcutInfo extends ItemInfo {
    boolean customIcon;
    Intent.ShortcutIconResource iconResource;
    Intent intent;
    private Bitmap mIcon;
    CharSequence title;
    boolean usingFallbackIcon;

    ShortcutInfo() {
        this.itemType = 1;
    }

    public ShortcutInfo(ApplicationInfo info) {
        super(info);
        this.title = info.title.toString();
        this.intent = new Intent(info.intent);
        this.customIcon = false;
    }

    public void setIcon(Bitmap b) {
        this.mIcon = b;
    }

    public Bitmap getIcon(IconCache iconCache) {
        if (this.mIcon == null) {
            this.mIcon = iconCache.getIcon(this.intent);
            this.usingFallbackIcon = iconCache.isDefaultIcon(this.mIcon);
        }
        return this.mIcon;
    }

    /* access modifiers changed from: package-private */
    public final void setActivity(ComponentName className, int launchFlags) {
        this.intent = new Intent("android.intent.action.MAIN");
        this.intent.addCategory("android.intent.category.LAUNCHER");
        this.intent.setComponent(className);
        this.intent.setFlags(launchFlags);
        this.itemType = 0;
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        String titleStr;
        String uri;
        super.onAddToDatabase(values);
        if (this.title != null) {
            titleStr = this.title.toString();
        } else {
            titleStr = null;
        }
        values.put("title", titleStr);
        if (this.intent != null) {
            uri = this.intent.toUri(0);
        } else {
            uri = null;
        }
        values.put("intent", uri);
        if (this.customIcon) {
            values.put("iconType", 1);
            writeBitmap(values, this.mIcon);
            return;
        }
        if (!this.usingFallbackIcon) {
            writeBitmap(values, this.mIcon);
        }
        values.put("iconType", 0);
        if (this.iconResource != null) {
            values.put("iconPackage", this.iconResource.packageName);
            values.put("iconResource", this.iconResource.resourceName);
        }
    }

    public String toString() {
        return "ShortcutInfo(title=" + this.title.toString() + ")";
    }

    /* access modifiers changed from: package-private */
    public void unbind() {
        super.unbind();
    }
}
