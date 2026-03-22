package com.dismal.unifiedlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class ThemeSelectionActivity extends Activity {

    private static final String PREFS = "launcher_theme_prefs";
    private static final String KEY = "selected_theme";

    private static final String[] LABELS = {
        "Donut", "Gingerbread", "Honeycomb", "Jelly Bean", "Marshmallow", "Oreo"
    };
    private static final String[] THEMES = {
        "launcher1-d", "launcher2-g", "launcher3-h",
        "launcher2-j", "launcher3-m", "launcher3-o"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String current = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY, "launcher2-j");
        int checkedItem = 0;
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i].equals(current)) { checkedItem = i; break; }
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose launcher version")
                .setSingleChoiceItems(LABELS, checkedItem, (dialog, which) -> {
                    applyTheme(THEMES[which]);
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton("Cancel", (d, w) -> finish())
                .setOnCancelListener(d -> finish())
                .show();
    }

    private void applyTheme(String theme) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putString(KEY, theme).apply();

        PackageManager pm = getPackageManager();
        String pkg = getPackageName();
        String[] aliases = {
            pkg + ".DonutHomeAlias", pkg + ".GingerbreadHomeAlias",
            pkg + ".JellyBeanHomeAlias", pkg + ".MarshmallowHomeAlias",
            pkg + ".OreoHomeAlias", pkg + ".HoneycombHomeAlias"
        };
        boolean[] enabled = {
            theme.equals("launcher1-d"), theme.equals("launcher2-g"),
            theme.equals("launcher2-j"), theme.equals("launcher3-m"),
            theme.equals("launcher3-o"), theme.equals("launcher3-h")
        };
        for (int i = 0; i < aliases.length; i++) {
            int desired = enabled[i]
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            pm.setComponentEnabledSetting(
                new ComponentName(pkg, aliases[i]), desired,
                PackageManager.DONT_KILL_APP);
        }
    }
}
