package com.android.launcher2;

import android.view.View;

public interface DragSource {
    void onDragViewVisible();

    void onDropCompleted(View view, Object obj, boolean z);
}
