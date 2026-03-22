package com.android.launcher2;

import java.lang.reflect.Method;

/**
 * Access to {@code android.os.SystemProperties} via reflection. The real class
 * is hidden from the public SDK, so this keeps the launcher buildable outside AOSP.
 */
public final class SystemPropertiesCompat {
    private SystemPropertiesCompat() {
    }

    public static int getInt(String key, int def) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("getInt", String.class, int.class);
            Object value = method.invoke(null, key, def);
            if (value instanceof Integer) {
                return (Integer) value;
            }
            return def;
        } catch (Throwable t) {
            return def;
        }
    }
}

