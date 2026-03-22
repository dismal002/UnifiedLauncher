package com.dismal.unifiedlauncher;

import android.content.Context;
import com.gingerbread.launcher2.LauncherApplication;

/**
 * Initialises the Gingerbread launcher's model into GingerState.
 */
public class GingerAppState {

    private static boolean sInitialised = false;

    public static void init(Context context) {
        if (sInitialised) return;
        sInitialised = true;
        LauncherApplication.initFromContext(context);
    }
}
