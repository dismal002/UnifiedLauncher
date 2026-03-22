package com.gingerbread.launcher2;

/**
 * Static holder for the Gingerbread launcher's model and icon cache.
 * Populated by GingerAppState before the Launcher activity starts.
 */
public class GingerState {
    private static LauncherModel sModel;
    private static IconCache sIconCache;

    public static void init(LauncherModel model, IconCache iconCache) {
        sModel = model;
        sIconCache = iconCache;
    }

    public static LauncherModel getModel() { return sModel; }
    public static IconCache getIconCache() { return sIconCache; }
}
