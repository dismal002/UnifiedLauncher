package com.dismal.unifiedlauncher;

/**
 * Unified Application class. Extends JellyBean's LauncherApplication so that
 * com.jellybean.launcher2.Launcher can cast getApplication() successfully.
 */
public class LauncherApplication extends com.jellybean.launcher2.LauncherApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        HoneycombAppState.init(this);
        GingerAppState.init(this);
        DonutAppState.init(this);
        // Do NOT call setComponentEnabledSetting here — doing so on startup
        // causes a SIGKILL restart loop. Aliases are already correct from the last switch.
    }
}
