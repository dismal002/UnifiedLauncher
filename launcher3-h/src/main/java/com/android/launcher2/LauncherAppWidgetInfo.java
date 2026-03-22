package com.android.launcher2;

import android.appwidget.AppWidgetHostView;
import android.content.ContentValues;

class LauncherAppWidgetInfo extends ItemInfo {
    int appWidgetId = -1;
    AppWidgetHostView hostView = null;
    int minHeight = -1;
    int minWidth = -1;

    LauncherAppWidgetInfo(int appWidgetId2) {
        this.itemType = 4;
        this.appWidgetId = appWidgetId2;
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put("appWidgetId", Integer.valueOf(this.appWidgetId));
    }

    public String toString() {
        return "AppWidget(id=" + Integer.toString(this.appWidgetId) + ")";
    }

    /* access modifiers changed from: package-private */
    public void unbind() {
        super.unbind();
        this.hostView = null;
    }
}
