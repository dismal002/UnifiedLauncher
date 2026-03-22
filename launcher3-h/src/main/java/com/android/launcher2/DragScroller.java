package com.android.launcher2;

public interface DragScroller {
    void onEnterScrollArea(int i);

    void onExitScrollArea();

    void scrollLeft();

    void scrollRight();
}
