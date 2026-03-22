/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oreo.launcher3;
import com.oreo.launcher3.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.provider.Settings.System;
import androidx.core.os.BuildCompat;

import com.oreo.launcher3.graphics.IconShapeOverride;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity {

    private static final String ICON_BADGING_PREFERENCE_KEY = "pref_icon_badging";
    // TODO: use Settings.Secure.NOTIFICATION_BADGING
    private static final String NOTIFICATION_BADGING = "notification_badging";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new LauncherSettingsFragment())
                .commit();
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragment {

        private SystemDisplayRotationLockObserver mRotationLockObserver;
        private IconBadgingObserver mIconBadgingObserver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            addPreferencesFromResource(R.xml.launcher_preferences);

            ContentResolver resolver = getActivity().getContentResolver();

            // Setup allow rotation preference
            Preference rotationPref = findPreference(Utilities.ALLOW_ROTATION_PREFERENCE_KEY);
            if (getResources().getBoolean(R.bool.allow_rotation)) {
                // Launcher supports rotation by default. No need to show this setting.
                getPreferenceScreen().removePreference(rotationPref);
            } else {
                mRotationLockObserver = new SystemDisplayRotationLockObserver(rotationPref, resolver);

                // Register a content observer to listen for system setting changes while
                // this UI is active.
                resolver.registerContentObserver(
                        Settings.System.getUriFor(System.ACCELEROMETER_ROTATION),
                        false, mRotationLockObserver);

                // Initialize the UI once
                mRotationLockObserver.onChange(true);
                rotationPref.setDefaultValue(Utilities.getAllowRotationDefaultValue(getActivity()));
            }

            Preference iconBadgingPref = findPreference(ICON_BADGING_PREFERENCE_KEY);
            if (!BuildCompat.isAtLeastO()) {
                getPreferenceScreen().removePreference(
                        findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
                getPreferenceScreen().removePreference(iconBadgingPref);
            } else {
                // Listen to system notification badge settings while this UI is active.
                mIconBadgingObserver = new IconBadgingObserver(iconBadgingPref, resolver);
                resolver.registerContentObserver(
                        Settings.Secure.getUriFor(NOTIFICATION_BADGING),
                        false, mIconBadgingObserver);
                mIconBadgingObserver.onChange(true);
            }

            Preference themePref = findPreference("pref_changeTheme");
            if (themePref != null) {
                themePref.setOnPreferenceClickListener(pref -> {
                    showOreoThemePicker();
                    return true;
                });
            }

            Preference iconShapeOverride = findPreference(IconShapeOverride.KEY_PREFERENCE);            if (iconShapeOverride != null) {
                if (IconShapeOverride.isSupported(getActivity())) {
                    IconShapeOverride.handlePreferenceUi((ListPreference) iconShapeOverride);
                } else {
                    getPreferenceScreen().removePreference(iconShapeOverride);
                }
            }
        }

        private void showOreoThemePicker() {
            final String PREFS = "launcher_theme_prefs";
            final String KEY = "selected_theme";
            final String[] themes = {"launcher1-d","launcher2-g","launcher3-h","launcher2-j","launcher3-m","launcher3-o"};
            final String[] names = {"Android 1.6 Donut","Android 2.3 Gingerbread","Android 3.0 Honeycomb","Android 4.1 Jelly Bean","Android 6.0 Marshmallow","Android 8.0 Oreo"};
            android.content.Context ctx = getActivity();
            final String cur = ctx.getSharedPreferences(PREFS, 0).getString(KEY, "launcher2-j");
            int idx = 0;
            for (int i = 0; i < themes.length; i++) if (themes[i].equals(cur)) { idx = i; break; }
            new android.app.AlertDialog.Builder(ctx)
                .setTitle("Change Theme")
                .setSingleChoiceItems(names, idx, (d, w) -> {
                    ctx.getSharedPreferences(PREFS, 0).edit().putString(KEY, themes[w]).commit();
                    applyOreoAlias(ctx, themes[w]);
                    d.dismiss();
                    ctx.startActivity(buildOreoIntent(ctx, themes[w]));
                })
                .setNegativeButton("Cancel", null).show();
        }

        private void applyOreoAlias(android.content.Context ctx, String theme) {
            android.content.pm.PackageManager pm = ctx.getPackageManager();
            String pkg = ctx.getPackageName();
            String[] aliases = {pkg+".DonutHomeAlias",pkg+".GingerbreadHomeAlias",pkg+".JellyBeanHomeAlias",pkg+".MarshmallowHomeAlias",pkg+".OreoHomeAlias",pkg+".HoneycombHomeAlias"};
            String[] keys = {"launcher1-d","launcher2-g","launcher2-j","launcher3-m","launcher3-o","launcher3-h"};
            for (int i = 0; i < aliases.length; i++) {
                boolean en = theme.equals(keys[i]);
                int desired = en ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                android.content.ComponentName cn = new android.content.ComponentName(pkg, aliases[i]);
                if (pm.getComponentEnabledSetting(cn) != desired) pm.setComponentEnabledSetting(cn, desired, android.content.pm.PackageManager.DONT_KILL_APP);
            }
        }

        private android.content.Intent buildOreoIntent(android.content.Context ctx, String theme) {
            String pkg = ctx.getPackageName();
            String cls;
            switch (theme) {
                case "launcher1-d": cls="com.donut.launcher.Launcher"; break;
                case "launcher2-g": cls="com.gingerbread.launcher2.Launcher"; break;
                case "launcher2-j": cls="com.jellybean.launcher2.Launcher"; break;
                case "launcher3-m": cls="com.marshmallow.launcher.Launcher"; break;
                case "launcher3-o": cls="com.oreo.launcher3.Launcher"; break;
                case "launcher3-h": cls="com.android.launcher2.Launcher"; break;
                default: cls="com.dismal.unifiedlauncher.MainActivity"; break;
            }
            return new android.content.Intent().setComponent(new android.content.ComponentName(pkg, cls))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        @Override
        public void onDestroy() {
            if (mRotationLockObserver != null) {
                getActivity().getContentResolver().unregisterContentObserver(mRotationLockObserver);
                mRotationLockObserver = null;
            }
            if (mIconBadgingObserver != null) {
                getActivity().getContentResolver().unregisterContentObserver(mIconBadgingObserver);
                mIconBadgingObserver = null;
            }
            super.onDestroy();
        }
    }

    /**
     * Content observer which listens for system auto-rotate setting changes, and enables/disables
     * the launcher rotation setting accordingly.
     */
    private static class SystemDisplayRotationLockObserver extends ContentObserver {

        private final Preference mRotationPref;
        private final ContentResolver mResolver;

        public SystemDisplayRotationLockObserver(
                Preference rotationPref, ContentResolver resolver) {
            super(new Handler());
            mRotationPref = rotationPref;
            mResolver = resolver;
        }

        @Override
        public void onChange(boolean selfChange) {
            boolean enabled = Settings.System.getInt(mResolver,
                    Settings.System.ACCELEROMETER_ROTATION, 1) == 1;
            mRotationPref.setEnabled(enabled);
            mRotationPref.setSummary(enabled
                    ? R.string.allow_rotation_desc : R.string.allow_rotation_blocked_desc);
        }
    }

    /**
     * Content observer which listens for system badging setting changes,
     * and updates the launcher badging setting subtext accordingly.
     */
    private static class IconBadgingObserver extends ContentObserver {

        private final Preference mBadgingPref;
        private final ContentResolver mResolver;

        public IconBadgingObserver(Preference badgingPref, ContentResolver resolver) {
            super(new Handler());
            mBadgingPref = badgingPref;
            mResolver = resolver;
        }

        @Override
        public void onChange(boolean selfChange) {
            boolean enabled = Settings.Secure.getInt(mResolver, NOTIFICATION_BADGING, 1) == 1;
            mBadgingPref.setSummary(enabled
                    ? R.string.icon_badging_desc_on
                    : R.string.icon_badging_desc_off);
        }
    }

}
