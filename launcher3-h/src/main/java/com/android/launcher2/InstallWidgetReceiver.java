package com.android.launcher2;
import com.launcher3h.R;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import java.util.List;

public class InstallWidgetReceiver {

    public static class WidgetMimeTypeHandlerData {
        public ResolveInfo resolveInfo;
        public AppWidgetProviderInfo widgetInfo;

        public WidgetMimeTypeHandlerData(ResolveInfo rInfo, AppWidgetProviderInfo wInfo) {
            this.resolveInfo = rInfo;
            this.widgetInfo = wInfo;
        }
    }

    public static class WidgetListAdapter implements DialogInterface.OnClickListener, ListAdapter {
        private List<WidgetMimeTypeHandlerData> mActivities;
        private ClipData mClipData;
        private LayoutInflater mInflater;
        private Launcher mLauncher;
        private String mMimeType;
        private CellLayout mTargetLayout;
        private int[] mTargetLayoutPos;
        private int mTargetLayoutScreen;

        public WidgetListAdapter(Launcher l, String mimeType, ClipData data, List<WidgetMimeTypeHandlerData> list, CellLayout target, int targetScreen, int[] targetPos) {
            this.mLauncher = l;
            this.mMimeType = mimeType;
            this.mClipData = data;
            this.mActivities = list;
            this.mTargetLayout = target;
            this.mTargetLayoutScreen = targetScreen;
            this.mTargetLayoutPos = targetPos;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        public int getCount() {
            return this.mActivities.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public boolean hasStableIds() {
            return true;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            PackageManager packageManager = context.getPackageManager();
            if (this.mInflater == null) {
                this.mInflater = LayoutInflater.from(context);
            }
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.launcher3h_external_widget_drop_list_item, parent, false);
            }
            WidgetMimeTypeHandlerData data = this.mActivities.get(position);
            ResolveInfo resolveInfo = data.resolveInfo;
            AppWidgetProviderInfo widgetInfo = data.widgetInfo;
            ((ImageView) convertView.findViewById(R.id.provider_icon)).setImageDrawable(resolveInfo.loadIcon(packageManager));
            CharSequence component = resolveInfo.loadLabel(packageManager);
            int[] widgetSpan = new int[2];
            this.mTargetLayout.rectToCell(widgetInfo.minWidth, widgetInfo.minHeight, widgetSpan);
            ((TextView) convertView.findViewById(R.id.provider)).setText(context.getString(R.string.external_drop_widget_pick_format, new Object[]{component, Integer.valueOf(widgetSpan[0]), Integer.valueOf(widgetSpan[1])}));
            return convertView;
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean isEmpty() {
            return this.mActivities.isEmpty();
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return true;
        }

        public void onClick(DialogInterface dialog, int which) {
            this.mLauncher.addAppWidgetFromDrop(new PendingAddWidgetInfo(this.mActivities.get(which).widgetInfo, this.mMimeType, this.mClipData), this.mTargetLayoutScreen, this.mTargetLayoutPos);
        }
    }
}
