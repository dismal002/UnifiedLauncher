package com.android.launcher2;

import android.appwidget.AppWidgetProviderInfo;
import android.os.Parcelable;

/* compiled from: PendingAddItemInfo */
class PendingAddWidgetInfo extends PendingAddItemInfo {
    Parcelable configurationData;
    String mimeType;
    int minHeight;
    int minWidth;

    public PendingAddWidgetInfo(AppWidgetProviderInfo i, String dataMimeType, Parcelable data) {
        this.itemType = 4;
        this.componentName = i.provider;
        this.minWidth = i.minWidth;
        this.minHeight = i.minHeight;
        if (dataMimeType != null && data != null) {
            this.mimeType = dataMimeType;
            this.configurationData = data;
        }
    }
}
