package com.android.launcher2;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

class LiveFolderInfo extends FolderInfo {
    Intent baseIntent;
    int displayMode;
    Bitmap icon;
    Intent.ShortcutIconResource iconResource;
    Uri uri;

    LiveFolderInfo() {
        this.itemType = 3;
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put("title", this.title.toString());
        values.put("uri", this.uri.toString());
        if (this.baseIntent != null) {
            values.put("intent", this.baseIntent.toUri(0));
        }
        values.put("iconType", 0);
        values.put("displayMode", Integer.valueOf(this.displayMode));
        if (this.iconResource != null) {
            values.put("iconPackage", this.iconResource.packageName);
            values.put("iconResource", this.iconResource.resourceName);
        }
    }
}
