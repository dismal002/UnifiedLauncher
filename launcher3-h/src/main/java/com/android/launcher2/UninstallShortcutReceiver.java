package com.android.launcher2;
import com.launcher3h.R;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.widget.Toast;
import com.android.launcher2.LauncherSettings;
import java.net.URISyntaxException;

public class UninstallShortcutReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent data) {
        if ("com.android.launcher.action.UNINSTALL_SHORTCUT".equals(data.getAction())) {
            Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
            String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
            boolean duplicate = data.getBooleanExtra("duplicate", true);
            if (intent != null && name != null) {
                ContentResolver cr = context.getContentResolver();
                Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"_id", "intent"}, "title=?", new String[]{name}, (String) null);
                int intentIndex = c.getColumnIndexOrThrow("intent");
                int idIndex = c.getColumnIndexOrThrow("_id");
                boolean changed = false;
                while (c.moveToNext()) {
                    try {
                        try {
                            if (intent.filterEquals(Intent.parseUri(c.getString(intentIndex), 0))) {
                                cr.delete(LauncherSettings.Favorites.getContentUri(c.getLong(idIndex), false), (String) null, (String[]) null);
                                changed = true;
                                if (!duplicate) {
                                    break;
                                }
                            } else {
                                continue;
                            }
                        } catch (URISyntaxException e) {
                        }
                    } finally {
                        c.close();
                    }
                }
                if (changed) {
                    cr.notifyChange(LauncherSettings.Favorites.CONTENT_URI, (ContentObserver) null);
                    Toast.makeText(context, context.getString(R.string.shortcut_uninstalled, new Object[]{name}), 0).show();
                }
            }
        }
    }
}
