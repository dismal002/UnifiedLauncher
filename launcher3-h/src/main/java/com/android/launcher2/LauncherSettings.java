package com.android.launcher2;

import android.net.Uri;
import android.provider.BaseColumns;

class LauncherSettings {

    interface BaseLauncherColumns extends BaseColumns {
    }

    LauncherSettings() {
    }

    static final class Favorites implements BaseLauncherColumns {
        static final Uri CONTENT_URI = Uri.parse("content://" + LauncherProvider.AUTHORITY + "/favorites?notify=true");
        static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://" + LauncherProvider.AUTHORITY + "/favorites?notify=false");

        Favorites() {
        }

        static Uri getContentUri(long id, boolean notify) {
            return Uri.parse("content://" + LauncherProvider.AUTHORITY + "/favorites/" + id + "?" + "notify" + "=" + notify);
        }
    }
}
