package com.android.launcher2;
import com.launcher3h.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;
import java.io.IOException;
import java.util.ArrayList;

public class WallpaperChooserDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {
    /* access modifiers changed from: private */
    public Bitmap mBitmap = null;
    private boolean mEmbedded;
    /* access modifiers changed from: private */
    public ImageView mImageView = null;
    /* access modifiers changed from: private */
    public ArrayList<Integer> mImages;
    /* access modifiers changed from: private */
    public WallpaperLoader mLoader;
    /* access modifiers changed from: private */
    public ArrayList<Integer> mThumbs;

    public static WallpaperChooserDialogFragment newInstance() {
        WallpaperChooserDialogFragment fragment = new WallpaperChooserDialogFragment();
        fragment.setCancelable(true);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null || !savedInstanceState.containsKey("com.android.launcher2.WallpaperChooserDialogFragment.EMBEDDED_KEY")) {
            this.mEmbedded = isInLayout();
        } else {
            this.mEmbedded = savedInstanceState.getBoolean("com.android.launcher2.WallpaperChooserDialogFragment.EMBEDDED_KEY");
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("com.android.launcher2.WallpaperChooserDialogFragment.EMBEDDED_KEY", this.mEmbedded);
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mLoader != null && this.mLoader.getStatus() != AsyncTask.Status.FINISHED) {
            this.mLoader.cancel(true);
            this.mLoader = null;
        }
    }

    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        findWallpapers();
        View v = getActivity().getLayoutInflater().inflate(R.layout.launcher3h_wallpaper_chooser, (ViewGroup) null, false);
        GridView gridView = (GridView) v.findViewById(R.id.gallery);
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(new ImageAdapter(getActivity()));
        int viewInset = getResources().getDimensionPixelSize(R.dimen.alert_dialog_content_inset);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setNegativeButton(R.string.wallpaper_cancel, (DialogInterface.OnClickListener) null);
        builder.setTitle(R.string.wallpaper_dialog_title);
        FrameLayout content = (FrameLayout) v.findViewById(R.id.wallpaper_list);
        content.setPadding(viewInset, viewInset, viewInset, viewInset);
        builder.setView(content);
        return builder.create();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        findWallpapers();
        if (!this.mEmbedded) {
            return null;
        }
        View view = inflater.inflate(R.layout.launcher3h_wallpaper_chooser, container, false);
        final Gallery gallery = (Gallery) view.findViewById(R.id.gallery);
        gallery.setCallbackDuringFling(false);
        gallery.setOnItemSelectedListener(this);
        gallery.setAdapter(new ImageAdapter(getActivity()));
        view.findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WallpaperChooserDialogFragment.this.selectWallpaper(gallery.getSelectedItemPosition());
            }
        });
        this.mImageView = (ImageView) view.findViewById(R.id.wallpaper);
        return view;
    }

    /* access modifiers changed from: private */
    public void selectWallpaper(int position) {
        try {
            ((WallpaperManager) getActivity().getSystemService("wallpaper")).setResource(this.mImages.get(position).intValue());
            Activity activity = getActivity();
            activity.setResult(-1);
            activity.finish();
        } catch (IOException e) {
            Log.e("Launcher.WallpaperChooserDialogFragment", "Failed to set wallpaper: " + e);
        }
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        selectWallpaper(position);
    }

    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        if (!(this.mLoader == null || this.mLoader.getStatus() == AsyncTask.Status.FINISHED)) {
            this.mLoader.cancel();
        }
        this.mLoader = (WallpaperLoader) new WallpaperLoader().execute(new Integer[]{Integer.valueOf(position)});
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void findWallpapers() {
        this.mThumbs = new ArrayList<>(24);
        this.mImages = new ArrayList<>(24);
        Resources resources = getResources();
        String packageName = resources.getResourcePackageName(R.array.wallpapers);
        addWallpapers(resources, packageName, R.array.wallpapers);
        addWallpapers(resources, packageName, R.array.extra_wallpapers);
    }

    private void addWallpapers(Resources resources, String packageName, int list) {
        int thumbRes;
        for (String extra : resources.getStringArray(list)) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (!(res == 0 || (thumbRes = resources.getIdentifier(extra + "_small", "drawable", packageName)) == 0)) {
                this.mThumbs.add(Integer.valueOf(thumbRes));
                this.mImages.add(Integer.valueOf(res));
            }
        }
    }

    private class ImageAdapter extends BaseAdapter implements ListAdapter, SpinnerAdapter {
        private LayoutInflater mLayoutInflater;

        ImageAdapter(Activity activity) {
            this.mLayoutInflater = activity.getLayoutInflater();
        }

        public int getCount() {
            return WallpaperChooserDialogFragment.this.mThumbs.size();
        }

        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        /* Debug info: failed to restart local var, previous not found, register: 7 */
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = this.mLayoutInflater.inflate(R.layout.launcher3h_wallpaper_item, parent, false);
            } else {
                view = convertView;
            }
            ImageView image = (ImageView) view.findViewById(R.id.wallpaper_image);
            int thumbRes = ((Integer) WallpaperChooserDialogFragment.this.mThumbs.get(position)).intValue();
            image.setImageResource(thumbRes);
            Drawable thumbDrawable = image.getDrawable();
            if (thumbDrawable != null) {
                thumbDrawable.setDither(true);
            } else {
                Log.e("Launcher.WallpaperChooserDialogFragment", "Error decoding thumbnail resId=" + thumbRes + " for wallpaper #" + position);
            }
            return view;
        }
    }

    class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
        BitmapFactory.Options mOptions = new BitmapFactory.Options();

        WallpaperLoader() {
            this.mOptions.inDither = false;
            this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }

        /* access modifiers changed from: protected */
        public Bitmap doInBackground(Integer... params) {
            if (isCancelled()) {
                return null;
            }
            try {
                return BitmapFactory.decodeResource(WallpaperChooserDialogFragment.this.getResources(), ((Integer) WallpaperChooserDialogFragment.this.mImages.get(params[0].intValue())).intValue(), this.mOptions);
            } catch (OutOfMemoryError e) {
                OutOfMemoryError outOfMemoryError = e;
                return null;
            }
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Bitmap b) {
            if (b != null) {
                if (isCancelled() || this.mOptions.mCancel) {
                    b.recycle();
                    return;
                }
                if (WallpaperChooserDialogFragment.this.mBitmap != null) {
                    WallpaperChooserDialogFragment.this.mBitmap.recycle();
                }
                ImageView view = WallpaperChooserDialogFragment.this.mImageView;
                if (view != null) {
                    view.setImageBitmap(b);
                    Bitmap unused = WallpaperChooserDialogFragment.this.mBitmap = b;
                    Drawable drawable = view.getDrawable();
                    drawable.setFilterBitmap(true);
                    drawable.setDither(true);
                    view.postInvalidate();
                }
                WallpaperLoader unused2 = WallpaperChooserDialogFragment.this.mLoader = null;
            }
        }

        /* access modifiers changed from: package-private */
        public void cancel() {
            this.mOptions.requestCancelDecode();
            super.cancel(true);
        }
    }
}
