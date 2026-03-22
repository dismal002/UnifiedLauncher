package com.android.launcher2;
import com.launcher3h.R;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class AddAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final ArrayList<ListItem> mItems = new ArrayList<>();

    public class ListItem {
        public final int actionTag;
        public final Drawable image;
        public final CharSequence text;

        public ListItem(Resources res, int textResourceId, int imageResourceId, int actionTag2) {
            this.text = res.getString(textResourceId);
            if (imageResourceId != -1) {
                this.image = res.getDrawable(imageResourceId);
            } else {
                this.image = null;
            }
            this.actionTag = actionTag2;
        }
    }

    public AddAdapter(Launcher launcher) {
        this.mInflater = (LayoutInflater) launcher.getSystemService("layout_inflater");
        Resources res = launcher.getResources();
        this.mItems.add(new ListItem(res, R.string.group_shortcuts, R.drawable.ic_launcher_shortcut, 0));
        this.mItems.add(new ListItem(res, R.string.group_widgets, R.drawable.ic_launcher_appwidget, 1));
        this.mItems.add(new ListItem(res, R.string.group_live_folders, R.drawable.ic_launcher_folder, 2));
        this.mItems.add(new ListItem(res, R.string.group_wallpapers, R.drawable.ic_launcher_wallpaper, 3));
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem item = (ListItem) getItem(position);
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.launcher3h_add_list_item, parent, false);
        }
        TextView textView = (TextView) convertView;
        textView.setTag(item);
        textView.setText(item.text);
        textView.setCompoundDrawablesWithIntrinsicBounds(item.image, (Drawable) null, (Drawable) null, (Drawable) null);
        return convertView;
    }

    public int getCount() {
        return this.mItems.size();
    }

    public Object getItem(int position) {
        return this.mItems.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }
}
