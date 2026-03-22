package com.android.launcher2;

/**
 * Shared constants used across the launcher for all-apps layout decisions.
 *
 * Honeycomb also had a RenderScript/GL based 3D all-apps implementation; this
 * standalone build removes RenderScript, so these constants live separately.
 */
public final class AllAppsDefines {
    public static final class Defines {
        public static final int COLUMNS_PER_PAGE_LANDSCAPE = 6;
        public static final int COLUMNS_PER_PAGE_PORTRAIT = 4;
        public static final int ROWS_PER_PAGE_LANDSCAPE = 3;
        public static final int ROWS_PER_PAGE_PORTRAIT = 4;
        public static final int SELECTION_TEXTURE_HEIGHT_PX = 94;
        public static final int SELECTION_TEXTURE_WIDTH_PX = 94;

        private Defines() {}
    }

    private AllAppsDefines() {}
}

