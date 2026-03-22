package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class LiveFolderIcon extends FolderIcon {
    public LiveFolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LiveFolderIcon(Context context) {
        super(context);
    }

    static LiveFolderIcon fromXml(int resId, Launcher launcher, ViewGroup group, LiveFolderInfo folderInfo) {
        LiveFolderIcon icon = (LiveFolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        Resources resources = launcher.getResources();
        Bitmap b = folderInfo.icon;
        if (b == null) {
            b = Utilities.createIconBitmap(resources.getDrawable(R.drawable.ic_launcher_folder), launcher);
        }
        icon.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new FastBitmapDrawable(b), (Drawable) null, (Drawable) null);
        icon.setText(folderInfo.title);
        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        return icon;
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        return false;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }
}
