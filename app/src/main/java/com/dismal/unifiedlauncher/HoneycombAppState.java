package com.dismal.unifiedlauncher;

/**
 * Honeycomb launcher state is bootstrapped lazily inside Launcher.onCreate()
 * when getApplication() is not a com.android.launcher2.LauncherApplication.
 * Nothing to do here at app startup.
 */
public class HoneycombAppState {
    public static void init(android.content.Context context) {
        // no-op: Launcher.onCreate handles its own init
    }
}
