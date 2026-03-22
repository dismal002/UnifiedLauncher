package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class FolderIcon extends BubbleTextView implements DropTarget {
    private Drawable mCloseIcon;
    private UserFolderInfo mInfo;
    private Launcher mLauncher;
    private Drawable mOpenIcon;

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderIcon(Context context) {
        super(context);
    }

    public boolean isDropEnabled() {
        return !((Workspace) getParent().getParent()).isSmall();
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group, UserFolderInfo folderInfo, IconCache iconCache) {
        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);
        Resources resources = launcher.getResources();
        Drawable d = iconCache.getFullResIcon(resources, R.drawable.ic_launcher_folder);
        icon.mCloseIcon = d;
        icon.mOpenIcon = iconCache.getFullResIcon(resources, R.drawable.ic_launcher_folder_open);
        icon.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, d, (Drawable) null, (Drawable) null);
        icon.setText(folderInfo.title);
        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        return icon;
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        ItemInfo item = (ItemInfo) dragInfo;
        int itemType = item.itemType;
        return (itemType == 0 || itemType == 1) && item.container != this.mInfo.id;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        ShortcutInfo item;
        if (dragInfo instanceof ApplicationInfo) {
            item = ((ApplicationInfo) dragInfo).makeShortcut();
        } else {
            item = (ShortcutInfo) dragInfo;
        }
        this.mInfo.add(item);
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, item, this.mInfo.id, 0, 0, 0);
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        if (acceptDrop(source, x, y, xOffset, yOffset, dragView, dragInfo)) {
            setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.mOpenIcon, (Drawable) null, (Drawable) null);
        }
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        setCompoundDrawablesWithIntrinsicBounds((Drawable) null, this.mCloseIcon, (Drawable) null, (Drawable) null);
    }

    public DropTarget getDropTargetDelegate(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        return null;
    }
}
