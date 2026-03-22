package com.android.launcher2;
import com.launcher3h.R;

import android.app.SearchManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class LauncherProvider extends ContentProvider {
    public static final String AUTHORITY = "com.dismal.unifiedlauncher.honeycomb.settings";
    static final Uri CONTENT_APPWIDGET_RESET_URI = Uri.parse("content://" + AUTHORITY + "/appWidgetReset");
    private SQLiteOpenHelper mOpenHelper;

    public boolean onCreate() {
        this.mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, (String) null, (String[]) null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        }
        return "vnd.android.cursor.item/" + args.table;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        Cursor result = qb.query(this.mOpenHelper.getWritableDatabase(), projection, args.where, args.args, (String) null, (String) null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    public Uri insert(Uri uri, ContentValues initialValues) {
        long rowId = this.mOpenHelper.getWritableDatabase().insert(new SqlArguments(uri).table, (String) null, initialValues);
        if (rowId <= 0) {
            return null;
        }
        Uri uri2 = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri2);
        return uri2;
    }

    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ContentValues insert : values) {
                if (db.insert(args.table, (String) null, insert) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            sendNotify(uri);
            return values.length;
        } finally {
            db.endTransaction();
        }
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        int count = this.mOpenHelper.getWritableDatabase().delete(args.table, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }
        return count;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        int count = this.mOpenHelper.getWritableDatabase().update(args.table, values, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }
        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter("notify");
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, (ContentObserver) null);
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private final AppWidgetHost mAppWidgetHost;
        private final Context mContext;

        DatabaseHelper(Context context) {
            super(context, "launcher.db", (SQLiteDatabase.CursorFactory) null, 8);
            this.mContext = context;
            this.mAppWidgetHost = new AppWidgetHost(context, 1024);
        }

        private void sendAppWidgetResetNotify() {
            this.mContext.getContentResolver().notifyChange(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, (ContentObserver) null);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE favorites (_id INTEGER PRIMARY KEY,title TEXT,intent TEXT,container INTEGER,screen INTEGER,cellX INTEGER,cellY INTEGER,spanX INTEGER,spanY INTEGER,itemType INTEGER,appWidgetId INTEGER NOT NULL DEFAULT -1,isShortcut INTEGER,iconType INTEGER,iconPackage TEXT,iconResource TEXT,icon BLOB,uri TEXT,displayMode INTEGER);");
            if (this.mAppWidgetHost != null) {
                this.mAppWidgetHost.deleteHost();
                sendAppWidgetResetNotify();
            }
            if (!convertDatabase(db)) {
                loadFavorites(db);
            }
        }

        private boolean convertDatabase(SQLiteDatabase db) {
            boolean converted = false;
            Uri uri = Uri.parse("content://settings/old_favorites?notify=true");
            ContentResolver resolver = this.mContext.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, (String[]) null, (String) null, (String[]) null, (String) null);
            } catch (Exception e) {
            }
            if (cursor != null && cursor.getCount() > 0) {
                try {
                    converted = copyFromCursor(db, cursor) > 0;
                    if (converted) {
                        resolver.delete(uri, (String) null, (String[]) null);
                    }
                } finally {
                    cursor.close();
                }
            }
            if (converted) {
                convertWidgets(db);
            }
            return converted;
        }

        private int copyFromCursor(SQLiteDatabase db, Cursor c) {
            int idIndex = c.getColumnIndexOrThrow("_id");
            int intentIndex = c.getColumnIndexOrThrow("intent");
            int titleIndex = c.getColumnIndexOrThrow("title");
            int iconTypeIndex = c.getColumnIndexOrThrow("iconType");
            int iconIndex = c.getColumnIndexOrThrow("icon");
            int iconPackageIndex = c.getColumnIndexOrThrow("iconPackage");
            int iconResourceIndex = c.getColumnIndexOrThrow("iconResource");
            int containerIndex = c.getColumnIndexOrThrow("container");
            int itemTypeIndex = c.getColumnIndexOrThrow("itemType");
            int screenIndex = c.getColumnIndexOrThrow("screen");
            int cellXIndex = c.getColumnIndexOrThrow("cellX");
            int cellYIndex = c.getColumnIndexOrThrow("cellY");
            int uriIndex = c.getColumnIndexOrThrow("uri");
            int displayModeIndex = c.getColumnIndexOrThrow("displayMode");
            ContentValues[] rows = new ContentValues[c.getCount()];
            int i = 0;
            while (c.moveToNext()) {
                ContentValues values = new ContentValues(c.getColumnCount());
                values.put("_id", Long.valueOf(c.getLong(idIndex)));
                values.put("intent", c.getString(intentIndex));
                values.put("title", c.getString(titleIndex));
                values.put("iconType", Integer.valueOf(c.getInt(iconTypeIndex)));
                values.put("icon", c.getBlob(iconIndex));
                values.put("iconPackage", c.getString(iconPackageIndex));
                values.put("iconResource", c.getString(iconResourceIndex));
                values.put("container", Integer.valueOf(c.getInt(containerIndex)));
                values.put("itemType", Integer.valueOf(c.getInt(itemTypeIndex)));
                values.put("appWidgetId", -1);
                values.put("screen", Integer.valueOf(c.getInt(screenIndex)));
                values.put("cellX", Integer.valueOf(c.getInt(cellXIndex)));
                values.put("cellY", Integer.valueOf(c.getInt(cellYIndex)));
                values.put("uri", c.getString(uriIndex));
                values.put("displayMode", Integer.valueOf(c.getInt(displayModeIndex)));
                rows[i] = values;
                i++;
            }
            db.beginTransaction();
            int total = 0;
            try {
                int numValues = rows.length;
                for (int i2 = 0; i2 < numValues; i2++) {
                    if (db.insert("favorites", (String) null, rows[i2]) < 0) {
                        return 0;
                    }
                    total++;
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                return total;
            } finally {
                db.endTransaction();
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("Launcher.LauncherProvider", "Downgrading database from version " + oldVersion + " to " + newVersion + " — recreating.");
            db.execSQL("DROP TABLE IF EXISTS favorites");
            onCreate(db);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            int version = oldVersion;
            if (version < 3) {
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE favorites ADD COLUMN appWidgetId INTEGER NOT NULL DEFAULT -1;");
                    db.setTransactionSuccessful();
                    version = 3;
                } catch (SQLException e) {
                    SQLException ex = e;
                    Log.e("Launcher.LauncherProvider", ex.getMessage(), ex);
                } finally {
                    db.endTransaction();
                }
                if (version == 3) {
                    convertWidgets(db);
                }
            }
            if (version < 4) {
                version = 4;
            }
            if (version < 6) {
                db.beginTransaction();
                try {
                    db.execSQL("UPDATE favorites SET screen=(screen + 1);");
                    db.setTransactionSuccessful();
                } catch (SQLException e2) {
                    SQLException ex2 = e2;
                    Log.e("Launcher.LauncherProvider", ex2.getMessage(), ex2);
                } finally {
                    db.endTransaction();
                }
                if (updateContactsShortcuts(db)) {
                    version = 6;
                }
            }
            if (version < 7) {
                convertWidgets(db);
                version = 7;
            }
            if (version < 8) {
                normalizeIcons(db);
                version = 8;
            }
            if (version != 8) {
                Log.w("Launcher.LauncherProvider", "Destroying all old data.");
                db.execSQL("DROP TABLE IF EXISTS favorites");
                onCreate(db);
            }
        }

        private boolean updateContactsShortcuts(SQLiteDatabase db) {
            Cursor c = null;
            String selectWhere = LauncherProvider.buildOrWhereString("itemType", new int[]{1});
            db.beginTransaction();
            try {
                c = db.query("favorites", new String[]{"_id", "intent"}, selectWhere, (String[]) null, (String) null, (String) null, (String) null);
                ContentValues values = new ContentValues();
                int idIndex = c.getColumnIndex("_id");
                int intentIndex = c.getColumnIndex("intent");
                while (c != null && c.moveToNext()) {
                    long favoriteId = c.getLong(idIndex);
                    String intentUri = c.getString(intentIndex);
                    if (intentUri != null) {
                        try {
                            Intent intent = Intent.parseUri(intentUri, 0);
                            Log.d("Home", intent.toString());
                            Uri uri = intent.getData();
                            String data = uri.toString();
                            if ("android.intent.action.VIEW".equals(intent.getAction()) && (data.startsWith("content://contacts/people/") || data.startsWith("content://com.android.contacts/contacts/lookup/"))) {
                                Intent intent2 = new Intent("com.android.contacts.action.QUICK_CONTACT");
                                intent2.setFlags(337641472);
                                intent2.setData(uri);
                                intent2.putExtra("mode", 3);
                                intent2.putExtra("exclude_mimes", (String[]) null);
                                values.clear();
                                values.put("intent", intent2.toUri(0));
                                db.update("favorites", values, "_id=" + favoriteId, (String[]) null);
                            }
                        } catch (RuntimeException e) {
                            Log.e("Launcher.LauncherProvider", "Problem upgrading shortcut", e);
                        } catch (URISyntaxException e2) {
                            Log.e("Launcher.LauncherProvider", "Problem upgrading shortcut", e2);
                        }
                    }
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
                return true;
            } catch (SQLException e3) {
                Log.w("Launcher.LauncherProvider", "Problem while upgrading contacts", e3);
                db.endTransaction();
                if (c == null) {
                    return false;
                }
                c.close();
                return false;
            } catch (Throwable th) {
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
                throw th;
            }
        }

        private void normalizeIcons(SQLiteDatabase db) {
            Log.d("Launcher.LauncherProvider", "normalizing icons");
            db.beginTransaction();
            Cursor c = null;
            SQLiteStatement update = null;
            boolean logged = false;
            try {
                update = db.compileStatement("UPDATE favorites SET icon=? WHERE _id=?");
                c = db.rawQuery("SELECT _id, icon FROM favorites WHERE iconType=1", (String[]) null);
                int idIndex = c.getColumnIndexOrThrow("_id");
                int iconIndex = c.getColumnIndexOrThrow("icon");
                while (c.moveToNext()) {
                    long id = c.getLong(idIndex);
                    byte[] data = c.getBlob(iconIndex);
                    try {
                        Bitmap bitmap = Utilities.resampleIconBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), this.mContext);
                        if (bitmap != null) {
                            update.bindLong(1, id);
                            byte[] data2 = ItemInfo.flattenBitmap(bitmap);
                            if (data2 != null) {
                                update.bindBlob(2, data2);
                                update.execute();
                            }
                            bitmap.recycle();
                        }
                    } catch (Exception e) {
                        Exception e2 = e;
                        if (!logged) {
                            Log.e("Launcher.LauncherProvider", "Failed normalizing icon " + id, e2);
                        } else {
                            Log.e("Launcher.LauncherProvider", "Also failed normalizing icon " + id);
                        }
                        logged = true;
                    }
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                if (update != null) {
                    update.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e3) {
                Log.w("Launcher.LauncherProvider", "Problem while allocating appWidgetIds for existing widgets", e3);
                db.endTransaction();
                if (update != null) {
                    update.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (Throwable th) {
                db.endTransaction();
                if (update != null) {
                    update.close();
                }
                if (c != null) {
                    c.close();
                }
                throw th;
            }
        }

        private void convertWidgets(SQLiteDatabase db) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
            String selectWhere = LauncherProvider.buildOrWhereString("itemType", new int[]{1000, 1002, 1001});
            Cursor c = null;
            db.beginTransaction();
            try {
                c = db.query("favorites", new String[]{"_id", "itemType"}, selectWhere, (String[]) null, (String) null, (String) null, (String) null);
                ContentValues values = new ContentValues();
                while (c != null && c.moveToNext()) {
                    long favoriteId = c.getLong(0);
                    int favoriteType = c.getInt(1);
                    try {
                        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                        values.clear();
                        values.put("itemType", 4);
                        values.put("appWidgetId", Integer.valueOf(appWidgetId));
                        if (favoriteType == 1001) {
                            values.put("spanX", 4);
                            values.put("spanY", 1);
                        } else {
                            values.put("spanX", 2);
                            values.put("spanY", 2);
                        }
                        db.update("favorites", values, "_id=" + favoriteId, (String[]) null);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            if (favoriteType == 1000) {
                                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, new ComponentName("com.android.alarmclock", "com.android.alarmclock.AnalogAppWidgetProvider"));
                            } else if (favoriteType == 1002) {
                                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, new ComponentName("com.android.camera", "com.android.camera.PhotoAppWidgetProvider"));
                            } else if (favoriteType == 1001) {
                                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, getSearchWidgetProvider());
                            }
                        }
                    } catch (RuntimeException e) {
                        Log.e("Launcher.LauncherProvider", "Problem allocating appWidgetId", e);
                    }
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e2) {
                Log.w("Launcher.LauncherProvider", "Problem while allocating appWidgetIds for existing widgets", e2);
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
            } catch (Throwable th) {
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
                throw th;
            }
        }

        private int loadFavorites(SQLiteDatabase db) {
            Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
            intent.addCategory("android.intent.category.LAUNCHER");
            ContentValues values = new ContentValues();
            PackageManager packageManager = this.mContext.getPackageManager();
            int i = 0;
            try {
                XmlResourceParser parser = this.mContext.getResources().getXml(R.xml.default_workspace);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                XmlUtilsCompat.beginDocument(parser, "favorites");
                int depth = parser.getDepth();
                while (true) {
                    int type = parser.next();
                    if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                        break;
                    } else if (type == 2) {
                        boolean added = false;
                        String name = parser.getName();
                        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.Favorite);
                        values.clear();
                        values.put("container", -100);
                        values.put("screen", Integer.valueOf(a.getInt(R.styleable.Favorite_screen, 0)));
                        values.put("cellX", Integer.valueOf(a.getInt(R.styleable.Favorite_x, 0)));
                        values.put("cellY", Integer.valueOf(a.getInt(R.styleable.Favorite_y, 0)));
                        if ("favorite".equals(name)) {
                            added = addAppShortcut(db, values, a, packageManager, intent);
                        } else if ("search".equals(name)) {
                            added = addSearchWidget(db, values);
                        } else if ("clock".equals(name)) {
                            added = addClockWidget(db, values);
                        } else if ("appwidget".equals(name)) {
                            added = addAppWidget(db, values, a, packageManager);
                        } else if ("shortcut".equals(name)) {
                            added = addUriShortcut(db, values, a);
                        }
                        if (added) {
                            i++;
                        }
                        a.recycle();
                    }
                }
            } catch (XmlPullParserException e) {
                Log.w("Launcher.LauncherProvider", "Got exception parsing favorites.", e);
            } catch (IOException e2) {
                Log.w("Launcher.LauncherProvider", "Got exception parsing favorites.", e2);
            }
            return i;
        }

        private boolean addAppShortcut(SQLiteDatabase db, ContentValues values, TypedArray a, PackageManager packageManager, Intent intent) {
            String packageName = a.getString(R.styleable.Favorite_packageName);
            String className = a.getString(R.styleable.Favorite_className);
            ComponentName cn = new ComponentName(packageName, className);
            ActivityInfo info;
            try {
                info = packageManager.getActivityInfo(cn, 0);
            } catch (PackageManager.NameNotFoundException e2) {
                try {
                    cn = new ComponentName(packageManager.currentToCanonicalPackageNames(new String[]{packageName})[0], className);
                    info = packageManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException e3) {
                    Log.w("Launcher.LauncherProvider", "Unable to add favorite: " + packageName + "/" + className, e3);
                    return false;
                }
            }
            intent.setComponent(cn);
            intent.setFlags(270532608);
            values.put("intent", intent.toUri(0));
            values.put("title", info.loadLabel(packageManager).toString());
            values.put("itemType", 0);
            values.put("spanX", 1);
            values.put("spanY", 1);
            db.insert("favorites", (String) null, values);
            return true;
        }

        private ComponentName getSearchWidgetProvider() {
            ComponentName searchComponent = ((SearchManager) this.mContext.getSystemService("search")).getGlobalSearchActivity();
            if (searchComponent == null) {
                return null;
            }
            return getProviderInPackage(searchComponent.getPackageName());
        }

        /* Debug info: failed to restart local var, previous not found, register: 7 */
        private ComponentName getProviderInPackage(String packageName) {
            List<AppWidgetProviderInfo> providers = AppWidgetManager.getInstance(this.mContext).getInstalledProviders();
            if (providers == null) {
                return null;
            }
            int providerCount = providers.size();
            for (int i = 0; i < providerCount; i++) {
                ComponentName provider = providers.get(i).provider;
                if (provider != null && provider.getPackageName().equals(packageName)) {
                    return provider;
                }
            }
            return null;
        }

        private boolean addSearchWidget(SQLiteDatabase db, ContentValues values) {
            return addAppWidget(db, values, getSearchWidgetProvider(), 4, 1);
        }

        private boolean addClockWidget(SQLiteDatabase db, ContentValues values) {
            return addAppWidget(db, values, new ComponentName("com.android.alarmclock", "com.android.alarmclock.AnalogAppWidgetProvider"), 2, 2);
        }

        private boolean addAppWidget(SQLiteDatabase db, ContentValues values, TypedArray a, PackageManager packageManager) {
            String packageName = a.getString(R.styleable.Favorite_packageName);
            String className = a.getString(R.styleable.Favorite_className);
            if (packageName == null || className == null) {
                return false;
            }
            boolean hasPackage = true;
            ComponentName cn = new ComponentName(packageName, className);
            try {
                packageManager.getReceiverInfo(cn, 0);
            } catch (Exception e) {
                cn = new ComponentName(packageManager.currentToCanonicalPackageNames(new String[]{packageName})[0], className);
                try {
                    packageManager.getReceiverInfo(cn, 0);
                } catch (Exception e2) {
                    hasPackage = false;
                }
            }
            if (!hasPackage) {
                return false;
            }
            return addAppWidget(db, values, cn, a.getInt(R.styleable.Favorite_spanX, 0), a.getInt(R.styleable.Favorite_spanY, 0));
        }

        private boolean addAppWidget(SQLiteDatabase db, ContentValues values, ComponentName cn, int spanX, int spanY) {
            boolean allocatedAppWidgets = false;
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
            try {
                int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                values.put("itemType", 4);
                values.put("spanX", Integer.valueOf(spanX));
                values.put("spanY", Integer.valueOf(spanY));
                values.put("appWidgetId", Integer.valueOf(appWidgetId));
                db.insert("favorites", (String) null, values);
                allocatedAppWidgets = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn);
                }
                return true;
            } catch (RuntimeException e) {
                Log.e("Launcher.LauncherProvider", "Problem allocating appWidgetId", e);
                return allocatedAppWidgets;
            }
        }

        private boolean addUriShortcut(SQLiteDatabase db, ContentValues values, TypedArray a) {
            Resources r = this.mContext.getResources();
            int iconResId = a.getResourceId(R.styleable.Favorite_icon, 0);
            int titleResId = a.getResourceId(R.styleable.Favorite_title, 0);
            String uri = null;
            try {
                uri = a.getString(R.styleable.Favorite_uri);
                Intent intent = Intent.parseUri(uri, 0);
                if (iconResId == 0 || titleResId == 0) {
                    Log.w("Launcher.LauncherProvider", "Shortcut is missing title or icon resource ID");
                    return false;
                }
                intent.setFlags(268435456);
                values.put("intent", intent.toUri(0));
                values.put("title", r.getString(titleResId));
                values.put("itemType", 1);
                values.put("spanX", 1);
                values.put("spanY", 1);
                values.put("iconType", 0);
                values.put("iconPackage", this.mContext.getPackageName());
                values.put("iconResource", r.getResourceName(iconResId));
                db.insert("favorites", (String) null, values);
                return true;
            } catch (URISyntaxException e) {
                URISyntaxException uRISyntaxException = e;
                Log.w("Launcher.LauncherProvider", "Shortcut has malformed uri: " + uri);
                return false;
            }
        }
    }

    static String buildOrWhereString(String column, int[] values) {
        StringBuilder selectWhere = new StringBuilder();
        for (int i = values.length - 1; i >= 0; i--) {
            selectWhere.append(column).append("=").append(values[i]);
            if (i > 0) {
                selectWhere.append(" OR ");
            }
        }
        return selectWhere.toString();
    }

    static class SqlArguments {
        public final String[] args;
        public final String table;
        public final String where;

        SqlArguments(Uri url, String where2, String[] args2) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where2;
                this.args = args2;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where2)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = null;
                this.args = null;
                return;
            }
            throw new IllegalArgumentException("Invalid URI: " + url);
        }
    }
}
