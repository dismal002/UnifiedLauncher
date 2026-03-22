package com.android.launcher2;
import com.launcher3h.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private final int[] mCoordinates = new int[2];

    public void onReceive(Context context, Intent data) {
        if ("com.android.launcher.action.INSTALL_SHORTCUT".equals(data.getAction())) {
            int screen = Launcher.getScreen();
            if (!installShortcut(context, data, screen)) {
                int i = 0;
                while (i < 5) {
                    if (i == screen || !installShortcut(context, data, i)) {
                        i++;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    private boolean installShortcut(Context context, Intent data, int screen) {
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        if (findEmptyCell(context, this.mCoordinates, screen)) {
            Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
            if (intent != null) {
                if (intent.getAction() == null) {
                    intent.setAction("android.intent.action.VIEW");
                }
                if (data.getBooleanExtra("duplicate", true) || !LauncherModel.shortcutExists(context, name, intent)) {
                    LauncherApplication.getSharedInstance(context).getModel().addShortcut(context, data, screen, this.mCoordinates[0], this.mCoordinates[1], true);
                    Toast.makeText(context, context.getString(R.string.shortcut_installed, new Object[]{name}), 0).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.shortcut_duplicate, new Object[]{name}), 0).show();
                }
                return true;
            }
        } else {
            Toast.makeText(context, context.getString(R.string.out_of_space), 0).show();
        }
        return false;
    }

    private static boolean findEmptyCell(Context context, int[] xy, int screen) {
        int xCount = LauncherModel.getCellCountX();
        int yCount = LauncherModel.getCellCountY();
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{xCount, yCount});
        ArrayList<ItemInfo> items = LauncherModel.getItemsInLocalCoordinates(context);
        int i = 0;
        while (i < items.size()) {
            ItemInfo item = items.get(i);
            if (item.screen == screen) {
                int cellX = item.cellX;
                int cellY = item.cellY;
                int spanX = item.spanX;
                int spanY = item.spanY;
                int x = cellX;
                while (x < cellX + spanX && x < xCount) {
                    int y = cellY;
                    while (y < cellY + spanY && y < yCount) {
                        occupied[x][y] = true;
                        y++;
                    }
                    x++;
                }
            }
            i++;
            ItemInfo itemInfo = item;
        }
        return CellLayout.findVacantCell(xy, 1, 1, xCount, yCount, occupied);
    }
}
