package com.android.launcher2;

import android.content.ContentValues;
import java.util.ArrayList;

class UserFolderInfo extends FolderInfo {
    ArrayList<ShortcutInfo> contents = new ArrayList<>();

    UserFolderInfo() {
        this.itemType = 2;
    }

    public void add(ShortcutInfo item) {
        this.contents.add(item);
    }

    public void remove(ShortcutInfo item) {
        this.contents.remove(item);
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put("title", this.title.toString());
    }
}
