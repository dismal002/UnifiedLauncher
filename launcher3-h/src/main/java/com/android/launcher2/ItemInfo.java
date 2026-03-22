package com.android.launcher2;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class ItemInfo {
    int cellX = -1;
    int cellY = -1;
    long container = -1;
    int[] dropPos = null;
    long id = -1;
    boolean isGesture = false;
    int itemType;
    int screen = -1;
    int spanX = 1;
    int spanY = 1;

    ItemInfo() {
    }

    ItemInfo(ItemInfo info) {
        this.id = info.id;
        this.cellX = info.cellX;
        this.cellY = info.cellY;
        this.spanX = info.spanX;
        this.spanY = info.spanY;
        this.screen = info.screen;
        this.itemType = info.itemType;
        this.container = info.container;
    }

    /* access modifiers changed from: package-private */
    public void onAddToDatabase(ContentValues values) {
        values.put("itemType", Integer.valueOf(this.itemType));
        if (!this.isGesture) {
            values.put("container", Long.valueOf(this.container));
            values.put("screen", Integer.valueOf(this.screen));
            values.put("cellX", Integer.valueOf(this.cellX));
            values.put("cellY", Integer.valueOf(this.cellY));
            values.put("spanX", Integer.valueOf(this.spanX));
            values.put("spanY", Integer.valueOf(this.spanY));
        }
    }

    /* access modifiers changed from: package-private */
    public void updateValuesWithCoordinates(ContentValues values, int cellX2, int cellY2) {
        values.put("cellX", Integer.valueOf(cellX2));
        values.put("cellY", Integer.valueOf(cellY2));
    }

    static byte[] flattenBitmap(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(bitmap.getWidth() * bitmap.getHeight() * 4);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            IOException iOException = e;
            Log.w("Favorite", "Could not write icon");
            return null;
        }
    }

    static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            values.put("icon", flattenBitmap(bitmap));
        }
    }

    /* access modifiers changed from: package-private */
    public void unbind() {
    }

    public String toString() {
        return "Item(id=" + this.id + " type=" + this.itemType + ")";
    }
}
