package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class ShortcutsAdapter extends ArrayAdapter<ShortcutInfo> {
    private final IconCache mIconCache;
    private final LayoutInflater mInflater;

    public ShortcutsAdapter(Context context, ArrayList<ShortcutInfo> apps) {
        super(context, 0, apps);
        this.mInflater = LayoutInflater.from(context);
        this.mIconCache = LauncherApplication.getSharedInstance(context).getIconCache();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ShortcutInfo info = (ShortcutInfo) getItem(position);
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.application_boxed, parent, false);
        }
        TextView textView = (TextView) convertView;
        textView.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new FastBitmapDrawable(info.getIcon(this.mIconCache)), (Drawable) null, (Drawable) null);
        textView.setText(info.title);
        return convertView;
    }
}
