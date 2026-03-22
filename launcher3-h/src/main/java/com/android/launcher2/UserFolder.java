package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class UserFolder extends Folder implements DropTarget {
    public UserFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    static UserFolder fromXml(Context context) {
        return (UserFolder) LayoutInflater.from(context).inflate(R.layout.launcher3h_user_folder, (ViewGroup) null);
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
        ((ShortcutsAdapter) this.mContent.getAdapter()).add(item);
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, item, this.mInfo.id, 0, 0, 0);
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
    }

    public void onDropCompleted(View target, Object dragInfo, boolean success) {
        if (success) {
            ((ShortcutsAdapter) this.mContent.getAdapter()).remove(this.mDragItem);
        }
    }

    public boolean isDropEnabled() {
        return true;
    }

    /* access modifiers changed from: package-private */
    public void bind(FolderInfo info) {
        super.bind(info);
        setContentAdapter(new ShortcutsAdapter(getContext(), ((UserFolderInfo) info).contents));
    }

    /* access modifiers changed from: package-private */
    public void onOpen() {
        super.onOpen();
        requestFocus();
    }

    public DropTarget getDropTargetDelegate(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) {
        return null;
    }
}
