package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher2.CustomizePagedView;

public class CustomizeTrayTabHost extends TabHost implements LauncherTransitionable {
    private Context mContext;
    /* access modifiers changed from: private */
    public CustomizePagedView mCustomizePagedView;
    private boolean mFirstLayout = true;
    private final LayoutInflater mInflater;
    private float mVerticalFillPercentage;

    public CustomizeTrayTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        Resources res = getResources();
        setup();
        this.mCustomizePagedView = (CustomizePagedView) findViewById(R.id.customization_drawer_tab_contents);
        TabHost.TabContentFactory contentFactory = new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                return CustomizeTrayTabHost.this.mCustomizePagedView;
            }
        };
        TabWidget tabWidget = (TabWidget) findViewById(16908307);
        TextView tabView = (TextView) this.mInflater.inflate(R.layout.customize_tab_widget_indicator, tabWidget, false);
        tabView.setText(this.mContext.getString(R.string.widgets_tab_label));
        addTab(newTabSpec("widgets").setIndicator(tabView).setContent(contentFactory));
        TextView tabView2 = (TextView) this.mInflater.inflate(R.layout.customize_tab_widget_indicator, tabWidget, false);
        tabView2.setText(this.mContext.getString(R.string.all_apps_tab_apps));
        addTab(newTabSpec("applications").setIndicator(tabView2).setContent(contentFactory));
        TextView tabView3 = (TextView) this.mInflater.inflate(R.layout.customize_tab_widget_indicator, tabWidget, false);
        tabView3.setText(this.mContext.getString(R.string.wallpapers_tab_label));
        addTab(newTabSpec("wallpapers").setIndicator(tabView3).setContent(contentFactory));
        TextView tabView4 = (TextView) this.mInflater.inflate(R.layout.customize_tab_widget_indicator, tabWidget, false);
        tabView4.setText(this.mContext.getString(R.string.shortcuts_tab_label));
        addTab(newTabSpec("shortcuts").setIndicator(tabView4).setContent(contentFactory));

        // Theme picker tab — its own ListView, not the CustomizePagedView
        final ListView themeListView = buildThemeListView();
        themeListView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        themeListView.setBackgroundColor(0xFF1A1A2E);
        final android.widget.FrameLayout themeContainer = new android.widget.FrameLayout(mContext);
        themeContainer.addView(themeListView);
        TextView tabView5 = (TextView) this.mInflater.inflate(R.layout.customize_tab_widget_indicator, tabWidget, false);
        tabView5.setText(this.mContext.getString(R.string.theme_tab_label));
        addTab(newTabSpec("theme").setIndicator(tabView5).setContent(tag -> themeContainer));

        this.mVerticalFillPercentage = ((float) res.getInteger(R.integer.customization_drawer_verticalFillPercentage)) / 100.0f;
        setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                if (tabId.equals("theme")) return; // theme tab has its own content view
                final CustomizePagedView.CustomizationType newType = CustomizeTrayTabHost.this.getCustomizeFilterForTabTag(tabId);
                if (newType != CustomizeTrayTabHost.this.mCustomizePagedView.getCustomizationFilter()) {
                    final int duration = CustomizeTrayTabHost.this.getResources().getInteger(R.integer.config_tabTransitionTime);
                    float alpha = CustomizeTrayTabHost.this.mCustomizePagedView.getAlpha();
                    ObjectAnimator ofFloat = ObjectAnimator.ofFloat(CustomizeTrayTabHost.this.mCustomizePagedView, "alpha", new float[]{alpha, 0.0f});
                    ofFloat.setDuration((long) duration);
                    ofFloat.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            CustomizeTrayTabHost.this.mCustomizePagedView.setCustomizationFilter(newType);
                            float alpha = CustomizeTrayTabHost.this.mCustomizePagedView.getAlpha();
                            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(CustomizeTrayTabHost.this.mCustomizePagedView, "alpha", new float[]{alpha, 1.0f});
                            ofFloat.setDuration((long) duration);
                            ofFloat.start();
                        }
                    });
                    ofFloat.start();
                }
            }
        });
    }

    public void onLauncherTransitionStart(Animator animation) {
        if (animation != null) {
            setLayerType(2, (Paint) null);
            if (!this.mFirstLayout) {
                buildLayer();
            }
        }
    }

    public void onLauncherTransitionEnd(Animator animation) {
        if (animation != null) {
            setLayerType(0, (Paint) null);
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (View.MeasureSpec.getMode(heightMeasureSpec) == Integer.MIN_VALUE) {
            super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(Math.max(getMeasuredHeight(), (int) (((float) View.MeasureSpec.getSize(heightMeasureSpec)) * this.mVerticalFillPercentage)), 1073741824));
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mFirstLayout) {
            this.mFirstLayout = false;
            TabWidget tabWidget = (TabWidget) findViewById(16908307);
            int pageWidth = ((CustomizePagedView) findViewById(R.id.customization_drawer_tab_contents)).getPageContentWidth();
            TabWidget customizeTabBar = (TabWidget) findViewById(16908307);
            if (customizeTabBar == null) {
                throw new Resources.NotFoundException();
            }
            int tabWidgetPadding = 0;
            if (tabWidget.getChildCount() > 0) {
                tabWidgetPadding = 0 + (tabWidget.getChildAt(0).getPaddingLeft() * 2);
            }
            int availableWidth = r - l;
            int desiredWidth = pageWidth + tabWidgetPadding;
            // Cap to available width so tabs don't overflow on small screens
            customizeTabBar.getLayoutParams().width = (desiredWidth > 0) ? Math.min(desiredWidth, availableWidth) : availableWidth;
        }
        super.onLayout(changed, l, t, r, b);
    }

    /* access modifiers changed from: package-private */
    public CustomizePagedView.CustomizationType getCustomizeFilterForTabTag(String tag) {
        if (tag.equals("widgets")) {
            return CustomizePagedView.CustomizationType.WidgetCustomization;
        }
        if (tag.equals("applications")) {
            return CustomizePagedView.CustomizationType.ApplicationCustomization;
        }
        if (tag.equals("wallpapers")) {
            return CustomizePagedView.CustomizationType.WallpaperCustomization;
        }
        if (tag.equals("shortcuts")) {
            return CustomizePagedView.CustomizationType.ShortcutCustomization;
        }
        return CustomizePagedView.CustomizationType.WidgetCustomization;
    }

    private ListView buildThemeListView() {
        final String PREFS = "launcher_theme_prefs";
        final String KEY = "selected_theme";
        final String[] themes = {
            "launcher1-d", "launcher2-g", "launcher3-h",
            "launcher2-j", "launcher3-m", "launcher3-o"
        };
        final String[] displayNames = {
            "Android 1.6 Donut", "Android 2.3 Gingerbread", "Android 3.0 Honeycomb",
            "Android 4.1 Jelly Bean", "Android 6.0 Marshmallow", "Android 8.0 Oreo"
        };
        final String current = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "launcher2-j");

        // Use Holo.Dark context so radio buttons render with Holo styling
        final Context holoCtx = new android.view.ContextThemeWrapper(
                mContext, android.R.style.Theme_Holo);

        ListView lv = new ListView(holoCtx);
        lv.setAdapter(new ArrayAdapter<String>(holoCtx, android.R.layout.simple_list_item_single_choice, displayNames));
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].equals(current)) {
                lv.setItemChecked(i, true);
                break;
            }
        }
        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = themes[position];
            mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY, selected).commit();
            applyAlias(selected);
            Toast.makeText(mContext, displayNames[position], Toast.LENGTH_SHORT).show();
            mContext.startActivity(buildLaunchIntent(selected));
        });
        return lv;
    }

    private void applyAlias(String theme) {
        android.content.pm.PackageManager pm = mContext.getPackageManager();
        String pkg = mContext.getPackageName();
        String[] aliases = {
            pkg + ".DonutHomeAlias",
            pkg + ".GingerbreadHomeAlias",
            pkg + ".JellyBeanHomeAlias",
            pkg + ".MarshmallowHomeAlias",
            pkg + ".OreoHomeAlias",
            pkg + ".HoneycombHomeAlias"
        };
        boolean[] enabled = {
            theme.equals("launcher1-d"),
            theme.equals("launcher2-g"),
            theme.equals("launcher2-j"),
            theme.equals("launcher3-m"),
            theme.equals("launcher3-o"),
            theme.equals("launcher3-h")
        };
        for (int i = 0; i < aliases.length; i++) {
            int desired = enabled[i]
                ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            android.content.ComponentName cn = new android.content.ComponentName(pkg, aliases[i]);
            if (pm.getComponentEnabledSetting(cn) != desired) {
                pm.setComponentEnabledSetting(cn, desired,
                    android.content.pm.PackageManager.DONT_KILL_APP);
            }
        }
    }

    private android.content.Intent buildLaunchIntent(String theme) {
        String pkg = mContext.getPackageName();
        String cls;
        switch (theme) {
            case "launcher2-g": cls = "com.gingerbread.launcher2.Launcher"; break;
            case "launcher2-j": cls = "com.jellybean.launcher2.Launcher"; break;
            case "launcher3-m": cls = "com.marshmallow.launcher.Launcher"; break;
            case "launcher3-o": cls = "com.oreo.launcher3.Launcher"; break;
            case "launcher3-h": cls = "com.android.launcher2.Launcher"; break;
            default:            cls = "com.dismal.unifiedlauncher.MainActivity"; break;
        }
        return new android.content.Intent()
                .setComponent(new android.content.ComponentName(pkg, cls))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }
}
