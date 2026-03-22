package com.android.launcher2;

import java.util.ArrayList;

public interface AllAppsView {
    void addApps(ArrayList<ApplicationInfo> arrayList);

    void dumpState();

    boolean isAnimating();

    boolean isVisible();

    void removeApps(ArrayList<ApplicationInfo> arrayList);

    void setApps(ArrayList<ApplicationInfo> arrayList);

    void setDragController(DragController dragController);

    void setLauncher(Launcher launcher);

    void surrender();

    void updateApps(ArrayList<ApplicationInfo> arrayList);

    void zoom(float f, boolean z);
}
