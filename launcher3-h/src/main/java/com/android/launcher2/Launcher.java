package com.android.launcher2;
import com.launcher3h.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher2.CellLayout;
import com.android.launcher2.LauncherModel;
import com.android.launcher2.Workspace;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class Launcher extends Activity implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener, LauncherModel.Callbacks {
    private static Drawable.ConstantState sAppMarketIcon;
    /* access modifiers changed from: private */
    public static HashMap<Long, FolderInfo> sFolders = new HashMap<>();
    private static Drawable.ConstantState sGlobalSearchIcon;
    /* access modifiers changed from: private */
    public static LocaleConfiguration sLocaleConfiguration = null;
    private static final Object sLock = new Object();
    private static ArrayList<PendingAddArguments> sPendingAddList = new ArrayList<>();
    private static int sScreen = 2;
    private static Drawable.ConstantState sVoiceSearchIcon;
    private final int ADVANCE_MSG = 1;
    private int[] mAddDropPosition;
    private int mAddIntersectCellX = -1;
    private int mAddIntersectCellY = -1;
    private int mAddScreen = -1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = 250;
    private View mAllAppsButton;
    private AllAppsView mAllAppsGrid;
    private AllAppsPagedView mAllAppsPagedView = null;
    private Intent mAppMarketIntent = null;
    /* access modifiers changed from: private */
    public LauncherAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private boolean mAttached = false;
    private boolean mAutoAdvanceRunning = false;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    private View mButtonCluster;
    private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver();
    private View mConfigureButton;
    private CustomizePagedView mCustomizePagedView = null;
    private SpannableStringBuilder mDefaultKeySsb = null;
    private DeleteZone mDeleteZone;
    private ArrayList<ItemInfo> mDesktopItems = new ArrayList<>();
    private View mDivider;
    private DragController mDragController;
    /* access modifiers changed from: private */
    public FolderInfo mFolderInfo;
    private HandleView mHandleView;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                int i = 0;
                for (View key : Launcher.this.mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(((AppWidgetProviderInfo) Launcher.this.mWidgetsToAdvance.get(key)).autoAdvanceViewId);
                    int delay = i * 250;
                    if (v instanceof Advanceable) {
                        postDelayed(new Runnable() {
                            public void run() {
                                ((Advanceable) v).advance();
                            }
                        }, (long) delay);
                    }
                    i++;
                }
                Launcher.this.sendAdvanceMessage(20000);
            }
        }
    };
    private CustomizeTrayTabHost mHomeCustomizationDrawer;
    private String[] mHotseatConfig = null;
    private Drawable[] mHotseatIcons = null;
    private CharSequence[] mHotseatLabels = null;
    private Intent[] mHotseats = null;
    private IconCache mIconCache;
    private LayoutInflater mInflater;
    /* access modifiers changed from: private */
    public LauncherModel mModel;
    private ImageView mNextView;
    private boolean mOnResumeNeedsLoad;
    /* access modifiers changed from: private */
    public boolean mPaused = true;
    private ImageView mPreviousView;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                boolean unused = Launcher.this.mUserPresent = false;
                Launcher.this.updateRunning();
            } else if ("android.intent.action.USER_PRESENT".equals(action)) {
                boolean unused2 = Launcher.this.mUserPresent = true;
                Launcher.this.updateRunning();
            }
        }
    };
    private final int mRestoreScreenOrientationDelay = 500;
    private boolean mRestoring;
    private Bundle mSavedInstanceState;
    private Bundle mSavedState;
    private State mState = State.WORKSPACE;
    private AnimatorSet mStateAnimation;
    private int[] mTmpAddItemCellCoordinates = new int[2];
    /* access modifiers changed from: private */
    public boolean mUserPresent = true;
    private boolean mVisible = false;
    /* access modifiers changed from: private */
    public boolean mWaitingForResult;
    private BubbleTextView mWaitingForResume;
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();
    /* access modifiers changed from: private */
    public HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance = new HashMap<>();
    /* access modifiers changed from: private */
    public Workspace mWorkspace;
    /* access modifiers changed from: private */
    public boolean mWorkspaceLoading = true;

    private enum State {
        WORKSPACE,
        ALL_APPS,
        CUSTOMIZE,
        CUSTOMIZE_SPRING_LOADED,
        ALL_APPS_SPRING_LOADED
    }

    private static class PendingAddArguments {
        int cellX;
        int cellY;
        Intent intent;
        int requestCode;
        int screen;

        private PendingAddArguments() {
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LauncherApplication.isInPlaceRotationEnabled()) {
            getWindow().setFlags(1024, 1024);
            setRequestedOrientation(4);
        }
        LauncherApplication app;
        if (getApplication() instanceof LauncherApplication) {
            app = (LauncherApplication) getApplication();
        } else {
            // Running inside a unified host app — use the shared singleton
            app = LauncherApplication.getSharedInstance(this);
        }
        this.mModel = app.setLauncher(this);
        this.mIconCache = app.getIconCache();
        this.mDragController = new DragController(this);
        this.mInflater = getLayoutInflater();
        this.mAppWidgetManager = AppWidgetManager.getInstance(this);
        this.mAppWidgetHost = new LauncherAppWidgetHost(this, 1024);
        this.mAppWidgetHost.startListening();
        loadHotseats();
        checkForLocaleChange();
        setContentView(R.layout.launcher3h_launcher);
        this.mHomeCustomizationDrawer = (CustomizeTrayTabHost) findViewById(R.id.customization_drawer);
        if (this.mHomeCustomizationDrawer != null) {
            this.mCustomizePagedView = (CustomizePagedView) findViewById(R.id.customization_drawer_tab_contents);
        }
        setupViews();
        registerContentObservers();
        lockAllApps();
        this.mSavedState = savedInstanceState;
        restoreState(this.mSavedState);
        if (this.mCustomizePagedView != null) {
            this.mCustomizePagedView.update();
        }
        if (!this.mRestoring) {
            this.mModel.startLoader(this, true);
        }
        this.mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(this.mDefaultKeySsb, 0);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(this.mCloseSystemDialogsReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"), android.content.Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(this.mCloseSystemDialogsReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        }
        if (LauncherApplication.isScreenXLarge()) {
            if (sGlobalSearchIcon == null || sVoiceSearchIcon == null || sAppMarketIcon == null) {
                updateIconsAffectedByPackageManagerChanges();
            }
            if (sGlobalSearchIcon != null) {
                updateGlobalSearchIcon(sGlobalSearchIcon);
            }
            if (sVoiceSearchIcon != null) {
                updateVoiceSearchIcon(sVoiceSearchIcon);
            }
            if (sAppMarketIcon != null) {
                updateAppMarketIcon(sAppMarketIcon);
            }
        }
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.dispatchPopulateAccessibilityEvent(event);
        event.getText().clear();
        if (this.mState == State.ALL_APPS) {
            event.getText().add(getString(R.string.all_apps_button_label));
            return true;
        } else if (this.mState != State.WORKSPACE) {
            return true;
        } else {
            event.getText().add(getString(R.string.all_apps_home_button_label));
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void checkForLocaleChange() {
        boolean localeChanged;
        if (sLocaleConfiguration == null) {
            new AsyncTask<Void, Void, LocaleConfiguration>() {
                /* access modifiers changed from: protected */
                public LocaleConfiguration doInBackground(Void... unused) {
                    LocaleConfiguration localeConfiguration = new LocaleConfiguration();
                    Launcher.readConfiguration(Launcher.this, localeConfiguration);
                    return localeConfiguration;
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(LocaleConfiguration result) {
                    LocaleConfiguration unused = Launcher.sLocaleConfiguration = result;
                    Launcher.this.checkForLocaleChange();
                }
            }.execute(new Void[0]);
            return;
        }
        Configuration configuration = getResources().getConfiguration();
        String previousLocale = sLocaleConfiguration.locale;
        String locale = configuration.locale.toString();
        int previousMcc = sLocaleConfiguration.mcc;
        int mcc = configuration.mcc;
        int previousMnc = sLocaleConfiguration.mnc;
        int mnc = configuration.mnc;
        if (locale.equals(previousLocale) && mcc == previousMcc && mnc == previousMnc) {
            localeChanged = false;
        } else {
            localeChanged = true;
        }
        if (localeChanged) {
            sLocaleConfiguration.locale = locale;
            sLocaleConfiguration.mcc = mcc;
            sLocaleConfiguration.mnc = mnc;
            this.mIconCache.flush();
            loadHotseats();
            final LocaleConfiguration localeConfiguration = sLocaleConfiguration;
            new Thread("WriteLocaleConfiguration") {
                public void run() {
                    Launcher.writeConfiguration(Launcher.this, localeConfiguration);
                }
            }.start();
        }
    }

    private static class LocaleConfiguration {
        public String locale;
        public int mcc;
        public int mnc;

        private LocaleConfiguration() {
            this.mcc = -1;
            this.mnc = -1;
        }
    }

    /* access modifiers changed from: private */
    public static void readConfiguration(Context context, LocaleConfiguration configuration) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(context.openFileInput("launcher.preferences"));
            configuration.locale = in.readUTF();
            configuration.mcc = in.readInt();
            configuration.mnc = in.readInt();
        } catch (FileNotFoundException e) {
            // First run; it's fine.
        } catch (IOException e) {
            // Corrupt prefs; ignore and fall back to current locale.
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x002c A[SYNTHETIC, Splitter:B:13:0x002c] */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x003f A[SYNTHETIC, Splitter:B:22:0x003f] */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0048 A[SYNTHETIC, Splitter:B:27:0x0048] */
    /* JADX WARNING: Removed duplicated region for block: B:40:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:41:? A[RETURN, SYNTHETIC] */
    public static void writeConfiguration(Context context, LocaleConfiguration configuration) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(context.openFileOutput("launcher.preferences", 0));
            out.writeUTF(configuration.locale != null ? configuration.locale : "");
            out.writeInt(configuration.mcc);
            out.writeInt(configuration.mnc);
            out.flush();
        } catch (FileNotFoundException e) {
            // Ignore.
        } catch (IOException e) {
            // Clean up corrupt file so next boot can recover.
            context.getFileStreamPath("launcher.preferences").delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }
    }

    static int getScreen() {
        int i;
        synchronized (sLock) {
            i = sScreen;
        }
        return i;
    }

    static void setScreen(int screen) {
        synchronized (sLock) {
            sScreen = screen;
        }
    }

    private Uri getDefaultBrowserUri() {
        String url = getString(R.string.default_browser_url);
        if (url.indexOf("{CID}") != -1) {
            url = url.replace("{CID}", "android-google");
        }
        return Uri.parse(url);
    }

    private void loadHotseats() {
        Uri defaultBrowserUri;
        if (this.mHotseatConfig == null) {
            this.mHotseatConfig = getResources().getStringArray(R.array.hotseats);
            if (this.mHotseatConfig.length > 0) {
                this.mHotseats = new Intent[this.mHotseatConfig.length];
                this.mHotseatLabels = new CharSequence[this.mHotseatConfig.length];
                this.mHotseatIcons = new Drawable[this.mHotseatConfig.length];
            } else {
                this.mHotseats = null;
                this.mHotseatIcons = null;
                this.mHotseatLabels = null;
            }
            TypedArray hotseatIconDrawables = getResources().obtainTypedArray(R.array.hotseat_icons);
            for (int i = 0; i < this.mHotseatConfig.length; i++) {
                try {
                    this.mHotseatIcons[i] = hotseatIconDrawables.getDrawable(i);
                } catch (ArrayIndexOutOfBoundsException e) {
                    ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException = e;
                    Log.w("Launcher", "Missing hotseat_icons array item #" + i);
                    this.mHotseatIcons[i] = null;
                }
            }
            hotseatIconDrawables.recycle();
        }
        PackageManager pm = getPackageManager();
        for (int i2 = 0; i2 < this.mHotseatConfig.length; i2++) {
            Intent intent = null;
            if (this.mHotseatConfig[i2].equals("*BROWSER*")) {
                String defaultUri = getString(R.string.default_browser_url);
                if (defaultUri != null) {
                    defaultBrowserUri = Uri.parse(defaultUri);
                } else {
                    defaultBrowserUri = getDefaultBrowserUri();
                }
                intent = new Intent("android.intent.action.VIEW", defaultBrowserUri).addCategory("android.intent.category.BROWSABLE");
            } else {
                try {
                    intent = Intent.parseUri(this.mHotseatConfig[i2], 0);
                } catch (URISyntaxException e2) {
                    URISyntaxException uRISyntaxException = e2;
                    Log.w("Launcher", "Invalid hotseat intent: " + this.mHotseatConfig[i2]);
                }
            }
            if (intent == null) {
                this.mHotseats[i2] = null;
                this.mHotseatLabels[i2] = getText(R.string.activity_not_found);
            } else {
                ResolveInfo bestMatch = pm.resolveActivity(intent, 65536);
                List<ResolveInfo> allMatches = pm.queryIntentActivities(intent, 65536);
                if (allMatches.size() == 0 || bestMatch == null) {
                    this.mHotseats[i2] = intent;
                    this.mHotseatLabels[i2] = getText(R.string.activity_not_found);
                } else {
                    boolean found = false;
                    Iterator<ResolveInfo> it = allMatches.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        ResolveInfo ri = it.next();
                        if (bestMatch.activityInfo.name.equals(ri.activityInfo.name) && bestMatch.activityInfo.applicationInfo.packageName.equals(ri.activityInfo.applicationInfo.packageName)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        this.mHotseats[i2] = intent;
                        this.mHotseatLabels[i2] = getText(R.string.title_select_shortcut);
                    } else {
                        this.mHotseats[i2] = new Intent("android.intent.action.MAIN").setComponent(new ComponentName(bestMatch.activityInfo.applicationInfo.packageName, bestMatch.activityInfo.name));
                        this.mHotseatLabels[i2] = bestMatch.activityInfo.loadLabel(pm);
                    }
                }
            }
        }
    }

    private void completeAdd(PendingAddArguments args) {
        switch (args.requestCode) {
            case 1:
                completeAddShortcut(args.intent, args.screen, args.cellX, args.cellY);
                return;
            case 4:
                completeAddLiveFolder(args.intent, args.screen, args.cellX, args.cellY);
                return;
            case 5:
                completeAddAppWidget(args.intent.getIntExtra("appWidgetId", -1), args.screen);
                return;
            case 6:
                completeAddApplication(args.intent, args.screen, args.cellX, args.cellY);
                return;
            case 7:
                processShortcut(args.intent);
                return;
            case 8:
                addLiveFolder(args.intent);
                return;
            case 9:
                addAppWidgetFromPick(args.intent);
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        int appWidgetId;
        this.mWaitingForResult = false;
        if (resultCode == -1 && this.mAddScreen != -1) {
            PendingAddArguments args = new PendingAddArguments();
            args.requestCode = requestCode;
            args.intent = data;
            args.screen = this.mAddScreen;
            args.cellX = this.mAddIntersectCellX;
            args.cellY = this.mAddIntersectCellY;
            if (isWorkspaceLocked()) {
                sPendingAddList.add(args);
            } else {
                completeAdd(args);
            }
        } else if ((requestCode == 9 || requestCode == 5) && resultCode == 0 && data != null && (appWidgetId = data.getIntExtra("appWidgetId", -1)) != -1) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        }
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        this.mPaused = false;
        if (this.mRestoring || this.mOnResumeNeedsLoad) {
            this.mWorkspaceLoading = true;
            this.mModel.setAllowLoadFromCache();
            this.mModel.startLoader(this, true);
            this.mRestoring = false;
            this.mOnResumeNeedsLoad = false;
        }
        if (this.mWaitingForResume != null) {
            this.mWaitingForResume.setStayPressed(false);
        }
        updateAppMarketIcon();
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        if (this.mPreviousView != null) {
            dismissPreview(this.mPreviousView);
        }
        if (this.mNextView != null) {
            dismissPreview(this.mNextView);
        }
        this.mPaused = true;
        this.mDragController.cancelDrag();
    }

    public Object onRetainNonConfigurationInstance() {
        this.mModel.stopLoader();
        this.mAllAppsGrid.surrender();
        return Boolean.TRUE;
    }

    private boolean acceptFilter() {
        return !((InputMethodManager) getSystemService("input_method")).isFullscreenMode();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = super.onKeyDown(keyCode, event);
        if (!handled && acceptFilter() && keyCode != 66 && TextKeyListener.getInstance().onKeyDown(this.mWorkspace, this.mDefaultKeySsb, keyCode, event) && this.mDefaultKeySsb != null && this.mDefaultKeySsb.length() > 0) {
            return onSearchRequested();
        }
        if (keyCode != 82 || !event.isLongPress()) {
            return handled;
        }
        return true;
    }

    private String getTypedText() {
        return this.mDefaultKeySsb.toString();
    }

    private void clearTypedText() {
        this.mDefaultKeySsb.clear();
        this.mDefaultKeySsb.clearSpans();
        Selection.setSelection(this.mDefaultKeySsb, 0);
    }

    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                return stateValues[i];
            }
        }
        return state;
    }

    private void restoreState(Bundle bundle) {
        String string;
        if (bundle != null) {
            State intToState = intToState(bundle.getInt("launcher.state", State.WORKSPACE.ordinal()));
            if (intToState == State.ALL_APPS) {
                showAllApps(false);
            } else if (intToState == State.CUSTOMIZE) {
                showCustomizationDrawer(false);
            }
            int i = bundle.getInt("launcher.current_screen", -1);
            if (i > -1) {
                this.mWorkspace.setCurrentPage(i);
            }
            int i2 = bundle.getInt("launcher.add_screen", -1);
            if (i2 > -1) {
                this.mAddScreen = i2;
                this.mAddIntersectCellX = bundle.getInt("launcher.add_cellX");
                this.mAddIntersectCellY = bundle.getInt("launcher.add_cellY");
                this.mRestoring = true;
            }
            if (bundle.getBoolean("launcher.rename_folder", false)) {
                this.mFolderInfo = this.mModel.getFolderById(this, sFolders, bundle.getLong("launcher.rename_folder_id"));
                this.mRestoring = true;
            }
            if (this.mAllAppsGrid != null && (this.mAllAppsGrid instanceof AllAppsTabbed)) {
                String string2 = bundle.getString("allapps_currentTab");
                if (string2 != null) {
                    ((AllAppsTabbed) this.mAllAppsGrid).setCurrentTabByTag(string2);
                }
                int i3 = bundle.getInt("allapps_currentPage", -1);
                if (i3 > -1) {
                    this.mAllAppsPagedView.setRestorePage(i3);
                }
            }
            if (this.mHomeCustomizationDrawer != null && (string = bundle.getString("customize_currentTab")) != null) {
                this.mCustomizePagedView.setCustomizationFilter(this.mHomeCustomizationDrawer.getCustomizeFilterForTabTag(string));
                this.mHomeCustomizationDrawer.setCurrentTabByTag(string);
            }
        }
    }

    private void setupViews() {
        DragController dragController = this.mDragController;
        DragLayer dragLayer = (DragLayer) findViewById(R.id.drag_layer);
        dragLayer.setDragController(dragController);
        this.mAllAppsGrid = (AllAppsView) dragLayer.findViewById(R.id.all_apps_view);
        this.mAllAppsGrid.setLauncher(this);
        this.mAllAppsGrid.setDragController(dragController);
        ((View) this.mAllAppsGrid).setWillNotDraw(false);
        ((View) this.mAllAppsGrid).setFocusable(false);
        if (LauncherApplication.isScreenXLarge()) {
            ((View) this.mAllAppsGrid).setVisibility(4);
            if (this.mHomeCustomizationDrawer != null) {
                this.mHomeCustomizationDrawer.setVisibility(4);
            }
        }
        this.mWorkspace = (Workspace) dragLayer.findViewById(R.id.workspace);
        Workspace workspace = this.mWorkspace;
        workspace.setHapticFeedbackEnabled(false);
        DeleteZone deleteZone = (DeleteZone) dragLayer.findViewById(R.id.delete_zone);
        this.mDeleteZone = deleteZone;
        View findViewById = findViewById(R.id.all_apps_button);
        if (findViewById != null && (findViewById instanceof HandleView)) {
            this.mHandleView = (HandleView) findViewById;
            this.mHandleView.setLauncher(this);
            this.mHandleView.setOnClickListener(this);
            this.mHandleView.setOnLongClickListener(this);
        }
        if (this.mCustomizePagedView != null) {
            this.mCustomizePagedView.setLauncher(this);
            this.mCustomizePagedView.setDragController(dragController);
            this.mCustomizePagedView.setAllAppsPagedView(this.mAllAppsPagedView);
        } else {
            ImageView imageView = (ImageView) findViewById(R.id.hotseat_left);
            imageView.setContentDescription(this.mHotseatLabels[0]);
            imageView.setImageDrawable(this.mHotseatIcons[0]);
            ImageView imageView2 = (ImageView) findViewById(R.id.hotseat_right);
            imageView2.setContentDescription(this.mHotseatLabels[1]);
            imageView2.setImageDrawable(this.mHotseatIcons[1]);
            this.mPreviousView = (ImageView) dragLayer.findViewById(R.id.previous_screen);
            this.mNextView = (ImageView) dragLayer.findViewById(R.id.next_screen);
            this.mWorkspace.setIndicators(this.mPreviousView.getDrawable(), this.mNextView.getDrawable());
            this.mPreviousView.setHapticFeedbackEnabled(false);
            this.mPreviousView.setOnLongClickListener(this);
            this.mNextView.setHapticFeedbackEnabled(false);
            this.mNextView.setOnLongClickListener(this);
        }
        workspace.setOnLongClickListener(this);
        workspace.setDragController(dragController);
        workspace.setLauncher(this);
        workspace.setWallpaperDimension();
        deleteZone.setLauncher(this);
        deleteZone.setDragController(dragController);
        View findViewById2 = findViewById(R.id.all_apps_button);
        View findViewById3 = findViewById(R.id.divider);
        View findViewById4 = findViewById(R.id.configure_button);
        if (LauncherApplication.isScreenXLarge()) {
            deleteZone.setOverlappingViews(new View[]{findViewById2, findViewById3, findViewById4});
        } else {
            deleteZone.setOverlappingView(findViewById(R.id.all_apps_button_cluster));
        }
        dragController.addDragListener(deleteZone);
        DeleteZone deleteZone2 = (DeleteZone) findViewById(R.id.all_apps_delete_zone);
        if (deleteZone2 != null) {
            deleteZone2.setLauncher(this);
            deleteZone2.setDragController(dragController);
            deleteZone2.setDragAndDropEnabled(false);
            dragController.addDragListener(deleteZone2);
            dragController.addDropTarget(deleteZone2);
        }
        ApplicationInfoDropTarget applicationInfoDropTarget = (ApplicationInfoDropTarget) findViewById(R.id.all_apps_info_target);
        if (applicationInfoDropTarget != null) {
            applicationInfoDropTarget.setLauncher(this);
            dragController.addDragListener(applicationInfoDropTarget);
            applicationInfoDropTarget.setDragAndDropEnabled(false);
            View findViewById5 = findViewById(R.id.market_button);
            if (findViewById5 != null) {
                applicationInfoDropTarget.setOverlappingView(findViewById5);
                findViewById5.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Launcher.this.onClickAppMarketButton(v);
                    }
                });
            }
        }
        dragController.setDragScoller(workspace);
        dragController.setScrollView(dragLayer);
        dragController.setMoveTarget(workspace);
        dragController.addDropTarget(workspace);
        dragController.addDropTarget(deleteZone);
        if (applicationInfoDropTarget != null) {
            dragController.addDropTarget(applicationInfoDropTarget);
        }
        if (deleteZone2 != null) {
            dragController.addDropTarget(deleteZone2);
        }
        this.mButtonCluster = findViewById(R.id.all_apps_button_cluster);
        this.mAllAppsButton = findViewById(R.id.all_apps_button);
        this.mDivider = findViewById(R.id.divider);
        this.mConfigureButton = findViewById(R.id.configure_button);
        if (this.mConfigureButton != null) {
            this.mConfigureButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Launcher.this.onClickConfigureButton(v);
                }
            });
        }
        if (this.mDivider != null) {
            this.mDivider.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Launcher.this.onClickAllAppsButton(v);
                }
            });
        }
        if (this.mAllAppsButton != null) {
            this.mAllAppsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Launcher.this.onClickAllAppsButton(v);
                }
            });
        }
    }

    public void previousScreen(View v) {
        if (this.mState != State.ALL_APPS) {
            this.mWorkspace.scrollLeft();
        }
    }

    public void nextScreen(View v) {
        if (this.mState != State.ALL_APPS) {
            this.mWorkspace.scrollRight();
        }
    }

    public void launchHotSeat(View view) {
        if (this.mState != State.ALL_APPS) {
            int i = -1;
            if (view.getId() == R.id.hotseat_left) {
                i = 0;
            } else if (view.getId() == R.id.hotseat_right) {
                i = 1;
            }
            loadHotseats();
            if (i >= 0 && i < this.mHotseats.length && this.mHotseats[i] != null) {
                startActivitySafely(this.mHotseats[i], "hotseat");
            }
        }
    }

    /* access modifiers changed from: package-private */
    public View createShortcut(ShortcutInfo shortcutInfo) {
        return createShortcut(R.layout.application, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), shortcutInfo);
    }

    /* access modifiers changed from: package-private */
    public View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) this.mInflater.inflate(layoutResId, parent, false);
        favorite.applyFromShortcutInfo(info, this.mIconCache);
        favorite.setOnClickListener(this);
        return favorite;
    }

    /* access modifiers changed from: package-private */
    public void completeAddApplication(Intent intent, int i, int i2, int i3) {
        int[] iArr = this.mTmpAddItemCellCoordinates;
        if (!((CellLayout) this.mWorkspace.getChildAt(i)).findCellForSpanThatIntersects(iArr, 1, 1, i2, i3)) {
            showOutOfSpaceMessage();
            return;
        }
        ShortcutInfo shortcutInfo = this.mModel.getShortcutInfo(getPackageManager(), intent, this);
        if (shortcutInfo != null) {
            shortcutInfo.setActivity(intent.getComponent(), 270532608);
            shortcutInfo.container = -1;
            this.mWorkspace.addApplicationShortcut(shortcutInfo, i, iArr[0], iArr[1], isWorkspaceLocked(), this.mAddIntersectCellX, this.mAddIntersectCellY);
            return;
        }
        Log.e("Launcher", "Couldn't find ActivityInfo for selected application: " + intent);
    }

    private void completeAddShortcut(Intent data, int screen, int intersectCellX, int intersectCellY) {
        boolean foundCellSpan;
        int[] cellXY = this.mTmpAddItemCellCoordinates;
        CellLayout layout = (CellLayout) this.mWorkspace.getChildAt(screen);
        int[] touchXY = this.mAddDropPosition;
        if (touchXY != null) {
            foundCellSpan = ((CellLayout) this.mWorkspace.getChildAt(screen)).findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY) != null;
        } else {
            foundCellSpan = layout.findCellForSpanThatIntersects(cellXY, 1, 1, intersectCellX, intersectCellY);
        }
        if (!foundCellSpan) {
            showOutOfSpaceMessage();
            return;
        }
        ShortcutInfo info = this.mModel.addShortcut(this, data, screen, cellXY[0], cellXY[1], false);
        if (!this.mRestoring) {
            this.mWorkspace.addInScreen(createShortcut(info), screen, cellXY[0], cellXY[1], 1, 1, isWorkspaceLocked());
        }
    }

    private void completeAddAppWidget(int i, int i2) {
        boolean findCellForSpanThatIntersects;
        AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(i);
        CellLayout cellLayout = (CellLayout) this.mWorkspace.getChildAt(i2);
        int[] rectToCell = cellLayout.rectToCell(appWidgetInfo.minWidth, appWidgetInfo.minHeight, (int[]) null);
        int[] iArr = this.mTmpAddItemCellCoordinates;
        int[] iArr2 = this.mAddDropPosition;
        if (iArr2 != null) {
            findCellForSpanThatIntersects = ((CellLayout) this.mWorkspace.getChildAt(i2)).findNearestVacantArea(iArr2[0], iArr2[1], rectToCell[0], rectToCell[1], iArr) != null;
        } else {
            findCellForSpanThatIntersects = cellLayout.findCellForSpanThatIntersects(iArr, rectToCell[0], rectToCell[1], this.mAddIntersectCellX, this.mAddIntersectCellY);
        }
        if (!findCellForSpanThatIntersects) {
            if (i != -1) {
                LauncherAppWidgetHost launcherAppWidgetHost = this.mAppWidgetHost;
                final int i3 = i;
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        Launcher.this.mAppWidgetHost.deleteAppWidgetId(i3);
                    }
                }.start();
            }
            showOutOfSpaceMessage();
            return;
        }
        LauncherAppWidgetInfo launcherAppWidgetInfo = new LauncherAppWidgetInfo(i);
        launcherAppWidgetInfo.spanX = rectToCell[0];
        launcherAppWidgetInfo.spanY = rectToCell[1];
        LauncherModel.addItemToDatabase(this, launcherAppWidgetInfo, -100, i2, iArr[0], iArr[1], false);
        if (!this.mRestoring) {
            this.mDesktopItems.add(launcherAppWidgetInfo);
            launcherAppWidgetInfo.hostView = this.mAppWidgetHost.createView(this, i, appWidgetInfo);
            launcherAppWidgetInfo.hostView.setAppWidget(i, appWidgetInfo);
            launcherAppWidgetInfo.hostView.setTag(launcherAppWidgetInfo);
            this.mWorkspace.addInScreen(launcherAppWidgetInfo.hostView, i2, iArr[0], iArr[1], launcherAppWidgetInfo.spanX, launcherAppWidgetInfo.spanY, isWorkspaceLocked());
            addWidgetToAutoAdvanceIfNeeded(launcherAppWidgetInfo.hostView, appWidgetInfo);
        }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(this.mReceiver, intentFilter, android.content.Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(this.mReceiver, intentFilter);
        }
        this.mAttached = true;
        this.mVisible = true;
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mVisible = false;
        if (this.mAttached) {
            unregisterReceiver(this.mReceiver);
            this.mAttached = false;
        }
        updateRunning();
    }

    public void onWindowVisibilityChanged(int visibility) {
        this.mVisible = visibility == 0;
        updateRunning();
    }

    /* access modifiers changed from: private */
    public void sendAdvanceMessage(long delay) {
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), delay);
        this.mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    /* access modifiers changed from: private */
    public void updateRunning() {
        boolean autoAdvanceRunning;
        if (!this.mVisible || !this.mUserPresent || this.mWidgetsToAdvance.isEmpty()) {
            autoAdvanceRunning = false;
        } else {
            autoAdvanceRunning = true;
        }
        if (autoAdvanceRunning != this.mAutoAdvanceRunning) {
            this.mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                sendAdvanceMessage(this.mAutoAdvanceTimeLeft == -1 ? 20000 : this.mAutoAdvanceTimeLeft);
                return;
            }
            if (!this.mWidgetsToAdvance.isEmpty()) {
                this.mAutoAdvanceTimeLeft = Math.max(0, 20000 - (System.currentTimeMillis() - this.mAutoAdvanceSentTime));
            }
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(0);
        }
    }

    /* access modifiers changed from: package-private */
    public void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo.autoAdvanceViewId != -1) {
            View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
            if (v instanceof Advanceable) {
                this.mWidgetsToAdvance.put(hostView, appWidgetInfo);
                ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
                updateRunning();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void removeWidgetToAutoAdvance(View hostView) {
        if (this.mWidgetsToAdvance.containsKey(hostView)) {
            this.mWidgetsToAdvance.remove(hostView);
            updateRunning();
        }
    }

    public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
        this.mDesktopItems.remove(launcherInfo);
        removeWidgetToAutoAdvance(launcherInfo.hostView);
        launcherInfo.hostView = null;
    }

    /* access modifiers changed from: package-private */
    public void showOutOfSpaceMessage() {
        Toast.makeText(this, getString(R.string.out_of_space), 0).show();
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return this.mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return this.mModel;
    }

    /* access modifiers changed from: package-private */
    public void closeSystemDialogs() {
        getWindow().closeAllPanels();
        try {
            dismissDialog(1);
        } catch (Exception e) {
        }
        try {
            dismissDialog(2);
        } catch (Exception e2) {
        }
        this.mWaitingForResult = false;
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("android.intent.action.MAIN".equals(intent.getAction())) {
            closeSystemDialogs();
            boolean z = (intent.getFlags() & 4194304) != 4194304;
            if (LauncherApplication.isScreenXLarge()) {
                this.mWorkspace.unshrink(z);
            }
            this.mWorkspace.exitWidgetResizeMode();
            if (z && this.mState == State.WORKSPACE && !this.mWorkspace.isTouchActive()) {
                this.mWorkspace.moveToDefaultScreen(true);
            }
            showWorkspace(z);
            View peekDecorView = getWindow().peekDecorView();
            if (peekDecorView != null && peekDecorView.getWindowToken() != null) {
                ((InputMethodManager) getSystemService("input_method")).hideSoftInputFromWindow(peekDecorView.getWindowToken(), 0);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        this.mSavedInstanceState = savedInstanceState;
    }

    /* access modifiers changed from: protected */
    public void onSaveInstanceState(Bundle bundle) {
        String currentTabTag;
        String currentTabTag2;
        bundle.putInt("launcher.current_screen", this.mWorkspace.getCurrentPage());
        ArrayList<Folder> openFolders = this.mWorkspace.getOpenFolders();
        if (openFolders.size() > 0) {
            int size = openFolders.size();
            long[] jArr = new long[size];
            for (int i = 0; i < size; i++) {
                jArr[i] = openFolders.get(i).getInfo().id;
            }
            bundle.putLongArray("launcher.user_folder", jArr);
        } else {
            super.onSaveInstanceState(bundle);
        }
        bundle.putInt("launcher.state", this.mState.ordinal());
        if (this.mAddScreen > -1 && this.mWaitingForResult) {
            bundle.putInt("launcher.add_screen", this.mAddScreen);
            bundle.putInt("launcher.add_cellX", this.mAddIntersectCellX);
            bundle.putInt("launcher.add_cellY", this.mAddIntersectCellY);
        }
        if (this.mFolderInfo != null && this.mWaitingForResult) {
            bundle.putBoolean("launcher.rename_folder", true);
            bundle.putLong("launcher.rename_folder_id", this.mFolderInfo.id);
        }
        if (!(this.mAllAppsGrid == null || !(this.mAllAppsGrid instanceof AllAppsTabbed) || (currentTabTag2 = ((AllAppsTabbed) this.mAllAppsGrid).getCurrentTabTag()) == null)) {
            bundle.putString("allapps_currentTab", currentTabTag2);
            bundle.putInt("allapps_currentPage", this.mAllAppsPagedView.getCurrentPage());
        }
        if (this.mHomeCustomizationDrawer != null && (currentTabTag = this.mHomeCustomizationDrawer.getCurrentTabTag()) != null) {
            bundle.putString("customize_currentTab", currentTabTag);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            this.mAppWidgetHost.stopListening();
        } catch (NullPointerException e) {
            Log.w("Launcher", "problem while stopping AppWidgetHost during Launcher destruction", e);
        }
        this.mAppWidgetHost = null;
        this.mWidgetsToAdvance.clear();
        TextKeyListener.getInstance().release();
        this.mModel.stopLoader();
        unbindDesktopItems();
        getContentResolver().unregisterContentObserver(this.mWidgetObserver);
        if (this.mPreviousView != null) {
            dismissPreview(this.mPreviousView);
        }
        if (this.mNextView != null) {
            dismissPreview(this.mNextView);
        }
        unregisterReceiver(this.mCloseSystemDialogsReceiver);
        ((ViewGroup) this.mWorkspace.getParent()).removeAllViews();
        this.mWorkspace.removeAllViews();
        this.mWorkspace = null;
        this.mDragController = null;
        // ValueAnimator.clearAllAnimations() was a hidden API in older platform builds.
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode >= 0) {
            this.mWaitingForResult = true;
        }
        super.startActivityForResult(intent, requestCode);
    }

    public void startSearch(String str, boolean z, Bundle bundle, boolean z2) {
        String str2;
        Bundle bundle2;
        showWorkspace(true);
        if (str == null) {
            String typedText = getTypedText();
            clearTypedText();
            str2 = typedText;
        } else {
            str2 = str;
        }
        if (bundle == null) {
            Bundle bundle3 = new Bundle();
            bundle3.putString("source", "launcher-search");
            bundle2 = bundle3;
        } else {
            bundle2 = bundle;
        }
        ((SearchManager) getSystemService("search")).startSearch(str2, z, getComponentName(), bundle2, z2);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (isWorkspaceLocked()) {
            return false;
        }
        super.onCreateOptionsMenu(menu);
        menu.add(1, 2, 0, R.string.menu_add).setIcon(17301555).setAlphabeticShortcut('A');
        menu.add(0, 3, 0, R.string.menu_manage_apps).setIcon(17301570).setAlphabeticShortcut('M');
        menu.add(2, 4, 0, R.string.menu_wallpaper).setIcon(17301567).setAlphabeticShortcut('W');
        menu.add(0, 5, 0, R.string.menu_search).setIcon(17301600).setAlphabeticShortcut('s');
        menu.add(0, 6, 0, R.string.menu_notifications).setIcon(17302262).setAlphabeticShortcut('N');
        Intent intent = new Intent("android.settings.SETTINGS");
        intent.setFlags(270532608);
        menu.add(0, 7, 0, R.string.menu_settings).setIcon(17301577).setAlphabeticShortcut('P').setIntent(intent);
        menu.add(0, 8, 0, R.string.menu_change_launcher).setIcon(17301577).setAlphabeticShortcut('T');
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean visible;
        super.onPrepareOptionsMenu(menu);
        if (this.mAllAppsGrid.isAnimating()) {
            return false;
        }
        if (!this.mAllAppsGrid.isVisible()) {
            visible = true;
        } else {
            visible = false;
        }
        menu.setGroupVisible(1, visible);
        menu.setGroupVisible(2, visible);
        if (visible) {
            menu.setGroupEnabled(1, ((CellLayout) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage())).existsEmptyCell());
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 2:
                addItems();
                return true;
            case 3:
                manageApps();
                return true;
            case 4:
                startWallpaper();
                return true;
            case 5:
                onSearchRequested();
                return true;
            case 6:
                showNotifications();
                return true;
            case 8:
                openLauncherSelector();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onSearchRequested() {
        startSearch((String) null, false, (Bundle) null, true);
        return true;
    }

    public boolean isWorkspaceLocked() {
        return this.mWorkspaceLoading || this.mWaitingForResult;
    }

    private boolean isPreviewVisible() {
        return ((this.mPreviousView == null || this.mPreviousView.getTag() == null) && (this.mNextView == null || this.mNextView.getTag() == null)) ? false : true;
    }

    private void addItems() {
        if (!LauncherApplication.isScreenXLarge()) {
            showWorkspace(true);
            showAddDialog(-1, -1);
        } else if (this.mState != State.CUSTOMIZE) {
            showCustomizationDrawer(true);
        }
    }

    private void resetAddInfo() {
        this.mAddScreen = -1;
        this.mAddIntersectCellX = -1;
        this.mAddIntersectCellY = -1;
        this.mAddDropPosition = null;
    }

    /* access modifiers changed from: package-private */
    public void addAppWidgetFromDrop(PendingAddWidgetInfo info, int screen, int[] position) {
        resetAddInfo();
        this.mAddScreen = screen;
        this.mAddDropPosition = position;
        int appWidgetId = getAppWidgetHost().allocateAppWidgetId();
        // AOSP Launcher2 used a hidden API here. In a standalone launcher we must use the
        // public binding flow (user approval may be required).
        if (!bindAppWidgetIdIfAllowedCompat(appWidgetId, info.componentName)) {
            // If not allowed, Launcher will fall back to the standard bind/configure flow
            // when we start the widget picker/configure UI.
        }
        addAppWidgetImpl(appWidgetId, info);
    }

    private boolean bindAppWidgetIdIfAllowedCompat(int appWidgetId, ComponentName provider) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return AppWidgetManager.getInstance(this).bindAppWidgetIdIfAllowed(appWidgetId, provider);
        }
        return false;
    }

    private void manageApps() {
        startActivity(new Intent("android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS"));
    }

    /* access modifiers changed from: package-private */
    public void addAppWidgetFromPick(Intent data) {
        addAppWidgetImpl(data.getIntExtra("appWidgetId", -1), (PendingAddWidgetInfo) null);
    }

    /* access modifiers changed from: package-private */
    public void addAppWidgetImpl(int i, PendingAddWidgetInfo pendingAddWidgetInfo) {
        AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(i);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra("appWidgetId", i);
            if (pendingAddWidgetInfo != null && pendingAddWidgetInfo.mimeType != null && !pendingAddWidgetInfo.mimeType.isEmpty()) {
                intent.putExtra("com.android.launcher.extra.widget.CONFIGURATION_DATA_MIME_TYPE", pendingAddWidgetInfo.mimeType);
                String str = pendingAddWidgetInfo.mimeType;
                ClipData clipData = (ClipData) pendingAddWidgetInfo.configurationData;
                ClipDescription description = clipData.getDescription();
                int i2 = 0;
                while (true) {
                    if (i2 >= description.getMimeTypeCount()) {
                        break;
                    } else if (description.getMimeType(i2).equals(str)) {
                        ClipData.Item itemAt = clipData.getItemAt(i2);
                        CharSequence text = itemAt.getText();
                        Uri uri = itemAt.getUri();
                        Intent intent2 = itemAt.getIntent();
                        if (uri != null) {
                            intent.putExtra("com.android.launcher.extra.widget.CONFIGURATION_DATA", uri);
                        } else if (intent2 != null) {
                            intent.putExtra("com.android.launcher.extra.widget.CONFIGURATION_DATA", intent2);
                        } else if (text != null) {
                            intent.putExtra("com.android.launcher.extra.widget.CONFIGURATION_DATA", text);
                        }
                    } else {
                        i2++;
                    }
                }
            }
            startActivityForResultSafely(intent, 5);
            return;
        }
        completeAddAppWidget(i, this.mAddScreen);
    }

    /* access modifiers changed from: package-private */
    public void processShortcutFromDrop(ComponentName componentName, int i, int[] iArr) {
        resetAddInfo();
        this.mAddScreen = i;
        this.mAddDropPosition = iArr;
        Intent intent = new Intent("android.intent.action.CREATE_SHORTCUT");
        intent.setComponent(componentName);
        processShortcut(intent);
    }

    /* access modifiers changed from: package-private */
    public void processShortcut(Intent intent) {
        String string = getResources().getString(R.string.group_applications);
        String stringExtra = intent.getStringExtra("android.intent.extra.shortcut.NAME");
        if (string == null || !string.equals(stringExtra)) {
            startActivityForResultSafely(intent, 1);
            return;
        }
        Intent intent2 = new Intent("android.intent.action.MAIN", (Uri) null);
        intent2.addCategory("android.intent.category.LAUNCHER");
        Intent intent3 = new Intent("android.intent.action.PICK_ACTIVITY");
        intent3.putExtra("android.intent.extra.INTENT", intent2);
        intent3.putExtra("android.intent.extra.TITLE", getText(R.string.title_select_application));
        startActivityForResultSafely(intent3, 6);
    }

    /* access modifiers changed from: package-private */
    public void processWallpaper(Intent intent) {
        startActivityForResult(intent, 10);
    }

    /* access modifiers changed from: package-private */
    public void addLiveFolderFromDrop(ComponentName componentName, int i, int[] iArr) {
        resetAddInfo();
        this.mAddScreen = i;
        this.mAddDropPosition = iArr;
        Intent intent = new Intent("android.intent.action.CREATE_LIVE_FOLDER");
        intent.setComponent(componentName);
        addLiveFolder(intent);
    }

    /* access modifiers changed from: package-private */
    public void addLiveFolder(Intent intent) {
        String string = getResources().getString(R.string.group_folder);
        String stringExtra = intent.getStringExtra("android.intent.extra.shortcut.NAME");
        if (string == null || !string.equals(stringExtra)) {
            startActivityForResultSafely(intent, 4);
        } else {
            addFolder(this.mAddScreen, this.mAddIntersectCellX, this.mAddIntersectCellY);
        }
    }

    /* access modifiers changed from: package-private */
    public void addFolder(int i, int i2, int i3) {
        UserFolderInfo userFolderInfo = new UserFolderInfo();
        userFolderInfo.title = getText(R.string.folder_name);
        int[] iArr = this.mTmpAddItemCellCoordinates;
        if (!((CellLayout) this.mWorkspace.getChildAt(i)).findCellForSpanThatIntersects(iArr, 1, 1, i2, i3)) {
            showOutOfSpaceMessage();
            return;
        }
        LauncherModel.addItemToDatabase(this, userFolderInfo, -100, i, iArr[0], iArr[1], false);
        sFolders.put(Long.valueOf(userFolderInfo.id), userFolderInfo);
        this.mWorkspace.addInScreen(FolderIcon.fromXml(R.layout.launcher3h_folder_icon, this, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), userFolderInfo, this.mIconCache), i, iArr[0], iArr[1], 1, 1, isWorkspaceLocked());
    }

    /* access modifiers changed from: package-private */
    public void removeFolder(FolderInfo folder) {
        sFolders.remove(Long.valueOf(folder.id));
    }

    private void completeAddLiveFolder(Intent intent, int i, int i2, int i3) {
        int[] iArr = this.mTmpAddItemCellCoordinates;
        if (!((CellLayout) this.mWorkspace.getChildAt(i)).findCellForSpanThatIntersects(iArr, 1, 1, i2, i3)) {
            showOutOfSpaceMessage();
            return;
        }
        LiveFolderInfo addLiveFolder = addLiveFolder(this, intent, i, iArr[0], iArr[1], false);
        if (!this.mRestoring) {
            this.mWorkspace.addInScreen(LiveFolderIcon.fromXml(R.layout.live_folder_icon, this, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), addLiveFolder), i, iArr[0], iArr[1], 1, 1, isWorkspaceLocked());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x003a  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x009c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static com.android.launcher2.LiveFolderInfo addLiveFolder(android.content.Context r11, android.content.Intent r12, int r13, int r14, int r15, boolean r16) {
        final Intent baseIntent = (Intent) r12.getParcelableExtra("android.intent.extra.livefolder.BASE_INTENT");
        final String name = r12.getStringExtra("android.intent.extra.livefolder.NAME");
        final Object iconObj = r12.getParcelableExtra("android.intent.extra.livefolder.ICON");

        Intent.ShortcutIconResource iconResource = null;
        Drawable icon = null;
        if (iconObj instanceof Intent.ShortcutIconResource) {
            iconResource = (Intent.ShortcutIconResource) iconObj;
            try {
                Resources res = r11.getPackageManager().getResourcesForApplication(iconResource.packageName);
                int id = res.getIdentifier(iconResource.resourceName, null, null);
                if (id != 0) {
                    icon = res.getDrawable(id);
                }
            } catch (Exception e) {
                Log.w("Launcher", "Could not load live folder icon: " + iconObj);
            }
        }
        if (icon == null) {
            icon = r11.getResources().getDrawable(R.drawable.ic_launcher_folder);
        }

        LiveFolderInfo info = new LiveFolderInfo();
        info.icon = Utilities.createIconBitmap(icon, r11);
        info.title = name;
        info.iconResource = iconResource;
        info.uri = r12.getData();
        info.baseIntent = baseIntent;
        info.displayMode = r12.getIntExtra("android.intent.extra.livefolder.DISPLAY_MODE", 1);

        LauncherModel.addItemToDatabase(r11, info, -100, r13, r14, r15, r16);
        sFolders.put(Long.valueOf(info.id), info);
        return info;
    }

    private void showNotifications() {
        Object statusBar = getSystemService("statusbar");
        if (statusBar == null) {
            return;
        }
        try {
            java.lang.reflect.Method expand = statusBar.getClass().getMethod("expandNotificationsPanel");
            expand.invoke(statusBar);
            return;
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Method expand = statusBar.getClass().getMethod("expand");
            expand.invoke(statusBar);
        } catch (Throwable ignored) {
        }
    }

    /* access modifiers changed from: private */
    private void openLauncherSelector() {
        android.content.Intent intent = new android.content.Intent()
                .setClassName(getPackageName(), "com.dismal.unifiedlauncher.ThemeSelectionActivity")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            android.widget.Toast.makeText(this, R.string.menu_change_launcher, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    public void startWallpaper() {
        showWorkspace(true);
        startActivityForResult(Intent.createChooser(new Intent("android.intent.action.SET_WALLPAPER"), getText(R.string.chooser_wallpaper)), 10);
    }

    private void registerContentObservers() {
        getContentResolver().registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true, this.mWidgetObserver);
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == 0) {
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case 25:
                    if (SystemPropertiesCompat.getInt("debug.launcher2.dumpstate", 0) != 0) {
                        dumpState();
                        return true;
                    }
                    break;
            }
        } else if (keyEvent.getAction() == 1) {
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
            }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    public void onBackPressed() {
        if (this.mState == State.ALL_APPS || this.mState == State.CUSTOMIZE) {
            showWorkspace(true);
        } else if (this.mWorkspace.getOpenFolder() != null) {
            closeFolder();
        } else if (isPreviewVisible()) {
            dismissPreview(this.mPreviousView);
            dismissPreview(this.mNextView);
        } else {
            this.mWorkspace.exitWidgetResizeMode();
            this.mWorkspace.showOutlinesTemporarily();
        }
    }

    private void closeFolder() {
        Folder folder = this.mWorkspace.getOpenFolder();
        if (folder != null) {
            closeFolder(folder);
        }
    }

    /* access modifiers changed from: package-private */
    public void closeFolder(Folder folder) {
        folder.getInfo().opened = false;
        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            ((CellLayout) parent).removeViewWithoutMarkingCells(folder);
            if (folder instanceof DropTarget) {
                this.mDragController.removeDropTarget((DropTarget) folder);
            }
        }
        folder.onClose();
    }

    /* access modifiers changed from: private */
    public void onAppWidgetReset() {
        if (this.mAppWidgetHost != null) {
            this.mAppWidgetHost.startListening();
        }
    }

    private void unbindDesktopItems() {
        Iterator<ItemInfo> it = this.mDesktopItems.iterator();
        while (it.hasNext()) {
            it.next().unbind();
        }
        this.mDesktopItems.clear();
    }

    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            Intent intent = ((ShortcutInfo) tag).intent;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
            if (startActivitySafely(intent, tag) && (v instanceof BubbleTextView)) {
                this.mWaitingForResume = (BubbleTextView) v;
                this.mWaitingForResume.setStayPressed(true);
            }
        } else if (tag instanceof FolderInfo) {
            handleFolderClick((FolderInfo) tag);
        } else if (v != this.mHandleView) {
        } else {
            if (this.mState == State.ALL_APPS) {
                showWorkspace(true);
            } else {
                showAllApps(true);
            }
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        showWorkspace(true);
        return false;
    }

    public void onClickSearchButton(View view) {
        startSearch((String) null, false, (Bundle) null, true);
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
    }

    public void onClickVoiceButton(View v) {
        startVoiceSearch();
    }

    private void startVoiceSearch() {
        Intent intent = new Intent("android.speech.action.WEB_SEARCH");
        intent.setFlags(268435456);
        startActivity(intent);
    }

    public void onClickConfigureButton(View v) {
        addItems();
    }

    public void onClickAllAppsButton(View v) {
        showAllApps(true);
    }

    public void onClickAppMarketButton(View view) {
        if (this.mAppMarketIntent != null) {
            startActivitySafely(this.mAppMarketIntent, "app market");
        }
    }

    /* access modifiers changed from: package-private */
    public void startApplicationDetailsActivity(ComponentName componentName) {
        startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", componentName.getPackageName(), (String) null)));
    }

    /* access modifiers changed from: package-private */
    public void startApplicationUninstallActivity(ApplicationInfo applicationInfo) {
        if ((applicationInfo.flags & 1) == 0) {
            Toast.makeText(this, R.string.uninstall_system_app_text, 0).show();
        } else {
            startActivity(new Intent("android.intent.action.DELETE", Uri.fromParts("package", applicationInfo.componentName.getPackageName(), applicationInfo.componentName.getClassName())));
        }
    }

    /* access modifiers changed from: package-private */
    public boolean startActivitySafely(Intent intent, Object obj) {
        intent.addFlags(268435456);
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Unable to launch. tag=" + obj + " intent=" + intent, e);
            return false;
        } catch (SecurityException e2) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + obj + " intent=" + intent, e2);
            return false;
        }
    }

    /* access modifiers changed from: package-private */
    public void startActivityForResultSafely(Intent intent, int i) {
        try {
            startActivityForResult(intent, i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
        } catch (SecurityException e2) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity.", e2);
        }
    }

    private void handleFolderClick(FolderInfo folderInfo) {
        if (!folderInfo.opened) {
            closeFolder();
            openFolder(folderInfo);
            return;
        }
        Folder openFolder = this.mWorkspace.getFolderForTag(folderInfo);
        if (openFolder != null) {
            int folderScreen = this.mWorkspace.getPageForView(openFolder);
            closeFolder(openFolder);
            if (folderScreen != this.mWorkspace.getCurrentPage()) {
                closeFolder();
                openFolder(folderInfo);
            }
        }
    }

    public void openFolder(FolderInfo folderInfo) {
        Folder openFolder;
        if (folderInfo instanceof UserFolderInfo) {
            openFolder = UserFolder.fromXml(this);
        } else if (folderInfo instanceof LiveFolderInfo) {
            openFolder = LiveFolder.fromXml(this, folderInfo);
        } else {
            return;
        }
        openFolder.setDragController(this.mDragController);
        openFolder.setLauncher(this);
        openFolder.bind(folderInfo);
        folderInfo.opened = true;
        this.mWorkspace.addInFullScreen(openFolder, folderInfo.screen);
        openFolder.onOpen();
    }

    public boolean onLongClick(View v) {
        if (this.mState != State.WORKSPACE) {
            return false;
        }
        int vid = v.getId();
        if (vid == R.id.all_apps_button) {
            if (this.mState != State.ALL_APPS) {
                this.mWorkspace.performHapticFeedback(0, 1);
                showPreviews(v);
            }
            return true;
        } else if (vid == R.id.previous_screen) {
            if (this.mState != State.ALL_APPS) {
                this.mWorkspace.performHapticFeedback(0, 1);
                showPreviews(v);
            }
            return true;
        } else if (vid == R.id.next_screen) {
            if (this.mState != State.ALL_APPS) {
                this.mWorkspace.performHapticFeedback(0, 1);
                showPreviews(v);
            }
            return true;
        } else {
                if (isWorkspaceLocked()) {
                    return false;
                }
                if (!(v instanceof CellLayout)) {
                    v = (View) v.getParent().getParent();
                }
                resetAddInfo();
                CellLayout.CellInfo longClickCellInfo = (CellLayout.CellInfo) v.getTag();
                if (longClickCellInfo == null || !longClickCellInfo.valid) {
                    return true;
                }
                View itemUnderLongClick = longClickCellInfo.cell;
                if (this.mWorkspace.allowLongPress() && !this.mDragController.isDragging()) {
                    if (itemUnderLongClick == null) {
                        this.mWorkspace.setAllowLongPress(false);
                        this.mWorkspace.performHapticFeedback(0, 1);
                        if (LauncherApplication.isScreenXLarge()) {
                            addItems();
                        } else {
                            showAddDialog(longClickCellInfo.cellX, longClickCellInfo.cellY);
                        }
                    } else if (!(itemUnderLongClick instanceof Folder)) {
                        this.mWorkspace.performHapticFeedback(0, 1);
                        this.mAddIntersectCellX = longClickCellInfo.cellX;
                        this.mAddIntersectCellY = longClickCellInfo.cellY;
                        this.mWorkspace.startDrag(longClickCellInfo);
                    }
                }
                return true;
        }
    }

    /* access modifiers changed from: private */
    public void dismissPreview(final View v) {
        final PopupWindow window = (PopupWindow) v.getTag();
        if (window != null) {
            window.setOnDismissListener(new PopupWindow.OnDismissListener() {
                public void onDismiss() {
                    ViewGroup group = (ViewGroup) v.getTag(R.id.workspace);
                    int count = group.getChildCount();
                    for (int i = 0; i < count; i++) {
                        ((ImageView) group.getChildAt(i)).setImageDrawable((Drawable) null);
                    }
                    @SuppressWarnings("unchecked")
                    ArrayList<Bitmap> bitmaps = (ArrayList<Bitmap>) v.getTag(R.id.icon);
                    if (bitmaps != null) {
                        Iterator<Bitmap> it = bitmaps.iterator();
                        while (it.hasNext()) {
                            Bitmap b = it.next();
                            if (b != null && !b.isRecycled()) {
                                b.recycle();
                            }
                        }
                    }
                    v.setTag(R.id.workspace, (Object) null);
                    v.setTag(R.id.icon, (Object) null);
                    window.setOnDismissListener((PopupWindow.OnDismissListener) null);
                }
            });
            window.dismiss();
        }
        v.setTag((Object) null);
    }

    private void showPreviews(View anchor) {
        showPreviews(anchor, 0, this.mWorkspace.getChildCount());
    }

    private void showPreviews(View view, int i, int i2) {
        Resources resources = getResources();
        Workspace workspace = this.mWorkspace;
        CellLayout cellLayout = (CellLayout) workspace.getChildAt(i);
        float childCount = (float) workspace.getChildCount();
        Rect rect = new Rect();
        resources.getDrawable(R.drawable.preview_background).getPadding(rect);
        int i3 = (int) (((float) (rect.left + rect.right)) * childCount);
        int i4 = rect.bottom + rect.top;
        float width = ((float) (cellLayout.getWidth() - i3)) / childCount;
        int width2 = cellLayout.getWidth();
        int height = cellLayout.getHeight();
        int leftPadding = cellLayout.getLeftPadding();
        int topPadding = cellLayout.getTopPadding();
        int rightPadding = width2 - (leftPadding + cellLayout.getRightPadding());
        int bottomPadding = height - (cellLayout.getBottomPadding() + topPadding);
        float f = width / ((float) rightPadding);
        int i5 = i2 - i;
        float f2 = ((float) rightPadding) * f;
        float f3 = ((float) bottomPadding) * f;
        LinearLayout linearLayout = new LinearLayout(this);
        PreviewTouchHandler previewTouchHandler = new PreviewTouchHandler(view);
        ArrayList arrayList = new ArrayList(i5);
        for (int i6 = i; i6 < i2; i6++) {
            ImageView imageView = new ImageView(this);
            CellLayout cellLayout2 = (CellLayout) workspace.getChildAt(i6);
            Bitmap createBitmap = Bitmap.createBitmap((int) f2, (int) f3, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(createBitmap);
            canvas.scale(f, f);
            canvas.translate((float) (-cellLayout2.getLeftPadding()), (float) (-cellLayout2.getTopPadding()));
            cellLayout2.drawChildren(canvas);
            imageView.setBackgroundDrawable(resources.getDrawable(R.drawable.preview_background));
            imageView.setImageBitmap(createBitmap);
            imageView.setTag(Integer.valueOf(i6));
            imageView.setOnClickListener(previewTouchHandler);
            imageView.setOnFocusChangeListener(previewTouchHandler);
            imageView.setFocusable(true);
            if (i6 == this.mWorkspace.getCurrentPage()) {
                imageView.requestFocus();
            }
            linearLayout.addView(imageView, -2, -2);
            arrayList.add(createBitmap);
        }
        PopupWindow popupWindow = new PopupWindow(this);
        popupWindow.setContentView(linearLayout);
        popupWindow.setWidth((int) ((((float) i5) * f2) + ((float) i3)));
        popupWindow.setHeight((int) (f3 + ((float) i4)));
        popupWindow.setAnimationStyle(R.style.AnimationPreview);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        popupWindow.showAsDropDown(view, 0, 0);
        final View view2 = view;
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            public void onDismiss() {
                Launcher.this.dismissPreview(view2);
            }
        });
        view.setTag(popupWindow);
        view.setTag(R.id.workspace, linearLayout);
        view.setTag(R.id.icon, arrayList);
    }

    class PreviewTouchHandler implements View.OnClickListener, View.OnFocusChangeListener, Runnable {
        private final View mAnchor;

        public PreviewTouchHandler(View anchor) {
            this.mAnchor = anchor;
        }

        public void onClick(View v) {
            Launcher.this.mWorkspace.snapToPage(((Integer) v.getTag()).intValue());
            v.post(this);
        }

        public void run() {
            Launcher.this.dismissPreview(this.mAnchor);
        }

        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                Launcher.this.mWorkspace.snapToPage(((Integer) v.getTag()).intValue());
            }
        }
    }

    /* access modifiers changed from: package-private */
    public Workspace getWorkspace() {
        return this.mWorkspace;
    }

    /* access modifiers changed from: package-private */
    public TabHost getCustomizationDrawer() {
        return this.mHomeCustomizationDrawer;
    }

    /* access modifiers changed from: protected */
    public Dialog onCreateDialog(int id) {
        switch (id) {
            case 1:
                return new CreateShortcut().createDialog();
            case 2:
                return new RenameFolder().createDialog();
            default:
                return super.onCreateDialog(id);
        }
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialog(int i, Dialog dialog) {
        switch (i) {
            case 2:
                if (this.mFolderInfo != null) {
                    EditText editText = (EditText) dialog.findViewById(R.id.folder_name);
                    CharSequence charSequence = this.mFolderInfo.title;
                    editText.setText(charSequence);
                    editText.setSelection(0, charSequence.length());
                    return;
                }
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public void showRenameDialog(FolderInfo info) {
        this.mFolderInfo = info;
        this.mWaitingForResult = true;
        showDialog(2);
    }

    private void showAddDialog(int intersectX, int intersectY) {
        resetAddInfo();
        this.mAddIntersectCellX = intersectX;
        this.mAddIntersectCellY = intersectY;
        this.mAddScreen = this.mWorkspace.getCurrentPage();
        this.mWaitingForResult = true;
        showDialog(1);
    }

    /* access modifiers changed from: private */
    public void pickShortcut() {
        Bundle bundle = new Bundle();
        ArrayList arrayList = new ArrayList();
        arrayList.add(getString(R.string.group_applications));
        bundle.putStringArrayList("android.intent.extra.shortcut.NAME", arrayList);
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_application));
        bundle.putParcelableArrayList("android.intent.extra.shortcut.ICON_RESOURCE", arrayList2);
        Intent intent = new Intent("android.intent.action.PICK_ACTIVITY");
        intent.putExtra("android.intent.extra.INTENT", new Intent("android.intent.action.CREATE_SHORTCUT"));
        intent.putExtra("android.intent.extra.TITLE", getText(R.string.title_select_shortcut));
        intent.putExtras(bundle);
        startActivityForResult(intent, 7);
    }

    private class RenameFolder {
        /* access modifiers changed from: private */
        public EditText mInput;

        private RenameFolder() {
        }

        /* access modifiers changed from: package-private */
        public Dialog createDialog() {
            View layout = View.inflate(Launcher.this, R.layout.launcher3h_rename_folder, (ViewGroup) null);
            this.mInput = (EditText) layout.findViewById(R.id.folder_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
            builder.setIcon(0);
            builder.setTitle(Launcher.this.getString(R.string.rename_folder_title));
            builder.setCancelable(true);
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    RenameFolder.this.cleanup();
                }
            });
            builder.setNegativeButton(Launcher.this.getString(R.string.cancel_action), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    RenameFolder.this.cleanup();
                }
            });
            builder.setPositiveButton(Launcher.this.getString(R.string.rename_action), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    RenameFolder.this.changeFolderName();
                }
            });
            builder.setView(layout);
            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                public void onShow(DialogInterface dialog) {
                    boolean unused = Launcher.this.mWaitingForResult = true;
                    RenameFolder.this.mInput.requestFocus();
                    ((InputMethodManager) Launcher.this.getSystemService("input_method")).showSoftInput(RenameFolder.this.mInput, 0);
                }
            });
            return dialog;
        }

        /* access modifiers changed from: private */
        public void changeFolderName() {
            String name = this.mInput.getText().toString();
            if (!TextUtils.isEmpty(name)) {
                FolderInfo unused = Launcher.this.mFolderInfo = (FolderInfo) Launcher.sFolders.get(Long.valueOf(Launcher.this.mFolderInfo.id));
                Launcher.this.mFolderInfo.title = name;
                LauncherModel.updateItemInDatabase(Launcher.this, Launcher.this.mFolderInfo);
                if (Launcher.this.mWorkspaceLoading) {
                    Launcher.this.lockAllApps();
                    Launcher.this.mModel.setAllowLoadFromCache();
                    Launcher.this.mModel.startLoader(Launcher.this, false);
                } else {
                    FolderIcon folderIcon = (FolderIcon) Launcher.this.mWorkspace.getViewForTag(Launcher.this.mFolderInfo);
                    if (folderIcon != null) {
                        folderIcon.setText(name);
                        Launcher.this.getWorkspace().requestLayout();
                    } else {
                        Launcher.this.lockAllApps();
                        boolean unused2 = Launcher.this.mWorkspaceLoading = true;
                        Launcher.this.mModel.setAllowLoadFromCache();
                        Launcher.this.mModel.startLoader(Launcher.this, false);
                    }
                }
            }
            cleanup();
        }

        /* access modifiers changed from: private */
        public void cleanup() {
            Launcher.this.dismissDialog(2);
            boolean unused = Launcher.this.mWaitingForResult = false;
            FolderInfo unused2 = Launcher.this.mFolderInfo = null;
        }
    }

    public boolean isAllAppsVisible() {
        return this.mState == State.ALL_APPS;
    }

    public void zoomed(float zoom) {
        if (zoom == 1.0f && !LauncherApplication.isScreenXLarge()) {
            this.mWorkspace.setVisibility(8);
        }
    }

    /* access modifiers changed from: private */
    public void showAndEnableToolbarButton(View button) {
        button.setVisibility(0);
        button.setFocusable(true);
        button.setClickable(true);
    }

    /* access modifiers changed from: private */
    public void hideToolbarButton(View button) {
        button.setAlpha(0.0f);
        button.setVisibility(4);
    }

    /* access modifiers changed from: private */
    public void disableToolbarButton(View button) {
        button.setFocusable(false);
        button.setClickable(false);
    }

    private void hideOrShowToolbarButton(final boolean z, final View view, AnimatorSet animatorSet) {
        final boolean z2 = !z;
        int integer = z ? getResources().getInteger(R.integer.config_toolbarButtonFadeInTime) : getResources().getInteger(R.integer.config_toolbarButtonFadeOutTime);
        if (animatorSet != null) {
            float[] fArr = new float[2];
            fArr[0] = view.getAlpha();
            fArr[1] = z ? 1.0f : 0.0f;
            ValueAnimator ofFloat = ValueAnimator.ofFloat(fArr);
            ofFloat.setDuration((long) integer);
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    view.setAlpha(((Float) animation.getAnimatedValue()).floatValue());
                }
            });
            ofFloat.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    if (z) {
                        Launcher.this.showAndEnableToolbarButton(view);
                    }
                    if (z2) {
                        Launcher.this.disableToolbarButton(view);
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    if (z2) {
                        Launcher.this.hideToolbarButton(view);
                    }
                }
            });
            animatorSet.play(ofFloat);
        } else if (z) {
            showAndEnableToolbarButton(view);
            view.setAlpha(1.0f);
        } else {
            disableToolbarButton(view);
            hideToolbarButton(view);
        }
    }

    /* renamed from: com.android.launcher2.Launcher$22  reason: invalid class name */
    static /* synthetic */ class AnonymousClass22 {
        static final /* synthetic */ int[] $SwitchMap$com$android$launcher2$Launcher$State = new int[State.values().length];

        static {
            try {
                $SwitchMap$com$android$launcher2$Launcher$State[State.WORKSPACE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$launcher2$Launcher$State[State.ALL_APPS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$launcher2$Launcher$State[State.CUSTOMIZE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void hideAndShowToolbarButtons(State state, AnimatorSet animatorSet, AnimatorSet animatorSet2) {
        switch (AnonymousClass22.$SwitchMap$com$android$launcher2$Launcher$State[state.ordinal()]) {
            case 1:
                hideOrShowToolbarButton(true, this.mButtonCluster, animatorSet);
                this.mDeleteZone.setOverlappingViews(new View[]{this.mAllAppsButton, this.mDivider, this.mConfigureButton});
                this.mDeleteZone.setDragAndDropEnabled(true);
                this.mDeleteZone.setText(getResources().getString(R.string.delete_zone_label_workspace));
                return;
            case 2:
                hideOrShowToolbarButton(false, this.mButtonCluster, animatorSet2);
                this.mDeleteZone.setDragAndDropEnabled(false);
                this.mDeleteZone.setText(getResources().getString(R.string.delete_zone_label_all_apps));
                return;
            case 3:
                hideOrShowToolbarButton(false, this.mButtonCluster, animatorSet2);
                this.mDeleteZone.setDragAndDropEnabled(false);
                return;
            default:
                return;
        }
    }

    private void setPivotsForZoom(View view, State state, float f) {
        int height = view.getHeight();
        view.setPivotX(((float) view.getWidth()) / 2.0f);
        if (state == State.ALL_APPS) {
            view.setPivotY(((float) height) * 0.825f);
        } else {
            view.setPivotY(((float) height) * -0.2f);
        }
    }

    private void cameraZoomOut(State state, boolean z) {
        Resources resources = getResources();
        boolean z2 = state == State.ALL_APPS;
        int integer = z2 ? resources.getInteger(R.integer.config_allAppsZoomInTime) : resources.getInteger(R.integer.config_customizeZoomInTime);
        int integer2 = z2 ? resources.getInteger(R.integer.config_allAppsFadeInTime) : resources.getInteger(R.integer.config_customizeFadeInTime);
        float integer3 = z2 ? (float) resources.getInteger(R.integer.config_allAppsZoomScaleFactor) : (float) resources.getInteger(R.integer.config_customizeZoomScaleFactor);
        View view = z2 ? (View) this.mAllAppsGrid : this.mHomeCustomizationDrawer;
        setPivotsForZoom(view, state, integer3);
        if (z2) {
            this.mWorkspace.shrink(Workspace.ShrinkState.BOTTOM_HIDDEN, z);
        } else {
            this.mWorkspace.shrink(Workspace.ShrinkState.TOP, z);
        }
        if (z) {
            ValueAnimator duration = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f}).setDuration((long) integer);
            duration.setInterpolator(new Workspace.ZoomOutInterpolator());
            final View view2 = view;
            final float f = integer3;
            duration.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float b = ((Float) animation.getAnimatedValue()).floatValue();
                    float a = 1.0f - b;
                    View parent = (View) view2.getParent();
                    if (parent != null) {
                        parent.invalidate();
                    }
                    view2.setScaleX((f * a) + (b * 1.0f));
                    view2.setScaleY((f * a) + (b * 1.0f));
                }
            });
            if (z2) {
                view.setAlpha(0.0f);
                ValueAnimator duration2 = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f}).setDuration((long) integer2);
                duration2.setInterpolator(new DecelerateInterpolator(1.5f));
                final View view3 = view;
                duration2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float b = ((Float) animation.getAnimatedValue()).floatValue();
                        view3.setAlpha((0.0f * (1.0f - b)) + (1.0f * b));
                    }
                });
                duration2.start();
            }
            if (view instanceof LauncherTransitionable) {
                ((LauncherTransitionable) view).onLauncherTransitionStart(duration);
            }
            final View view4 = view;
            final boolean z3 = z2;
            final ValueAnimator valueAnimator = duration;
            duration.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    view4.setTranslationX(0.0f);
                    view4.setTranslationY(0.0f);
                    view4.setVisibility(0);
                    if (!z3) {
                        view4.setAlpha(1.0f);
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    view4.setScaleX(1.0f);
                    view4.setScaleY(1.0f);
                    if (view4 instanceof LauncherTransitionable) {
                        ((LauncherTransitionable) view4).onLauncherTransitionEnd(valueAnimator);
                    }
                }
            });
            AnimatorSet animatorSet = new AnimatorSet();
            AnimatorSet animatorSet2 = new AnimatorSet();
            hideAndShowToolbarButtons(state, animatorSet2, animatorSet);
            if (this.mStateAnimation != null) {
                this.mStateAnimation.cancel();
            }
            this.mStateAnimation = new AnimatorSet();
            this.mStateAnimation.playTogether(new Animator[]{duration, animatorSet});
            this.mStateAnimation.play(duration).after(0);
            this.mStateAnimation.play(animatorSet2).after((long) ((integer + 0) - resources.getInteger(R.integer.config_toolbarButtonFadeInTime)));
            this.mStateAnimation.start();
            return;
        }
        view.setTranslationX(0.0f);
        view.setTranslationY(0.0f);
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setVisibility(0);
        if (view instanceof LauncherTransitionable) {
            ((LauncherTransitionable) view).onLauncherTransitionStart((Animator) null);
            ((LauncherTransitionable) view).onLauncherTransitionEnd((Animator) null);
        }
        hideAndShowToolbarButtons(state, (AnimatorSet) null, (AnimatorSet) null);
    }

    private void cameraZoomIn(State fromState, boolean animated) {
        cameraZoomIn(fromState, animated, false);
    }

    private void cameraZoomIn(State state, boolean z, boolean z2) {
        Resources resources = getResources();
        boolean z3 = state == State.ALL_APPS;
        int integer = z3 ? resources.getInteger(R.integer.config_allAppsZoomOutTime) : resources.getInteger(R.integer.config_customizeZoomOutTime);
        final float integer2 = z3 ? (float) resources.getInteger(R.integer.config_allAppsZoomScaleFactor) : (float) resources.getInteger(R.integer.config_customizeZoomScaleFactor);
        final View view = z3 ? (View) this.mAllAppsGrid : this.mHomeCustomizationDrawer;
        this.mCustomizePagedView.endChoiceMode();
        this.mAllAppsPagedView.endChoiceMode();
        setPivotsForZoom(view, state, integer2);
        if (!z2) {
            this.mWorkspace.unshrink(z);
        }
        if (z) {
            if (this.mStateAnimation != null) {
                this.mStateAnimation.cancel();
            }
            this.mStateAnimation = new AnimatorSet();
            final float scaleX = view.getScaleX();
            final float scaleY = view.getScaleY();
            ValueAnimator duration = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f}).setDuration((long) integer);
            duration.setInterpolator(new Workspace.ZoomInInterpolator());
            duration.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float b = ((Float) animation.getAnimatedValue()).floatValue();
                    float a = 1.0f - b;
                    View parent = (View) view.getParent();
                    if (parent != null) {
                        parent.invalidate();
                    }
                    view.setScaleX((scaleX * a) + (integer2 * b));
                    view.setScaleY((scaleY * a) + (integer2 * b));
                }
            });
            final ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            ofFloat.setDuration((long) resources.getInteger(R.integer.config_allAppsFadeOutTime));
            ofFloat.setInterpolator(new DecelerateInterpolator(1.5f));
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float b = ((Float) animation.getAnimatedValue()).floatValue();
                    view.setAlpha((1.0f * (1.0f - b)) + (0.0f * b));
                }
            });
            if (view instanceof LauncherTransitionable) {
                ((LauncherTransitionable) view).onLauncherTransitionStart(ofFloat);
            }
            ofFloat.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(8);
                    if (view instanceof LauncherTransitionable) {
                        ((LauncherTransitionable) view).onLauncherTransitionEnd(ofFloat);
                    }
                }
            });
            AnimatorSet animatorSet = new AnimatorSet();
            AnimatorSet animatorSet2 = new AnimatorSet();
            if (!z2) {
                hideAndShowToolbarButtons(State.WORKSPACE, animatorSet2, animatorSet);
            }
            this.mStateAnimation.playTogether(new Animator[]{duration, animatorSet, ofFloat});
            this.mStateAnimation.play(animatorSet2).after((long) (resources.getInteger(R.integer.config_workspaceUnshrinkTime) - resources.getInteger(R.integer.config_toolbarButtonFadeInTime)));
            this.mStateAnimation.start();
            return;
        }
        view.setVisibility(8);
        if (view instanceof LauncherTransitionable) {
            ((LauncherTransitionable) view).onLauncherTransitionStart((Animator) null);
            ((LauncherTransitionable) view).onLauncherTransitionEnd((Animator) null);
        }
        if (!z2) {
            hideAndShowToolbarButtons(State.WORKSPACE, (AnimatorSet) null, (AnimatorSet) null);
        }
    }

    /* access modifiers changed from: package-private */
    public void showAllApps(boolean animated) {
        if (this.mState == State.WORKSPACE) {
            if (LauncherApplication.isScreenXLarge()) {
                cameraZoomOut(State.ALL_APPS, animated);
            } else {
                this.mAllAppsGrid.zoom(1.0f, animated);
            }
            ((View) this.mAllAppsGrid).setFocusable(true);
            ((View) this.mAllAppsGrid).requestFocus();
            this.mDeleteZone.setVisibility(8);
            this.mState = State.ALL_APPS;
            this.mUserPresent = false;
            updateRunning();
            getWindow().getDecorView().sendAccessibilityEvent(4);
        }
    }

    /* access modifiers changed from: package-private */
    public void showWorkspace(boolean animated) {
        showWorkspace(animated, (CellLayout) null);
    }

    /* access modifiers changed from: package-private */
    public void showWorkspace(boolean animated, CellLayout layout) {
        if (layout != null) {
            this.mWorkspace.unshrink(layout);
        } else {
            this.mWorkspace.unshrink(animated);
        }
        if (this.mState == State.ALL_APPS) {
            closeAllApps(animated);
        } else if (this.mState == State.CUSTOMIZE) {
            hideCustomizationDrawer(animated);
        }
        this.mState = State.WORKSPACE;
        this.mUserPresent = true;
        updateRunning();
        getWindow().getDecorView().sendAccessibilityEvent(4);
    }

    /* access modifiers changed from: package-private */
    public void enterSpringLoadedDragMode(CellLayout layout) {
        this.mWorkspace.enterSpringLoadedDragMode(layout);
        if (this.mState == State.ALL_APPS) {
            cameraZoomIn(State.ALL_APPS, true, true);
            this.mState = State.ALL_APPS_SPRING_LOADED;
        } else if (this.mState == State.CUSTOMIZE) {
            cameraZoomIn(State.CUSTOMIZE, true, true);
            this.mState = State.CUSTOMIZE_SPRING_LOADED;
        }
    }

    /* access modifiers changed from: package-private */
    public void exitSpringLoadedDragMode() {
        if (this.mState == State.ALL_APPS_SPRING_LOADED) {
            this.mWorkspace.exitSpringLoadedDragMode(Workspace.ShrinkState.BOTTOM_VISIBLE);
            cameraZoomOut(State.ALL_APPS, true);
            this.mState = State.ALL_APPS;
        } else if (this.mState == State.CUSTOMIZE_SPRING_LOADED) {
            this.mWorkspace.exitSpringLoadedDragMode(Workspace.ShrinkState.TOP);
            cameraZoomOut(State.CUSTOMIZE, true);
            this.mState = State.CUSTOMIZE;
        }
    }

    /* access modifiers changed from: package-private */
    public void closeAllApps(boolean animated) {
        if (this.mState == State.ALL_APPS || this.mState == State.ALL_APPS_SPRING_LOADED) {
            this.mWorkspace.setVisibility(0);
            if (LauncherApplication.isScreenXLarge()) {
                cameraZoomIn(State.ALL_APPS, animated);
            } else {
                this.mAllAppsGrid.zoom(0.0f, animated);
            }
            ((View) this.mAllAppsGrid).setFocusable(false);
            this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()).requestFocus();
        }
    }

    /* access modifiers changed from: package-private */
    public void lockAllApps() {
    }

    private void showCustomizationDrawer(boolean animated) {
        if (this.mState == State.WORKSPACE) {
            cameraZoomOut(State.CUSTOMIZE, animated);
            this.mState = State.CUSTOMIZE;
            this.mUserPresent = false;
            updateRunning();
        }
    }

    /* access modifiers changed from: package-private */
    public void hideCustomizationDrawer(boolean animated) {
        if (this.mState == State.CUSTOMIZE || this.mState == State.CUSTOMIZE_SPRING_LOADED) {
            cameraZoomIn(State.CUSTOMIZE, animated);
        }
    }

    /* access modifiers changed from: package-private */
    public void addExternalItemToScreen(ItemInfo itemInfo, CellLayout layout) {
        if (!this.mWorkspace.addExternalItemToScreen(itemInfo, layout)) {
            showOutOfSpaceMessage();
        } else {
            layout.animateDrop();
        }
    }

    /* access modifiers changed from: package-private */
    public void onWorkspaceClick(CellLayout layout) {
        showWorkspace(true, layout);
    }

    private Drawable getExternalPackageToolbarIcon(ComponentName componentName) {
        int i;
        try {
            PackageManager packageManager = getPackageManager();
            Bundle bundle = packageManager.getActivityInfo(componentName, 128).metaData;
            if (!(bundle == null || (i = bundle.getInt("com.android.launcher.toolbar_icon")) == 0)) {
                return packageManager.getResourcesForActivity(componentName).getDrawable(i);
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        return null;
    }

    private Drawable.ConstantState updateTextButtonWithIconFromExternalActivity(int buttonId, ComponentName activityName, int fallbackDrawableId) {
        TextView button = (TextView) findViewById(buttonId);
        if (button == null) {
            return null;
        }
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName);
        if (toolbarIcon == null) {
            button.setCompoundDrawablesWithIntrinsicBounds(fallbackDrawableId, 0, 0, 0);
            return null;
        }
        button.setCompoundDrawablesWithIntrinsicBounds(toolbarIcon, (Drawable) null, (Drawable) null, (Drawable) null);
        return toolbarIcon.getConstantState();
    }

    private Drawable.ConstantState updateButtonWithIconFromExternalActivity(int buttonId, ComponentName activityName, int fallbackDrawableId) {
        ImageView button = (ImageView) findViewById(buttonId);
        if (button == null) {
            return null;
        }
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName);
        if (toolbarIcon == null) {
            button.setImageResource(fallbackDrawableId);
            return null;
        }
        button.setImageDrawable(toolbarIcon);
        return toolbarIcon.getConstantState();
    }

    private void updateTextButtonWithDrawable(int buttonId, Drawable.ConstantState d) {
        TextView button = (TextView) findViewById(buttonId);
        if (button != null) {
            button.setCompoundDrawables(d.newDrawable(getResources()), (Drawable) null, (Drawable) null, (Drawable) null);
        }
    }

    private void updateButtonWithDrawable(int buttonId, Drawable.ConstantState d) {
        ImageView button = (ImageView) findViewById(buttonId);
        if (button != null) {
            button.setImageDrawable(d.newDrawable(getResources()));
        }
    }

    private void updateGlobalSearchIcon() {
        if (LauncherApplication.isScreenXLarge()) {
            ComponentName globalSearchActivity = ((SearchManager) getSystemService("search")).getGlobalSearchActivity();
            if (globalSearchActivity != null) {
                sGlobalSearchIcon = updateButtonWithIconFromExternalActivity(R.id.search_button, globalSearchActivity, R.drawable.ic_generic_search);
            } else {
                View searchBtn = findViewById(R.id.search_button);
                if (searchBtn != null) searchBtn.setVisibility(8);
            }
        }
    }

    private void updateGlobalSearchIcon(Drawable.ConstantState constantState) {
        updateButtonWithDrawable(R.id.search_button, constantState);
    }

    private void updateVoiceSearchIcon() {
        if (LauncherApplication.isScreenXLarge()) {
            ComponentName resolveActivity = new Intent("android.speech.action.WEB_SEARCH").resolveActivity(getPackageManager());
            if (resolveActivity != null) {
                sVoiceSearchIcon = updateButtonWithIconFromExternalActivity(R.id.voice_button, resolveActivity, R.drawable.ic_voice_search);
            } else {
                View voiceBtn = findViewById(R.id.voice_button);
                if (voiceBtn != null) voiceBtn.setVisibility(8);
            }
        }
    }

    private void updateVoiceSearchIcon(Drawable.ConstantState constantState) {
        updateButtonWithDrawable(R.id.voice_button, constantState);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:2:0x0006, code lost:
        r0 = new android.content.Intent("android.intent.action.MAIN").addCategory("android.intent.category.APP_MARKET");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateAppMarketIcon() {
        if (!LauncherApplication.isScreenXLarge()) {
            return;
        }
        final View marketButton = findViewById(R.id.market_button);
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET);
        ComponentName activityName = intent.resolveActivity(getPackageManager());
        if (activityName != null) {
            this.mAppMarketIntent = intent;
            sAppMarketIcon = updateTextButtonWithIconFromExternalActivity(R.id.market_button, activityName, R.drawable.app_market_generic);
            if (marketButton != null) {
                marketButton.setVisibility(View.VISIBLE);
                marketButton.setEnabled(true);
            }
            return;
        }
        if (marketButton != null) {
            marketButton.setVisibility(View.GONE);
            marketButton.setEnabled(false);
        }
    }

    private void updateAppMarketIcon(Drawable.ConstantState constantState) {
        updateTextButtonWithDrawable(R.id.market_button, constantState);
    }

    private class CreateShortcut implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener, DialogInterface.OnDismissListener, DialogInterface.OnShowListener {
        private AddAdapter mAdapter;

        private CreateShortcut() {
        }

        /* access modifiers changed from: package-private */
        public Dialog createDialog() {
            this.mAdapter = new AddAdapter(Launcher.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
            builder.setTitle(Launcher.this.getString(R.string.menu_item_add_item));
            builder.setAdapter(this.mAdapter, this);
            builder.setInverseBackgroundForced(true);
            AlertDialog dialog = builder.create();
            dialog.setOnCancelListener(this);
            dialog.setOnDismissListener(this);
            dialog.setOnShowListener(this);
            return dialog;
        }

        public void onCancel(DialogInterface dialog) {
            boolean unused = Launcher.this.mWaitingForResult = false;
            cleanup();
        }

        public void onDismiss(DialogInterface dialog) {
        }

        private void cleanup() {
            try {
                Launcher.this.dismissDialog(1);
            } catch (Exception e) {
            }
        }

        public void onClick(DialogInterface dialog, int which) {
            Resources res = Launcher.this.getResources();
            cleanup();
            switch (which) {
                case 0:
                    Launcher.this.pickShortcut();
                    return;
                case 1:
                    int appWidgetId = Launcher.this.mAppWidgetHost.allocateAppWidgetId();
                    Intent pickIntent = new Intent("android.appwidget.action.APPWIDGET_PICK");
                    pickIntent.putExtra("appWidgetId", appWidgetId);
                    Launcher.this.startActivityForResult(pickIntent, 9);
                    return;
                case 2:
                    Bundle bundle = new Bundle();
                    ArrayList<String> shortcutNames = new ArrayList<>();
                    shortcutNames.add(res.getString(R.string.group_folder));
                    bundle.putStringArrayList("android.intent.extra.shortcut.NAME", shortcutNames);
                    ArrayList<Intent.ShortcutIconResource> shortcutIcons = new ArrayList<>();
                    shortcutIcons.add(Intent.ShortcutIconResource.fromContext(Launcher.this, R.drawable.ic_launcher_folder));
                    bundle.putParcelableArrayList("android.intent.extra.shortcut.ICON_RESOURCE", shortcutIcons);
                    Intent pickIntent2 = new Intent("android.intent.action.PICK_ACTIVITY");
                    pickIntent2.putExtra("android.intent.extra.INTENT", new Intent("android.intent.action.CREATE_LIVE_FOLDER"));
                    pickIntent2.putExtra("android.intent.extra.TITLE", Launcher.this.getText(R.string.title_select_live_folder));
                    pickIntent2.putExtras(bundle);
                    Launcher.this.startActivityForResult(pickIntent2, 8);
                    return;
                case 3:
                    Launcher.this.startWallpaper();
                    return;
                default:
                    return;
            }
        }

        public void onShow(DialogInterface dialog) {
            boolean unused = Launcher.this.mWaitingForResult = true;
        }
    }

    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        private CloseSystemDialogsIntentReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Launcher.this.closeSystemDialogs();
            String reason = intent.getStringExtra("reason");
            if (!"homekey".equals(reason)) {
                boolean animate = true;
                if (Launcher.this.mPaused || "lock".equals(reason)) {
                    animate = false;
                }
                Launcher.this.showWorkspace(animate);
            }
        }
    }

    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            Launcher.this.onAppWidgetReset();
        }
    }

    public boolean setLoadOnResume() {
        if (!this.mPaused) {
            return false;
        }
        Log.i("Launcher", "setLoadOnResume");
        this.mOnResumeNeedsLoad = true;
        return true;
    }

    public int getCurrentWorkspaceScreen() {
        if (this.mWorkspace != null) {
            return this.mWorkspace.getCurrentPage();
        }
        return 2;
    }

    /* access modifiers changed from: package-private */
    public void setAllAppsPagedView(AllAppsPagedView view) {
        this.mAllAppsPagedView = view;
    }

    public void startBinding() {
        Workspace workspace = this.mWorkspace;
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            ((CellLayout) workspace.getChildAt(i)).removeAllViewsInLayout();
        }
        unbindDesktopItems();
    }

    public void bindItems(ArrayList<ItemInfo> arrayList, int i, int i2) {
        setLoadOnResume();
        Workspace workspace = this.mWorkspace;
        for (int i3 = i; i3 < i2; i3++) {
            ItemInfo itemInfo = arrayList.get(i3);
            this.mDesktopItems.add(itemInfo);
            switch (itemInfo.itemType) {
                case 0:
                case 1:
                    workspace.addInScreen(createShortcut((ShortcutInfo) itemInfo), itemInfo.screen, itemInfo.cellX, itemInfo.cellY, 1, 1, false);
                    break;
                case 2:
                    workspace.addInScreen(FolderIcon.fromXml(R.layout.launcher3h_folder_icon, this, (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()), (UserFolderInfo) itemInfo, this.mIconCache), itemInfo.screen, itemInfo.cellX, itemInfo.cellY, 1, 1, false);
                    break;
                case 3:
                    workspace.addInScreen(LiveFolderIcon.fromXml(R.layout.live_folder_icon, this, (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()), (LiveFolderInfo) itemInfo), itemInfo.screen, itemInfo.cellX, itemInfo.cellY, 1, 1, false);
                    break;
            }
        }
        workspace.requestLayout();
    }

    public void bindFolders(HashMap<Long, FolderInfo> folders) {
        setLoadOnResume();
        sFolders.clear();
        sFolders.putAll(folders);
    }

    public void bindAppWidget(LauncherAppWidgetInfo item) {
        setLoadOnResume();
        Workspace workspace = this.mWorkspace;
        int appWidgetId = item.appWidgetId;
        AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        item.hostView = this.mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        item.hostView.setAppWidget(appWidgetId, appWidgetInfo);
        item.hostView.setTag(item);
        workspace.addInScreen(item.hostView, item.screen, item.cellX, item.cellY, item.spanX, item.spanY, false);
        addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);
        workspace.requestLayout();
        this.mDesktopItems.add(item);
    }

    public void finishBindingItems() {
        setLoadOnResume();
        if (this.mSavedState != null) {
            if (!this.mWorkspace.hasFocus()) {
                this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()).requestFocus();
            }
            long[] longArray = this.mSavedState.getLongArray("launcher.user_folder");
            if (longArray != null) {
                for (long valueOf : longArray) {
                    FolderInfo folderInfo = sFolders.get(Long.valueOf(valueOf));
                    if (folderInfo != null) {
                        openFolder(folderInfo);
                    }
                }
                Folder openFolder = this.mWorkspace.getOpenFolder();
                if (openFolder != null) {
                    openFolder.requestFocus();
                }
            }
            this.mSavedState = null;
        }
        if (this.mSavedInstanceState != null) {
            super.onRestoreInstanceState(this.mSavedInstanceState);
            this.mSavedInstanceState = null;
        }
        if (LauncherApplication.isScreenXLarge() && this.mState == State.CUSTOMIZE) {
            int childCount = this.mWorkspace.getChildCount();
            for (int i = 0; i < childCount; i++) {
                this.mWorkspace.getChildAt(i).requestLayout();
            }
        }
        this.mWorkspaceLoading = false;
        for (int i2 = 0; i2 < sPendingAddList.size(); i2++) {
            completeAdd(sPendingAddList.get(i2));
        }
        sPendingAddList.clear();
    }

    private void updateIconsAffectedByPackageManagerChanges() {
        updateAppMarketIcon();
        updateGlobalSearchIcon();
        updateVoiceSearchIcon();
    }

    public void bindAllApplications(ArrayList<ApplicationInfo> apps) {
        this.mAllAppsGrid.setApps(apps);
        if (this.mCustomizePagedView != null) {
            this.mCustomizePagedView.setApps(apps);
        }
        updateIconsAffectedByPackageManagerChanges();
    }

    public void bindAppsAdded(ArrayList<ApplicationInfo> apps) {
        setLoadOnResume();
        removeDialog(1);
        this.mAllAppsGrid.addApps(apps);
        if (this.mCustomizePagedView != null) {
            this.mCustomizePagedView.addApps(apps);
        }
        updateIconsAffectedByPackageManagerChanges();
    }

    public void bindAppsUpdated(ArrayList<ApplicationInfo> apps) {
        setLoadOnResume();
        removeDialog(1);
        if (this.mWorkspace != null) {
            this.mWorkspace.updateShortcuts(apps);
        }
        if (this.mAllAppsGrid != null) {
            this.mAllAppsGrid.updateApps(apps);
        }
        if (this.mCustomizePagedView != null) {
            this.mCustomizePagedView.updateApps(apps);
        }
        updateIconsAffectedByPackageManagerChanges();
    }

    public void bindAppsRemoved(ArrayList<ApplicationInfo> apps, boolean permanent) {
        removeDialog(1);
        if (permanent) {
            this.mWorkspace.removeItems(apps);
        }
        this.mAllAppsGrid.removeApps(apps);
        if (this.mCustomizePagedView != null) {
            this.mCustomizePagedView.removeApps(apps);
        }
        updateIconsAffectedByPackageManagerChanges();
    }

    public void bindPackagesUpdated() {
        if (this.mCustomizePagedView != null) {
            this.mCustomizePagedView.update();
        }
    }

    private int mapConfigurationOriActivityInfoOri(int configOri) {
        Display d = getWindowManager().getDefaultDisplay();
        int naturalOri = 2;
        switch (d.getRotation()) {
            case 0:
            case 2:
                naturalOri = configOri;
                break;
            case 1:
            case 3:
                if (configOri != 2) {
                    naturalOri = 2;
                    break;
                } else {
                    naturalOri = 1;
                    break;
                }
        }
        int[] oriMap = {1, 0, 9, 8};
        int indexOffset = 0;
        if (naturalOri == 2) {
            indexOffset = 1;
        }
        return oriMap[(d.getRotation() + indexOffset) % 4];
    }

    public void lockScreenOrientation() {
        setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources().getConfiguration().orientation));
    }

    public void unlockScreenOrientation() {
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                Launcher.this.setRequestedOrientation(-1);
            }
        }, 500);
    }

    public void dumpState() {
        Log.d("Launcher", "BEGIN launcher2 dump state for launcher " + this);
        Log.d("Launcher", "mSavedState=" + this.mSavedState);
        Log.d("Launcher", "mWorkspaceLoading=" + this.mWorkspaceLoading);
        Log.d("Launcher", "mRestoring=" + this.mRestoring);
        Log.d("Launcher", "mWaitingForResult=" + this.mWaitingForResult);
        Log.d("Launcher", "mSavedInstanceState=" + this.mSavedInstanceState);
        Log.d("Launcher", "mDesktopItems.size=" + this.mDesktopItems.size());
        Log.d("Launcher", "sFolders.size=" + sFolders.size());
        this.mModel.dumpState();
        this.mAllAppsGrid.dumpState();
        Log.d("Launcher", "END launcher2 dump state");
    }
}
