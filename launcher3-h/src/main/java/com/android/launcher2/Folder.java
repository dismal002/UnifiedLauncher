package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;

public class Folder extends LinearLayout implements View.OnClickListener, View.OnLongClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, DragSource {
    protected Button mCloseButton;
    protected AbsListView mContent;
    protected DragController mDragController;
    protected ShortcutInfo mDragItem;
    protected FolderInfo mInfo;
    protected Launcher mLauncher;

    public Folder(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAlwaysDrawnWithCacheEnabled(false);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = (AbsListView) findViewById(R.id.folder_content);
        this.mContent.setOnItemClickListener(this);
        this.mContent.setOnItemLongClickListener(this);
        this.mCloseButton = (Button) findViewById(R.id.folder_close);
        this.mCloseButton.setOnClickListener(this);
        this.mCloseButton.setOnLongClickListener(this);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        ShortcutInfo app = (ShortcutInfo) parent.getItemAtPosition(position);
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        app.intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
        this.mLauncher.startActivitySafely(app.intent, app);
    }

    public void onClick(View v) {
        this.mLauncher.closeFolder(this);
    }

    public boolean onLongClick(View v) {
        this.mLauncher.closeFolder(this);
        this.mLauncher.showRenameDialog(this.mInfo);
        return true;
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!view.isInTouchMode()) {
            return false;
        }
        ShortcutInfo app = (ShortcutInfo) parent.getItemAtPosition(position);
        this.mDragController.startDrag(view, this, app, DragController.DRAG_ACTION_COPY);
        this.mLauncher.closeFolder(this);
        this.mDragItem = app;
        return true;
    }

    public void setDragController(DragController dragController) {
        this.mDragController = dragController;
    }

    public void onDropCompleted(View target, Object dragInfo, boolean success) {
    }

    public void onDragViewVisible() {
    }

    /* access modifiers changed from: package-private */
    public void setContentAdapter(BaseAdapter adapter) {
        this.mContent.setAdapter(adapter);
    }

    /* access modifiers changed from: package-private */
    public void notifyDataSetChanged() {
        ((BaseAdapter) this.mContent.getAdapter()).notifyDataSetChanged();
    }

    /* access modifiers changed from: package-private */
    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    /* access modifiers changed from: package-private */
    public FolderInfo getInfo() {
        return this.mInfo;
    }

    /* access modifiers changed from: package-private */
    public void onOpen() {
        this.mContent.requestLayout();
    }

    /* access modifiers changed from: package-private */
    public void onClose() {
        Workspace workspace = this.mLauncher.getWorkspace();
        workspace.getChildAt(workspace.getCurrentPage()).requestFocus();
    }

    /* access modifiers changed from: package-private */
    public void bind(FolderInfo info) {
        this.mInfo = info;
        this.mCloseButton.setText(info.title);
    }
}
