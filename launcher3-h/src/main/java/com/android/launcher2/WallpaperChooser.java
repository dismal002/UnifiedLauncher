package com.android.launcher2;
import com.launcher3h.R;

import android.app.Activity;
import android.os.Bundle;

public class WallpaperChooser extends Activity {
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.launcher3h_wallpaper_chooser_base);
        if (getFragmentManager().findFragmentById(R.id.wallpaper_chooser_fragment) == null) {
            WallpaperChooserDialogFragment.newInstance().show(getFragmentManager(), "dialog");
        }
    }
}
