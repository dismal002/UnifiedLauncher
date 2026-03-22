package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.ref.SoftReference;
import java.net.URISyntaxException;
import java.util.HashMap;

class LiveFolderAdapter extends CursorAdapter {
    private final HashMap<Long, SoftReference<Drawable>> mCustomIcons = new HashMap<>();
    private final HashMap<String, Drawable> mIcons = new HashMap<>();
    private LayoutInflater mInflater;
    private boolean mIsList;
    private final Launcher mLauncher;

    LiveFolderAdapter(Launcher launcher, LiveFolderInfo info, Cursor cursor) {
        super(launcher, cursor, true);
        this.mIsList = info.displayMode == 2;
        this.mInflater = LayoutInflater.from(launcher);
        this.mLauncher = launcher;
        this.mLauncher.startManagingCursor(getCursor());
    }

    static Cursor query(Context context, LiveFolderInfo info) {
        return context.getContentResolver().query(info.uri, (String[]) null, (String) null, (String[]) null, "name ASC");
    }

    /* Debug info: failed to restart local var, previous not found, register: 5 */
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view;
        ViewHolder holder = new ViewHolder();
        if (!this.mIsList) {
            view = this.mInflater.inflate(R.layout.application_boxed, parent, false);
        } else {
            view = this.mInflater.inflate(R.layout.application_list, parent, false);
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
        }
        holder.name = (TextView) view.findViewById(R.id.name);
        holder.idIndex = cursor.getColumnIndexOrThrow("_id");
        holder.nameIndex = cursor.getColumnIndexOrThrow("name");
        holder.descriptionIndex = cursor.getColumnIndex("description");
        holder.intentIndex = cursor.getColumnIndex("intent");
        holder.iconBitmapIndex = cursor.getColumnIndex("icon_bitmap");
        holder.iconResourceIndex = cursor.getColumnIndex("icon_resource");
        holder.iconPackageIndex = cursor.getColumnIndex("icon_package");
        view.setTag(holder);
        return view;
    }

    public void bindView(View view, Context context, Cursor cursor) {
        boolean hasIcon;
        int i;
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.id = cursor.getLong(holder.idIndex);
        Drawable icon = loadIcon(context, cursor, holder);
        holder.name.setText(cursor.getString(holder.nameIndex));
        if (!this.mIsList) {
            holder.name.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, icon, (Drawable) null, (Drawable) null);
        } else {
            if (icon != null) {
                hasIcon = true;
            } else {
                hasIcon = false;
            }
            ImageView imageView = holder.icon;
            if (hasIcon) {
                i = 0;
            } else {
                i = 8;
            }
            imageView.setVisibility(i);
            if (hasIcon) {
                holder.icon.setImageDrawable(icon);
            }
            if (holder.descriptionIndex != -1) {
                String description = cursor.getString(holder.descriptionIndex);
                if (description != null) {
                    holder.description.setText(description);
                    holder.description.setVisibility(0);
                } else {
                    holder.description.setVisibility(8);
                }
            } else {
                holder.description.setVisibility(8);
            }
        }
        if (holder.intentIndex != -1) {
            try {
                holder.intent = Intent.parseUri(cursor.getString(holder.intentIndex), 0);
            } catch (URISyntaxException e) {
            }
        } else {
            holder.useBaseIntent = true;
        }
    }

    private Drawable loadIcon(Context context, Cursor cursor, ViewHolder holder) {
        Drawable icon = null;
        byte[] data = null;
        if (holder.iconBitmapIndex != -1) {
            data = cursor.getBlob(holder.iconBitmapIndex);
        }
        if (data != null) {
            SoftReference<Drawable> reference = this.mCustomIcons.get(Long.valueOf(holder.id));
            if (reference != null) {
                icon = reference.get();
            }
            if (icon != null) {
                return icon;
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap resampled = Utilities.resampleIconBitmap(bitmap, context);
            if (bitmap != resampled) {
                bitmap.recycle();
            }
            Drawable icon2 = new FastBitmapDrawable(resampled);
            this.mCustomIcons.put(Long.valueOf(holder.id), new SoftReference(icon2));
            return icon2;
        } else if (holder.iconResourceIndex == -1 || holder.iconPackageIndex == -1) {
            return null;
        } else {
            String resource = cursor.getString(holder.iconResourceIndex);
            Drawable icon3 = this.mIcons.get(resource);
            if (icon3 != null) {
                return icon3;
            }
            try {
                Resources resources = context.getPackageManager().getResourcesForApplication(cursor.getString(holder.iconPackageIndex));
                Drawable icon4 = new FastBitmapDrawable(Utilities.createIconBitmap(resources.getDrawable(resources.getIdentifier(resource, (String) null, (String) null)), context));
                try {
                    this.mIcons.put(resource, icon4);
                    return icon4;
                } catch (Exception e) {
                    return icon4;
                }
            } catch (Exception e2) {
                return icon3;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void cleanup() {
        for (Drawable icon : this.mIcons.values()) {
            icon.setCallback((Drawable.Callback) null);
        }
        this.mIcons.clear();
        for (SoftReference<Drawable> icon2 : this.mCustomIcons.values()) {
            Drawable drawable = icon2.get();
            if (drawable != null) {
                drawable.setCallback((Drawable.Callback) null);
            }
        }
        this.mCustomIcons.clear();
        Cursor cursor = getCursor();
        if (cursor != null) {
            try {
                cursor.close();
            } finally {
                this.mLauncher.stopManagingCursor(cursor);
            }
        }
    }

    static class ViewHolder {
        TextView description;
        int descriptionIndex = -1;
        ImageView icon;
        int iconBitmapIndex = -1;
        int iconPackageIndex = -1;
        int iconResourceIndex = -1;
        long id;
        int idIndex;
        Intent intent;
        int intentIndex = -1;
        TextView name;
        int nameIndex;
        boolean useBaseIntent;

        ViewHolder() {
        }
    }
}
